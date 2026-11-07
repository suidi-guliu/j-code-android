package dev.suicode.packages

import java.io.File

/** Actual JVM validator CLI. --installed ID=VERSION; official trust is supplied by the host, not a pack. */
object PackageTool {
    @JvmStatic fun main(args: Array<String>) {
        try {
            require(args.isNotEmpty()) { "Usage: PackageTool FILE [ID=VERSION ...]" }
            val file=File(args[0])
            if(file.length() !in 20..(32*1024*1024)) fail("E_SIZE","Package file size out of bounds")
            val installed=args.drop(1).associate { it.substringBefore('=') to it.substringAfter('=') }
            val p=PackageVerifier().verify(file.readBytes(), file.extension, installed)
            println("VALID ${p.kind.suffix} ${p.id} ${p.version} sha256=${p.digest} trust=${if(p.official) "official" else "unverified"}")
        } catch(e: Exception) {
            System.err.println(e.message)
            kotlin.system.exitProcess(1)
        }
    }
}
