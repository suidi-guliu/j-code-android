package dev.suicode.packages

import kotlinx.serialization.json.*
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/** One host-owned store per private directory. No scripts, rootfs changes, migration or startup hooks. */
class PackageStore(private val root: Path, private val verifier: PackageVerifier = PackageVerifier()) {
    private val sessions=mutableMapOf<String, CoreSession>()
    init {
        require(!Files.isSymbolicLink(root)) { "E_PATH: store must not be a symlink" }
        Files.createDirectories(root)
    }
    private fun state(): JsonObject {
        val file=root.resolve("state.json")
        return if(Files.exists(file,LinkOption.NOFOLLOW_LINKS)) jsonObject(Files.readAllBytes(file)) else JsonObject(emptyMap())
    }
    private fun publish(path: Path, bytes: ByteArray) {
        if(Files.isSymbolicLink(path)) fail("E_PATH","Store entry is a symlink")
        val temp=Files.createTempFile(root,".transaction-",".tmp")
        try {
            FileOutputStream(temp.toFile()).use { it.write(bytes);it.fd.sync() }
            Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING)
        } finally { Files.deleteIfExists(temp) }
    }
    private fun publishState(s: JsonObject) = publish(root.resolve("state.json"),(canonical(s)+"\n").toByteArray())
    @Synchronized fun installed(): Map<String,String> = state().mapValues { (_,v) -> v.jsonObject.string("version") }
    @Synchronized fun inspect(bytes: ByteArray, suffix: String): VerifiedPack = verifier.verify(bytes,suffix,installed())
    @Synchronized fun install(bytes: ByteArray, suffix: String, consent: Consent): VerifiedPack {
        val p=inspect(bytes,suffix)
        // Authorize without starting any backend or component.
        p.activate(consent,emptyMap()).close()
        val old=state()
        val previous=old[p.id]?.jsonObject
        val oldSigner=previous?.get("signer")?.jsonPrimitive?.contentOrNull
        if(previous!=null && oldSigner!=p.signer) fail("E_SIGNER_CHANGED","Uninstall explicitly before changing publisher identity")
        // An update must not strand existing dependents on an incompatible version.
        val candidateVersions=installed()+(p.id to p.version)
        for((other,value) in old) if(other!=p.id) {
            val record=value.jsonObject
            val otherKind=record.string("kind")
            verifier.verify(Files.readAllBytes(root.resolve("${record.string("digest")}.$otherKind")),otherKind,candidateVersions)
        }
        val target=root.resolve("${p.digest}.${p.kind.suffix}")
        publish(target,bytes)
        val record=buildJsonObject {
            put("digest",p.digest);put("kind",p.kind.suffix);put("version",p.version)
            put("signer",p.signer?.let(::JsonPrimitive) ?: JsonNull)
            put("previous",previous?.get("digest") ?: JsonNull)
            put("enabled",false)
            put("allowUnverified",consent.allowUnverified)
            put("grants",JsonArray(consent.grants.sortedBy { it.wire }.map { JsonPrimitive(it.wire) }))
        }
        // No deletion before commit. Failure leaves previous state usable; orphan archive is inert.
        publishState(JsonObject(old + (p.id to record)))
        sessions.remove(p.id)?.close()
        return p
    }
    @Synchronized fun enable(id: String, backends: Map<Operation,CoreBackend>): CoreSession {
        val s=state();val r=s[id]?.jsonObject ?: fail("E_NOT_INSTALLED",id)
        val kind=r.string("kind");val digest=r.string("digest")
        val p=verifier.verify(Files.readAllBytes(root.resolve("$digest.$kind")),kind,installed())
        if(p.id!=id || p.digest!=digest) fail("E_CHECKSUM","Stored archive differs")
        val grants=r.array("grants").map { v -> Capability.entries.first { it.wire==v.jsonPrimitive.content } }.toSet()
        val session=p.activate(Consent(digest,grants,r.getValue("allowUnverified").jsonPrimitive.boolean),backends)
        publishState(JsonObject(s+(id to JsonObject(r+("enabled" to JsonPrimitive(true))))))
        sessions.remove(id)?.close();sessions[id]=session
        return session
    }
    @Synchronized fun disable(id: String) {
        val s=state();val r=s[id]?.jsonObject ?: fail("E_NOT_INSTALLED",id)
        sessions.remove(id)?.close()
        publishState(JsonObject(s+(id to JsonObject(r+("enabled" to JsonPrimitive(false))))))
    }
    @Synchronized fun uninstall(id: String) {
        val s=state();if(id !in s) fail("E_NOT_INSTALLED",id)
        for((other,v) in s) if(other!=id) {
            val r=v.jsonObject;val kind=r.string("kind")
            val p=verifier.verify(Files.readAllBytes(root.resolve("${r.string("digest")}.$kind")),kind,installed())
            if(p.manifest.array("dependencies").any { it.jsonObject.string("id")==id }) fail("E_DEPENDENCY","Required by $other")
        }
        publishState(JsonObject(s-id));sessions.remove(id)?.close()
        // Archives retained for explicit recovery; never remove tools, projects or user files.
    }
    @Synchronized fun rollback(id: String, consent: Consent): VerifiedPack {
        val r=state()[id]?.jsonObject ?: fail("E_NOT_INSTALLED",id)
        val previous=r["previous"]?.jsonPrimitive?.contentOrNull ?: fail("E_ROLLBACK","No previous package")
        return install(Files.readAllBytes(root.resolve("$previous.${r.string("kind")}")),r.string("kind"),consent)
    }
    @Synchronized fun close() { sessions.values.forEach(CoreSession::close);sessions.clear() }
}
