package dev.suicode.packages

import kotlinx.serialization.json.*
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.zip.ZipInputStream

class PackageFailure(val code: String, detail: String) : IllegalArgumentException("$code: $detail")
internal fun fail(code: String, detail: String): Nothing = throw PackageFailure(code, detail)
internal fun sha(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
internal fun canonical(value: JsonElement): String = when (value) {
    is JsonObject -> value.keys.sorted().joinToString(",", "{", "}") { JsonPrimitive(it).toString() + ":" + canonical(value.getValue(it)) }
    is JsonArray -> value.joinToString(",", "[", "]") { canonical(it) }
    else -> value.toString()
}
internal fun jsonObject(bytes: ByteArray): JsonObject {
    if(bytes.size>1024*1024) fail("E_SIZE","JSON limit is 1 MiB")
    val s = try { Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString() }
        catch(_: Exception) { fail("E_MANIFEST","Invalid UTF-8") }
    var depth=0;var inString=false;var escaped=false
    for(ch in s) {
        if(inString) {
            if(escaped) escaped=false else if(ch=='\\') escaped=true else if(ch=='"') inString=false
        } else when(ch) {
            '"' -> inString=true
            '{','[' -> { depth++;if(depth>64) fail("E_SIZE","JSON nesting exceeds 64") }
            '}',']' -> depth--
        }
    }
    val o = try { Json.parseToJsonElement(s) as JsonObject } catch (_: Exception) { fail("E_MANIFEST", "Invalid JSON object") }
    if (s != canonical(o) + "\n") fail("E_CANONICAL", "JSON must use canonical sorted keys, UTF-8, LF")
    return o
}
internal fun JsonObject.string(key: String): String = (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content ?: fail("E_MANIFEST", "$key must be a string")
internal fun JsonObject.array(key: String): JsonArray = get(key) as? JsonArray ?: fail("E_MANIFEST", "$key must be an array")
internal fun JsonObject.keysExactly(required: Set<String>, optional: Set<String> = emptySet()) {
    if (!keys.containsAll(required) || keys.any { it !in required + optional }) fail("E_MANIFEST", "Missing or unknown fields: $keys")
}
internal fun version(s: String): List<Int> {
    if (!s.matches(Regex("(0|[1-9][0-9]{0,5})\\.(0|[1-9][0-9]{0,5})\\.(0|[1-9][0-9]{0,5})"))) fail("E_VERSION", s)
    return s.split('.').map(String::toInt)
}
internal fun compareVersion(a: String, b: String): Int {
    val av=version(a); val bv=version(b)
    return av.zip(bv).firstOrNull { it.first != it.second }?.let { it.first.compareTo(it.second) } ?: 0
}
class VerifiedPack internal constructor(
    val kind: PackKind, val id: String, val version: String, val digest: String,
    val official: Boolean, val signer: String?,
    permissions: Set<Capability>, components: Map<String, Operation?>,
    manifest: JsonObject, payload: Map<String,ByteArray>
) {
    private val payload = payload.mapValues { it.value.copyOf() }
    fun content(path: String): ByteArray = payload[path]?.copyOf() ?: fail("E_COMPONENT", "Missing package content: $path")
    fun declarations(): List<PackageComponent> = manifest.array("components").map { c ->
        val entry=c.jsonObject
        PackageComponent(entry.string("id"),entry.string("type"),jsonObject(content(entry.string("entry"))))
    }
    val permissions: Set<Capability> = permissions.toSet()
    internal val components: Map<String, Operation?> = components.toMap()
    val manifest: JsonObject = Json.parseToJsonElement(manifest.toString()).jsonObject
    internal fun activate(consent: Consent, backends: Map<Operation, CoreBackend>): CoreSession {
        if (consent.digest != digest || !permissions.containsAll(consent.grants)) fail("E_CONSENT", "Consent does not match this exact package")
        if (!official && !consent.allowUnverified) fail("E_UNVERIFIED", "Explicit unverified-package consent required")
        return CoreSession(id, digest, kind, permissions, consent.grants.toSet(), components, backends.toMap())
    }
}

/** Bounded, in-memory verification. Never extracts, starts a process or loads package code. */
class PackageVerifier(private val officialKeys: Map<String, PublicKey> = emptyMap()) {
    fun verify(bytes: ByteArray, suffix: String, installed: Map<String, String> = emptyMap()): VerifiedPack {
        if (bytes.size !in 20..(32*1024*1024)) fail("E_SIZE", "Package size out of bounds")
        val kind = PackKind.entries.firstOrNull { bytes.copyOfRange(0,8).contentEquals(it.magic.toByteArray()) } ?: fail("E_MAGIC", "Not a sui-code package")
        if (suffix != kind.suffix) fail("E_KIND", "Suffix/header mismatch")
        val header = ByteBuffer.wrap(bytes,8,12)
        if (header.short.toInt()!=1 || header.short.toInt()!=0) fail("E_FORMAT", "Unsupported package version")
        if (header.long != (bytes.size-20).toLong()) fail("E_SIZE", "Payload length mismatch")
        val files = linkedMapOf<String, ByteArray>()
        var total = 0
        try {
            ZipInputStream(ByteArrayInputStream(bytes,20,bytes.size-20)).use { z ->
                while (true) {
                    val e = z.nextEntry ?: break
                    val name=e.name
                    if (e.isDirectory || !name.matches(Regex("[A-Za-z0-9_.-]+(/[A-Za-z0-9_.-]+)*")) || name.split('/').any { it == "." || it == ".." }) fail("E_PATH", name)
                    if (name in files || files.size >= 256) fail("E_STRUCTURE", "Duplicate name or too many entries")
                    val data=z.readNBytes(8*1024*1024+1)
                    total+=data.size
                    if (data.size>8*1024*1024 || total>32*1024*1024) fail("E_SIZE", "Expanded archive limit")
                    files[name]=data
                    z.closeEntry()
                }
            }
        } catch (e: PackageFailure) { throw e } catch (_: Exception) { fail("E_ARCHIVE", "Invalid ZIP payload") }
        val checks = files["checksums.sha256"] ?: fail("E_CHECKSUM", "Missing checksums")
        val expected = files.filterKeys { it !in setOf("checksums.sha256", "signature.sig") }.toSortedMap().entries.joinToString("") { "${sha(it.value)}  ${it.key}\n" }
        if (!checks.contentEquals(expected.toByteArray())) fail("E_CHECKSUM", "Checksum table or payload does not match")
        val m=jsonObject(files["manifest.json"] ?: fail("E_MANIFEST", "Missing manifest"))
        m.keysExactly(setOf("id","name","author","version","kind","api","permissions","dependencies","components","description"))
        val id=m.string("id")
        if (!id.matches(Regex("[a-z][a-z0-9]*(\\.[a-z][a-z0-9-]*)+"))) fail("E_MANIFEST", "Invalid package ID")
        for (field in listOf("name","author","description")) if (m.string(field).length !in 1..1024) fail("E_MANIFEST", field)
        version(m.string("version"))
        if (m.string("kind") != kind.suffix) fail("E_KIND", "Manifest/header mismatch")
        val api=m["api"] as? JsonObject ?: fail("E_API", "api missing")
        api.keysExactly(setOf("min","max"))
        val min=(api["min"] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull ?: fail("E_API","min")
        val max=(api["max"] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull ?: fail("E_API","max")
        if (!CoreApi.compatible(min,max)) fail("E_API", "Incompatible Core API")
        val permissions=m.array("permissions").map { p -> Capability.entries.firstOrNull { p is JsonPrimitive && p.isString && it.wire==p.content } ?: fail("E_PERMISSION","Unknown permission") }
        if (permissions.distinct().size!=permissions.size) fail("E_PERMISSION","Duplicate permission")
        if (kind==PackKind.PLUGIN && permissions.any { it !in setOf(Capability.FILE_READ,Capability.RUN) }) fail("E_BOUNDARY","Plugin permission outside SimpleRun scope")
        m.array("dependencies").forEach { d ->
            val dep=d as? JsonObject ?: fail("E_DEPENDENCY","Invalid dependency")
            dep.keysExactly(setOf("id","min","max"))
            val depId=dep.string("id")
            if (depId==id || !depId.matches(Regex("[a-z][a-z0-9]*(\\.[a-z][a-z0-9-]*)+"))) fail("E_DEPENDENCY","Invalid dependency ID")
            val low=dep.string("min"); val high=dep.string("max")
            if (compareVersion(low, high)>=0) fail("E_DEPENDENCY","Empty version interval")
            val v=installed[depId] ?: fail("E_DEPENDENCY","Missing $depId")
            if (compareVersion(v,low)<0 || compareVersion(v,high)>=0) fail("E_DEPENDENCY","Incompatible $depId")
        }
        val allowed=if(kind==PackKind.PLUGIN) setOf("language","simpleRun","environment","basicBuild") else setOf("language","simpleRun","environment","lsp","dap","build","test","format","lint","project","workflow","command","panel")
        val componentIds=mutableMapOf<String, Operation?>()
        m.array("components").forEach { c ->
            val comp=c as? JsonObject ?: fail("E_COMPONENT","Expected object")
            comp.keysExactly(setOf("id","type","entry"))
            if (!comp.string("id").matches(Regex("[a-z][a-z0-9.-]{0,63}")) || componentIds.containsKey(comp.string("id"))) fail("E_COMPONENT","Duplicate/invalid component ID")
            if (comp.string("type") !in allowed) fail("E_BOUNDARY","Component outside ${kind.suffix} scope")
            val entry=comp.string("entry")
            val prefix=if(kind==PackKind.PLUGIN) "plugin/" else "ide/"
            if (!entry.startsWith(prefix) || entry !in files || !entry.endsWith(".json")) fail("E_COMPONENT","Missing declarative component $entry")
            componentIds[comp.string("id")]=validateDescriptor(comp.string("type"), jsonObject(files.getValue(entry)))
        }
        m.array("components").forEach { c ->
            if(c.jsonObject.string("type")=="panel") {
                val d=jsonObject(files.getValue(c.jsonObject.string("entry")))
                if("nodes" in d) {
                    if(min<2) fail("E_API","Native UI nodes require minimum Core API 2")
                    UiNodes.validate(d.array("nodes"),componentIds.filterValues { it!=null && it!=Operation.PANEL_OPEN }.keys)
                }
            }
        }
        val prefixes=if(kind==PackKind.PLUGIN) setOf("plugin","resources","assets") else setOf("ide","components","adapters","workflows","resources","assets")
        if (files.keys.any { it !in setOf("manifest.json","checksums.sha256","signature.sig") && it.substringBefore('/') !in prefixes }) fail("E_STRUCTURE","Unknown root directory")
        val sig=files["signature.sig"]?.let(::jsonObject)
        var fingerprint: String?=null
        var official=false
        if(sig!=null) {
            sig.keysExactly(setOf("algorithm","publicKey","signature"))
            if(sig.string("algorithm")!="ECDSA-P256-SHA256") fail("E_SIGNATURE","Unsupported signature")
            try {
                val der=Base64.getDecoder().decode(sig.string("publicKey"))
                val key=KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(der)) as ECPublicKey
                val target=java.security.AlgorithmParameters.getInstance("EC").apply { init(java.security.spec.ECGenParameterSpec("secp256r1")) }.getParameterSpec(java.security.spec.ECParameterSpec::class.java)
                if(key.params.curve!=target.curve || key.params.generator!=target.generator || key.params.order!=target.order || key.params.cofactor!=target.cofactor) fail("E_SIGNATURE","P-256 key required")
                val verifier=Signature.getInstance("SHA256withECDSA")
                verifier.initVerify(key)
                verifier.update("SUI-PACK-SIGNATURE-V1\n".toByteArray()+kind.magic.toByteArray()+checks)
                if(!verifier.verify(Base64.getDecoder().decode(sig.string("signature")))) fail("E_SIGNATURE","Invalid signature")
                fingerprint=sha(der)
                official=officialKeys[fingerprint]?.encoded?.contentEquals(der)==true
            } catch(e: PackageFailure) { throw e } catch(_: Exception) { fail("E_SIGNATURE","Malformed signature") }
        }
        return VerifiedPack(kind,id,m.string("version"),sha(bytes),official,fingerprint,permissions.toSet(),componentIds,m,files)
    }
}

class PluginParser(private val verifier: PackageVerifier = PackageVerifier()) {
    fun parse(bytes: ByteArray, installed: Map<String,String> = emptyMap()): VerifiedPack = verifier.verify(bytes,"suic",installed)
}
class IdeKitParser(private val verifier: PackageVerifier = PackageVerifier()) {
    fun parse(bytes: ByteArray, installed: Map<String,String> = emptyMap()): VerifiedPack = verifier.verify(bytes,"suik",installed)
}

data class PackageComponent(val id: String, val type: String, val descriptor: JsonObject)
