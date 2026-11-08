package dev.suicode.packages

import kotlinx.serialization.json.*

/** Language-neutral host registry. Registration never starts a process or installs an environment. */
class NativeProviders {
    private val providers=linkedMapOf<String,List<RunProvider>>()
    @Synchronized fun register(pack: VerifiedPack, session: CoreSession) {
        if(pack.id!=session.packageId || pack.digest!=session.digest) fail("E_CONSENT","Session/package mismatch")
        val declarations=pack.declarations()
        val languages=declarations.filter { it.type=="language" }
        val simple=declarations.filter { it.type=="simpleRun" }
        val primaryLanguages=simple.map { it.descriptor.string("languageId") }.toSet()
        val runs=simple+declarations.filter { it.type=="basicBuild" && it.descriptor.string("languageId") !in primaryLanguages }
        providers[pack.id]=runs.map { run ->
            val language=languages.singleOrNull { it.descriptor.string("languageId")==run.descriptor.string("languageId") }
                ?: fail("E_COMPONENT","SimpleRun needs exactly one matching language declaration")
            RunProvider(pack,session,run,language.descriptor.array("extensions").map { it.jsonPrimitive.content }.toSet())
        }
    }
    @Synchronized fun unregister(packageId: String) { providers.remove(packageId) }
    @Synchronized fun resolve(fileName: String): RunProvider? {
        val extension=fileName.substringAfterLast('.',"")
        val candidates=providers.values.flatten().filter { extension in it.extensions }
        if(candidates.size>1) {
            val kit=candidates.singleOrNull { it.pack.kind==PackKind.KIT }
            if(kit!=null) {
                val dependencies=kit.pack.manifest.array("dependencies").map { it.jsonObject.string("id") }
                if(candidates.filter { it!==kit }.all { it.pack.kind==PackKind.PLUGIN && it.pack.id in dependencies }) return kit
            }
            fail("E_CONFLICT","Multiple enabled providers claim .$extension; disable one in Plugin Center")
        }
        return candidates.singleOrNull()
    }
}
class RunProvider internal constructor(pack: VerifiedPack,session: CoreSession,private val component: PackageComponent,val extensions: Set<String>) :
    NativeRecipe(pack,session,component.id,Operation.SIMPLE_RUN,component.descriptor.array("prepare"),component.descriptor.array("run")) {
    val languageId get()=component.descriptor.string("languageId")
}
