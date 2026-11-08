package dev.blamspot.jcode.fs

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex

/** Serialized app-owned mutations plus an editor acknowledgement barrier before a save. */
object FileMutations {
    data class Change(val from: FsPath?,val to: FsPath?,val restored: Boolean=false,val barrier: CompletableDeferred<Unit>?=null)
    val mutex=Mutex()
    private val channel=Channel<Change>(Channel.UNLIMITED)
    val changes=channel.receiveAsFlow()
    suspend fun awaitEditor() { val ack=CompletableDeferred<Unit>();channel.send(Change(null,null,barrier=ack));kotlinx.coroutines.withTimeout(5000) { ack.await() } }
    fun restored(from: FsPath,to: FsPath) { channel.trySend(Change(from,to,true)) }
    fun moved(from: FsPath,to: FsPath) { channel.trySend(Change(from,to)) }
    fun removed(path: FsPath) { channel.trySend(Change(path,null)) }
}
