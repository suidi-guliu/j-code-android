package dev.blamspot.jcode.fs
import java.nio.file.Files
import java.io.File
import java.io.IOException
import kotlinx.coroutines.runBlocking
import kotlin.test.*
class TrashJournalTest {
    @Test fun diskFullBeforeJournalLeavesOriginalUnmoved()=runBlocking {
        val root=Files.createTempDirectory("sui-trash").toFile()
        try {
            val file=File(root,"source.txt").apply { writeText("unsaved data") }
            val trash=Trash(File(root,"bin")) { throw IOException("disk full") }
            assertFailsWith<IOException> { trash.put(null,FsPath.Local(file),"test",root.path,TrashSource.Explorer) }
            assertEquals("unsaved data",file.readText())
        } finally { SafeLocalFiles.remove(root) }
    }
    @Test fun diskFullAfterMoveKeepsDurableRecoveryJournal()=runBlocking {
        val root=Files.createTempDirectory("sui-trash").toFile()
        try {
            val file=File(root,"中文 + source.txt").apply { writeText("recover me") }
            val trash=Trash(File(root,"bin")) { if(it.sizeBytes>0) throw IOException("disk full after move") }
            trash.put(null,FsPath.Local(file),"test",root.path,TrashSource.Explorer)
            assertFalse(file.exists())
            val entry=trash.list().single()
            assertTrue(File(File(root,"bin"),"${entry.id}/data/${file.name}").isFile)
            assertEquals(file.path,entry.originalPath)
            assertEquals("recover me",File(File(root,"bin"),"${entry.id}/data/${file.name}").readText())
        } finally { SafeLocalFiles.remove(root) }
    }
    @Test fun damagedJournalPayloadIsNeverImplicitlyPurged()=runBlocking {
        val root=Files.createTempDirectory("sui-trash").toFile()
        try {
            val entry=File(root,"broken/data").apply { mkdirs() }
            val payload=File(entry,"precious.txt").apply { writeText("preserve") }
            val trash=Trash(root)
            assertEquals(0,trash.sweep(1));assertEquals("preserve",payload.readText())
        } finally { SafeLocalFiles.remove(root) }
    }
}
