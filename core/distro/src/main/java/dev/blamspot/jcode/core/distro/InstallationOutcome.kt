package dev.blamspot.jcode.core.distro

/** Command exit status and observed postconditions are distinct facts. */
data class InstallationOutcome(val ready: Boolean,val warning: String?,val message: String) {
    companion object {
        fun assess(exit: Int?,verified: Boolean,cancelled: Boolean=false): InstallationOutcome = when {
            verified -> InstallationOutcome(true,if(exit==0) null else "Installer exit $exit; required tools passed independent verification",if(exit==0) "Ready · all required tools verified" else "Ready with installer warnings · inspect the log; no reinstall needed")
            cancelled || exit==130 -> InstallationOutcome(false,null,"Cancelled at a safe boundary · completed package operations retained; retry missing components")
            else -> InstallationOutcome(false,null,"Partially Ready · verification failed (installer exit $exit); inspect logs and retry missing components")
        }
    }
}
