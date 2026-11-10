package dev.suicode.nativehost

import dev.suicode.packages.InstalledPackage

/** Invalidate only changed service owners and their dependents, never unrelated debug hosts. */
internal fun affectedServicePackages(previous: Map<String,InstalledPackage>,current: Map<String,InstalledPackage>,dependencies: Map<String,List<String>>): Set<String> {
    val affected=(previous.keys+current.keys).filterTo(mutableSetOf()) { previous[it]!=current[it] }
    var changed: Boolean
    do { changed=false;dependencies.forEach { (id,required) -> if(id !in affected && required.any { it in affected }) { affected+=id;changed=true } } } while(changed)
    return affected
}

/** A component repair can change installed tools without changing the package digest or grants. */
internal fun requiresServiceReload(previous: InstalledPackage?,current: InstalledPackage?,hasLsp: Boolean): Boolean =
    hasLsp && previous!=null && previous.enabled && previous==current
