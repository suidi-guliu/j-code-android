package dev.blamspot.jcode.startup

/** Pure non-critical UI preference decoder; wrong values never become a startup ClassCastException. */
object UiPreferenceValues {
    fun language(value: Any?,invalid: (Exception)->Unit): String? = when {
        value==null -> null
        value is String && value.length<=512 -> value
        else -> { invalid(IllegalArgumentException("Expected a UI language String, found ${value.javaClass.simpleName}"));null }
    }
    fun paneWidth(value: Any?,invalid: (Exception)->Unit): Float = when {
        value==null -> Float.NaN
        value is Float && value.isFinite() && value>0f -> value
        else -> { invalid(IllegalArgumentException("Invalid optional panel-width value (${value.javaClass.simpleName})"));Float.NaN }
    }
}
