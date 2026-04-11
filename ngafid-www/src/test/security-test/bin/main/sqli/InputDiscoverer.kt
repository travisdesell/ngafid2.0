package sqli
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import java.net.URI
/**
 *discovers all input sources on a web page using Selenium WebDriver
 *finds form fields, textareas, selects, standalone inputs, URL params, and links with params
 */
class InputDiscoverer(private val driver: WebDriver) {
    fun discoverAll(url: String): List<InputSource> {
        println("Navigating to: $url")
        driver.get(url)
        Thread.sleep(2000) 
        val inputs = mutableListOf<InputSource>()
        inputs.addAll(discoverFormInputs())
        inputs.addAll(discoverStandaloneInputs())
        inputs.addAll(discoverUrlParams(url))
        inputs.addAll(discoverLinkParams(url))
        inputs.addAll(discoverContentEditables())
        return inputs.distinctBy { "${it.inputType}:${it.name}:${it.formAction}:${it.formMethod}" }
    }
    private fun discoverFormInputs(): List<InputSource> {
        val inputs = mutableListOf<InputSource>()
        val forms = driver.findElements(By.tagName("form"))
        for (form in forms) {
            val action = form.getAttribute("action") ?: ""
            val method = (form.getAttribute("method") ?: "GET").uppercase()
            val resolvedAction = resolveUrl(driver.currentUrl, action)
            //input fields
            for (input in form.findElements(By.tagName("input"))) {
                val name = input.getAttribute("name")?.takeIf { it.isNotBlank() }
                    ?: input.getAttribute("id")?.takeIf { it.isNotBlank() }
                    ?: continue
                val type = (input.getAttribute("type") ?: "text").lowercase()
                if (type in listOf("submit", "button", "image", "reset", "file", "hidden")) continue
                inputs.add(
                    InputSource(
                        inputType = InputType.FORM_FIELD,
                        name = name,
                        formAction = resolvedAction,
                        formMethod = method,
                        fieldType = type,
                        location = "Form [$method $resolvedAction] <input type=\"$type\" name=\"$name\">",
                    )
                )
            }
            //test hidden
            for (input in form.findElements(By.tagName("input"))) {
                val name = input.getAttribute("name")?.takeIf { it.isNotBlank() }
                    ?: input.getAttribute("id")?.takeIf { it.isNotBlank() }
                    ?: continue
                val type = (input.getAttribute("type") ?: "text").lowercase()
                if (type != "hidden") continue
                inputs.add(
                    InputSource(
                        inputType = InputType.FORM_FIELD,
                        name = name,
                        formAction = resolvedAction,
                        formMethod = method,
                        fieldType = "hidden",
                        location = "Form [$method $resolvedAction] <input type=\"hidden\" name=\"$name\">",
                    )
                )
            }
            //text
            for (textarea in form.findElements(By.tagName("textarea"))) {
                val name = textarea.getAttribute("name")?.takeIf { it.isNotBlank() }
                    ?: textarea.getAttribute("id")?.takeIf { it.isNotBlank() }
                    ?: continue
                inputs.add(
                    InputSource(
                        inputType = InputType.FORM_FIELD,
                        name = name,
                        formAction = resolvedAction,
                        formMethod = method,
                        fieldType = "textarea",
                        location = "Form [$method $resolvedAction] <textarea name=\"$name\">",
                    )
                )
            }
            //select
            for (select in form.findElements(By.tagName("select"))) {
                val name = select.getAttribute("name")?.takeIf { it.isNotBlank() }
                    ?: select.getAttribute("id")?.takeIf { it.isNotBlank() }
                    ?: continue
                inputs.add(
                    InputSource(
                        inputType = InputType.FORM_FIELD,
                        name = name,
                        formAction = resolvedAction,
                        formMethod = method,
                        fieldType = "select",
                        location = "Form [$method $resolvedAction] <select name=\"$name\">",
                    )
                )
            }
        }
        return inputs
    }
    private fun discoverStandaloneInputs(): List<InputSource> {
        val inputs = mutableListOf<InputSource>()
        val js = driver as org.openqa.selenium.JavascriptExecutor
        @Suppress("UNCHECKED_CAST")
        val standaloneInputs = js.executeScript(
            "return Array.from(document.querySelectorAll('input')).filter(el => !el.closest('form'));"
        ) as? List<WebElement> ?: emptyList()
        for (input in standaloneInputs) {
            val name = input.getAttribute("name")?.takeIf { it.isNotBlank() }
                ?: input.getAttribute("id")?.takeIf { it.isNotBlank() }
                ?: continue
            val type = (input.getAttribute("type") ?: "text").lowercase()
            if (type in listOf("submit", "button", "image", "reset", "file")) continue
            inputs.add(
                InputSource(
                    inputType = InputType.STANDALONE_INPUT,
                    name = name,
                    formAction = driver.currentUrl,
                    formMethod = "GET",
                    fieldType = type,
                    location = "Standalone <input type=\"$type\" name=\"$name\">",
                )
            )
        }
        @Suppress("UNCHECKED_CAST")
        val standaloneTextareas = js.executeScript(
            "return Array.from(document.querySelectorAll('textarea')).filter(el => !el.closest('form'));"
        ) as? List<WebElement> ?: emptyList()
        for (textarea in standaloneTextareas) {
            val name = textarea.getAttribute("name")?.takeIf { it.isNotBlank() }
                ?: textarea.getAttribute("id")?.takeIf { it.isNotBlank() }
                ?: continue
            inputs.add(
                InputSource(
                    inputType = InputType.STANDALONE_INPUT,
                    name = name,
                    formAction = driver.currentUrl,
                    formMethod = "GET",
                    fieldType = "textarea",
                    location = "Standalone <textarea name=\"$name\">",
                )
            )
        }
        return inputs
    }
    //test url queries
    private fun discoverUrlParams(url: String): List<InputSource> {
        val inputs = mutableListOf<InputSource>()
        try {
            val uri = URI(url)
            val query = uri.query ?: return inputs
            val baseUrl = "${uri.scheme}://${uri.authority}${uri.path}"
            query.split("&").forEach { param ->
                val parts = param.split("=", limit = 2)
                val name = parts[0]
                if (name.isNotBlank()) {
                    inputs.add(
                        InputSource(
                            inputType = InputType.URL_PARAM,
                            name = name,
                            formAction = baseUrl,
                            formMethod = "GET",
                            fieldType = null,
                            location = "URL query parameter: ?$name=...",
                        )
                    )
                }
            }
        } catch (_: Exception) { }
        return inputs
    }
    /** Discover links on the page that have query parameters */
    private fun discoverLinkParams(currentUrl: String): List<InputSource> {
        val inputs = mutableListOf<InputSource>()
        val links = driver.findElements(By.tagName("a"))
        val seenParams = mutableSetOf<String>()
        for (link in links) {
            val href = link.getAttribute("href") ?: continue
            try {
                val uri = URI(href)
                val query = uri.query ?: continue
                val baseUrl = "${uri.scheme}://${uri.authority}${uri.path}"
                query.split("&").forEach { param ->
                    val parts = param.split("=", limit = 2)
                    val name = parts[0]
                    val key = "$baseUrl:$name"
                    if (name.isNotBlank() && key !in seenParams) {
                        seenParams.add(key)
                        inputs.add(
                            InputSource(
                                inputType = InputType.URL_PARAM,
                                name = name,
                                formAction = baseUrl,
                                formMethod = "GET",
                                fieldType = null,
                                location = "Link parameter in <a href>: $name → $baseUrl",
                            )
                        )
                    }
                }
            } catch (_: Exception) { }
        }
        return inputs
    }
    private fun discoverContentEditables(): List<InputSource> {
        val inputs = mutableListOf<InputSource>()
        val editables = driver.findElements(By.cssSelector("[contenteditable='true']"))
        for ((index, element) in editables.withIndex()) {
            val name = element.getAttribute("id") ?: element.getAttribute("name") ?: "contenteditable_$index"
            inputs.add(
                InputSource(
                    inputType = InputType.STANDALONE_INPUT,
                    name = name,
                    formAction = driver.currentUrl,
                    formMethod = null,
                    fieldType = "contenteditable",
                    location = "Content-editable element: $name",
                )
            )
        }
        return inputs
    }
    private fun resolveUrl(base: String, relative: String): String {
        if (relative.isBlank()) return base
        if (relative.startsWith("http://") || relative.startsWith("https://")) return relative
        return try {
            URI(base).resolve(relative).toString()
        } catch (_: Exception) {
            "$base/$relative"
        }
    }
}
