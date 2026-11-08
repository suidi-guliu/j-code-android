package dev.suicode.packages

import java.security.PublicKey
import java.security.Signature
import kotlinx.serialization.json.*

data class LibraryItem(val id: String,val name: String,val version: String,val kind: String,val url: String,val bytes: Int,val sha256: String,
    val author: String,val description: String,val dependencies: String,val changeLog: String,val signer: String)

/** Verifies the catalog before interpreting any URL or offering an update. */
object OfficialLibrary {
    fun verify(bytes: ByteArray,signature: ByteArray,key: PublicKey): List<LibraryItem> {
        if(bytes.size>1024*1024 || signature.size>1024) fail("E_SIZE","Library metadata too large")
        val verified=try { Signature.getInstance("SHA256withECDSA").run { initVerify(key);update("SUI-LIBRARY-V1\n".toByteArray()+bytes);verify(signature) } } catch(_: Exception) { false }
        if(!verified) fail("E_SIGNATURE","Official library signature is invalid")
        val root=jsonObject(bytes)
        root.keysExactly(setOf("format","packages"))
        if(root["format"]?.jsonPrimitive?.intOrNull!=1) fail("E_FORMAT","Unknown library format")
        val entries=root.array("packages")
        if(entries.size>256) fail("E_SIZE","Too many library entries")
        val ids=mutableSetOf<String>()
        return entries.map { value ->
            val o=value.jsonObject
            o.keysExactly(setOf("id","name","version","kind","url","bytes","sha256","author","description","dependencies","changeLog","signer","signature","api"))
            val id=o.string("id");if(!ids.add(id)) fail("E_MANIFEST","Duplicate library ID")
            version(o.string("version"))
            val size=o.getValue("bytes").jsonPrimitive.int
            if(size !in 20..32*1024*1024 || !o.string("sha256").matches(Regex("[a-f0-9]{64}"))) fail("E_CHECKSUM","Invalid library size or digest")
            val uri=java.net.URI(o.string("url"))
            if(uri.scheme!="https" || uri.host!="raw.githubusercontent.com" || !uri.path.startsWith("/suidi-guliu/j-code-android/") || uri.rawQuery!=null || uri.userInfo!=null) fail("E_PERMISSION","Unapproved library origin")
            if(o.string("kind") !in setOf("suic","suik") || o.string("signer")!=sha(key.encoded)) fail("E_SIGNATURE","Library publisher identity differs")
            val api=o.getValue("api").jsonObject
            if(!CoreApi.compatible(api.getValue("min").jsonPrimitive.int,api.getValue("max").jsonPrimitive.int)) fail("E_API","Catalog requires incompatible API")
            LibraryItem(id,o.string("name"),o.string("version"),o.string("kind"),o.string("url"),size,o.string("sha256"),o.string("author"),o.string("description"),o.array("dependencies").toString(),o.string("changeLog"),o.string("signer"))
        }
    }
}
