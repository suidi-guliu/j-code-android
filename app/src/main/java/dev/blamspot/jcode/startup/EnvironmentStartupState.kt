package dev.blamspot.jcode.startup

/** A missing distro deliberately has no user probe result. Null there is not ongoing work. */
internal fun environmentStartupResolving(distroInstalled: Boolean?, userReady: Boolean?, runningStep: Boolean): Boolean =
    distroInstalled == null || (distroInstalled && userReady == null) || runningStep
