package dev.suicode.packages

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.*
import org.junit.Test

class PackageVerifierTest {
    private val examples=File("../../docs/specifications/examples/packages")
    private fun load(name: String) = File(examples,name).readBytes()
    private fun manifest(kind: String="suic", components: String="[]", permissions: String="[]") = """{"api":{"max":1,"min":1},"author":"Tests","components":$components,"dependencies":[],"description":"Fixture","id":"dev.test.fixture","kind":"$kind","name":"Test","permissions":$permissions,"version":"0.1.0"}
""".toByteArray()
    private fun pack(kind: String="suic", payload: Map<String,ByteArray> = mapOf("manifest.json" to manifest()), signed: Boolean=false, corruptSignature: Boolean=false): Pair<ByteArray, java.security.PublicKey?> {
        val magic=PackKind.entries.first { it.suffix==kind }.magic.toByteArray()
        val files=payload.toMutableMap()
        val checks=files.toSortedMap().entries.joinToString("") { "${sha(it.value)}  ${it.key}\n" }.toByteArray()
        files["checksums.sha256"]=checks
        val key=if(signed) KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair() else null
        if(key!=null) {
            val sig=Signature.getInstance("SHA256withECDSA").apply { initSign(key.private); update("SUI-PACK-SIGNATURE-V1\n".toByteArray()+magic+checks) }.sign()
            if(corruptSignature) sig[sig.lastIndex]=(sig.last().toInt() xor 1).toByte()
            files["signature.sig"]="""{"algorithm":"ECDSA-P256-SHA256","publicKey":"${Base64.getEncoder().encodeToString(key.public.encoded)}","signature":"${Base64.getEncoder().encodeToString(sig)}"}
""".toByteArray()
        }
        val out=ByteArrayOutputStream()
        ZipOutputStream(out).use { z -> files.forEach { (name,data) -> z.putNextEntry(ZipEntry(name));z.write(data);z.closeEntry() } }
        val body=out.toByteArray()
        return (magic+ByteBuffer.allocate(12).putShort(1).putShort(0).putLong(body.size.toLong()).array()+body) to key?.public
    }
    private fun rejected(code: String, action: ()->Unit) { assertEquals(code,assertFailsWith<PackageFailure>(block=action).code) }
    @Test fun allValidExamplesVerify() {
        examples.listFiles()!!.filter { !it.name.startsWith("illegal") }.forEach { f ->
            PackageVerifier().verify(f.readBytes(),f.extension,mapOf("dev.suicode.example.java" to "0.1.0","dev.suicode.example.c-cpp" to "0.1.0","dev.suicode.example.python" to "0.1.0"))
        }
    }
    @Test fun illegalBoundaryIsRejected() { rejected("E_BOUNDARY") { PackageVerifier().verify(load("illegal-plugin.suic"),"suic") } }
    @Test fun renamedZipIsRejected() { rejected("E_MAGIC") { PackageVerifier().verify(ByteArray(100),"suic") } }
    @Test fun kindCannotBeRenamed() { rejected("E_KIND") { PackageVerifier().verify(load("minimal.suic"),"suik") } }
    @Test fun versionRejected() { val b=pack().first; b[9]=2; rejected("E_FORMAT") { PackageVerifier().verify(b,"suic") } }
    @Test fun lengthRejected() { rejected("E_SIZE") { PackageVerifier().verify(pack().first+byteArrayOf(0),"suic") } }
    @Test fun traversalRejected() { rejected("E_PATH") { PackageVerifier().verify(pack(payload=mapOf("manifest.json" to manifest(),"../escape" to byteArrayOf(1))).first,"suic") } }
    @Test fun missingDependencyRejected() { rejected("E_DEPENDENCY") { PackageVerifier().verify(load("java-kit.suik"),"suik") } }
    @Test fun schemaRejectsUnknownFields() { val m=manifest().toString(Charsets.UTF_8).replace("\"version\"","\"unknown\":true,\"version\""); rejected("E_MANIFEST") { PackageVerifier().verify(pack(payload=mapOf("manifest.json" to m.toByteArray())).first,"suic") } }
    @Test fun duplicateJsonFieldsRejected() { val m=manifest().toString(Charsets.UTF_8).replace("\"author\":\"Tests\"","\"author\":\"First\",\"author\":\"Tests\"");rejected("E_CANONICAL") { PackageVerifier().verify(pack(payload=mapOf("manifest.json" to m.toByteArray())).first,"suic") } }
    @Test fun incompatibleApiRejected() { val m=manifest().toString(Charsets.UTF_8).replace("\"min\":1","\"min\":2"); rejected("E_API") { PackageVerifier().verify(pack(payload=mapOf("manifest.json" to m.toByteArray())).first,"suic") } }
    @Test fun signatureVerifiedAndTrustPinned() {
        val (b,key)=pack(signed=true)
        assertFalse(PackageVerifier().verify(b,"suic").official)
        val verified=PackageVerifier(mapOf(sha(key!!.encoded) to key)).verify(b,"suic")
        assertTrue(verified.official)
        assertEquals(sha(key.encoded),verified.signer)
    }
    @Test fun unverifiedRequiresExplicitConsent() {
        val p=PackageVerifier().verify(load("minimal.suic"),"suic")
        rejected("E_UNVERIFIED") { PluginLoader().activate(p,Consent(p.digest,emptySet(),false),emptyMap()) }
    }
    @Test fun changedPackageCannotReuseConsent() {
        val p=PackageVerifier().verify(load("minimal.suic"),"suic")
        rejected("E_CONSENT") { PluginLoader().activate(p,Consent("wrong",emptySet(),true),emptyMap()) }
    }
    @Test fun lifecyclePermissionAndMissingBackend() {
        val p=PackageVerifier().verify(load("minimal-kit.suik"),"suik")
        val denied=IdeKitLoader().activate(p,Consent(p.digest,emptySet(),true),emptyMap())
        assertEquals("E_PERMISSION",denied.request(Request(Operation.PANEL_OPEN,"overview",emptyMap())).code)
        val s=IdeKitLoader().activate(p,Consent(p.digest,setOf(Capability.UI),true),emptyMap())
        assertEquals("E_UNAVAILABLE",s.request(Request(Operation.PANEL_OPEN,"overview",emptyMap())).code)
        s.close();assertEquals("E_DISABLED",s.request(Request(Operation.PANEL_OPEN,"overview",emptyMap())).code)
    }
    @Test fun panelUpdatesAreScopedAndReleased() {
        val p=PackageVerifier().verify(load("minimal-kit.suik"),"suik")
        val s=IdeKitLoader().activate(p,Consent(p.digest,setOf(Capability.UI),true),mapOf(Operation.PANEL_OPEN to CoreBackend { Reply(true,"OK") }))
        val store=PanelStore(); assertTrue(store.put(s,PanelState("overview","Title","Text")).accepted)
        assertEquals(1,store.snapshot().size);store.release(p.id);assertTrue(store.snapshot().isEmpty())
        assertEquals("E_BOUNDARY",s.request(Request(Operation.SIMPLE_RUN,"overview",emptyMap())).code)
    }
    @Test fun checksumTableMismatchRejected() {
        rejected("E_CHECKSUM") { PackageVerifier().verify(pack(payload=mapOf("manifest.json" to manifest(),"checksums.sha256" to byteArrayOf(1))).first,"suic") }
    }
    @Test fun invalidSignatureNeverDowngradesToUnverified() {
        rejected("E_SIGNATURE") { PackageVerifier().verify(pack(signed=true,corruptSignature=true).first,"suic") }
    }
    @Test fun differentLoaderRejectsKind() {
        val p=PackageVerifier().verify(load("minimal.suic"),"suic")
        assertFailsWith<IllegalArgumentException> { IdeKitLoader().activate(p,Consent(p.digest,emptySet(),true),emptyMap()) }
    }
    @Test fun unknownPermissionRejected() {
        val m=manifest(permissions="[\"root.access\"]")
        rejected("E_PERMISSION") { PackageVerifier().verify(pack(payload=mapOf("manifest.json" to m)).first,"suic") }
    }
    @Test fun extraGrantRejected() {
        val p=PackageVerifier().verify(load("minimal.suic"),"suic")
        rejected("E_CONSENT") { PluginLoader().activate(p,Consent(p.digest,setOf(Capability.RUN),true),emptyMap()) }
    }
    @Test fun backendExceptionFailsClosed() {
        val p=PackageVerifier().verify(load("minimal-kit.suik"),"suik")
        val s=IdeKitLoader().activate(p,Consent(p.digest,setOf(Capability.UI),true),mapOf(Operation.PANEL_OPEN to CoreBackend { error("secret detail") }))
        val result=s.request(Request(Operation.PANEL_OPEN,"overview",emptyMap()))
        assertEquals("E_BACKEND",result.code);assertFalse(result.accepted)
    }

    @Test fun distinctVersionsRollbackAndRevokeOldSession() {
        val directory=java.nio.file.Files.createTempDirectory("sui-rollback-")
        try {
            val store=PackageStore(directory)
            val b1=pack().first
            val b2=pack(payload=mapOf("manifest.json" to manifest().toString(Charsets.UTF_8).replace("0.1.0","0.2.0").toByteArray())).first
            val p1=store.inspect(b1,"suic");val p2=store.inspect(b2,"suic")
            val c1=Consent(p1.digest,emptySet(),true);val c2=Consent(p2.digest,emptySet(),true)
            store.install(b1,"suic",c1)
            val old=store.enable(p1.id,emptyMap())
            store.install(b2,"suic",c2)
            assertEquals("0.2.0",store.installed()[p1.id])
            assertEquals("E_DISABLED",old.request(Request(Operation.SIMPLE_RUN,"none",emptyMap())).code)
            assertEquals("0.1.0",store.rollback(p1.id,c1).version)
            store.close()
        } finally { directory.toFile().deleteRecursively() }
    }

    @Test fun excessiveJsonNestingRejectedBeforeParsing() {
        val nested=("[".repeat(65)+"0"+"]".repeat(65)).toByteArray()
        rejected("E_SIZE") { PackageVerifier().verify(pack(payload=mapOf("manifest.json" to nested)).first,"suic") }
    }
    @Test fun invalidUtf8Rejected() {
        rejected("E_MANIFEST") { PackageVerifier().verify(pack(payload=mapOf("manifest.json" to byteArrayOf(0xff.toByte()))).first,"suic") }
    }

}
