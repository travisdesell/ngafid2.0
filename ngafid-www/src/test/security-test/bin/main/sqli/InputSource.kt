package sqli

/**
 *represents a discovered input source on a web page
 */
data class InputSource(
    val inputType: InputType,
    val name: String,
    val formAction: String? = null,
    val formMethod: String? = null,
    val fieldType: String? = null,
    val location: String,
)

enum class InputType {
    FORM_FIELD,
    URL_PARAM,
    COOKIE,
    STANDALONE_INPUT,
}
