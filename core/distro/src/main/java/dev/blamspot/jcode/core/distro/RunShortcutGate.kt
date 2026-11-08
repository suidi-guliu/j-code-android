package dev.blamspot.jcode.core.distro

/** Deduplicates Android's normal and shortcut dispatch of the same physical key-down. */
class RunShortcutGate {
    private var last: Pair<Long,Int>?=null
    fun accept(downTime: Long,keyCode: Int,repeat: Int): Boolean {
        val identity=downTime to keyCode
        if(repeat!=0 || last==identity) return false
        last=identity;return true
    }
}
