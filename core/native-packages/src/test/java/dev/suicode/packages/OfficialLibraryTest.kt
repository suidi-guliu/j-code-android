package dev.suicode.packages

import java.io.File
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import kotlin.test.*
import org.junit.Test

class OfficialLibraryTest {
    private val library=File("../../native-library")
    private val key=KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(File(library,"official-p256.der").readBytes()))
    @Test fun realCatalogAndEveryAdvertisedDownloadAreConsistent() {
        val entries=OfficialLibrary.verify(File(library,"v1/index.json").readBytes(),File(library,"v1/index.sig").readBytes(),key)
        assertEquals(7,entries.size)
        val installed=entries.filter { it.kind=="suic" }.associate { it.id to it.version }
        entries.forEach { item ->
            val file=if(item.signer.isEmpty()) File("../../localization/releases/"+item.url.substringAfterLast('/')) else File(library,"v1/"+item.url.substringAfterLast('/'));val bytes=file.readBytes()
            assertEquals(item.bytes,bytes.size);assertEquals(item.sha256,sha(bytes))
            val verified=PackageVerifier(mapOf(sha(key.encoded) to key)).verify(bytes,item.kind,installed)
            assertEquals(item.signer.isNotEmpty(),verified.official);assertEquals(item.id,verified.id)
            assertEquals(item.category,PackageCards.forPack(verified).category.wire)
        }
    }
    @Test fun changedCatalogIsRejectedBeforeInterpretingUrls() {
        val data=File(library,"v1/index.json").readBytes();data[data.lastIndex]=0
        assertEquals("E_SIGNATURE",assertFailsWith<PackageFailure> { OfficialLibrary.verify(data,File(library,"v1/index.sig").readBytes(),key) }.code)
    }
}
