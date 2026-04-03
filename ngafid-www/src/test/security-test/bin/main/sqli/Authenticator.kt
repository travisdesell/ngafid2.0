package sqli
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration
/**
 * Handles authentication/login before scanning protected pages.
 *
 * Supports two login flows:
 * 1. Direct login page: navigate to URL, fill fields, submit
 * 2. Modal-based login: navigate to URL, click a trigger (link/button) to open a modal,
 *    fill fields inside the modal, submit, wait for modal to close
 *
 * Credentials can come from system properties, env vars, or a .env file.
 */
class Authenticator(private val driver: WebDriver) {
    data class LoginConfig(
        val loginUrl: String,
        val username: String,
        val password: String,
        val usernameField: String? = null,
        val passwordField: String? = null,
        val submitSelector: String? = null,
        val triggerText: String? = null,
        val triggerSelector: String? = null,
        val modalSelector: String? = null,
        val successUrlContains: String? = null,
    )
    companion object {
        fun fromConfig(): LoginConfig? {
            val loginUrl = prop("login.url", "LOGIN_URL") ?: return null
            val username = prop("login.username", "LOGIN_USERNAME")
                ?: error("login.url is set but login.username is missing. Provide -Dlogin.username=<user> or set LOGIN_USERNAME env var.")
            val password = prop("login.password", "LOGIN_PASSWORD")
                ?: error("login.url is set but login.password is missing. Provide -Dlogin.password=<pass> or set LOGIN_PASSWORD env var.")
            return LoginConfig(
                loginUrl = loginUrl,
                username = username,
                password = password,
                usernameField = prop("login.username.field", "LOGIN_USERNAME_FIELD"),
                passwordField = prop("login.password.field", "LOGIN_PASSWORD_FIELD"),
                submitSelector = prop("login.submit.selector", "LOGIN_SUBMIT_SELECTOR"),
                triggerText = prop("login.trigger.text", "LOGIN_TRIGGER_TEXT"),
                triggerSelector = prop("login.trigger.selector", "LOGIN_TRIGGER_SELECTOR"),
                modalSelector = prop("login.modal.selector", "LOGIN_MODAL_SELECTOR"),
                successUrlContains = prop("login.success.url.contains", "LOGIN_SUCCESS_URL_CONTAINS"),
            )
        }
        private fun prop(sysProp: String, envVar: String): String? {
            return System.getProperty(sysProp)?.takeIf { it.isNotBlank() }
                ?: System.getenv(envVar)?.takeIf { it.isNotBlank() }
        }
        fun loadEnvFile(path: String) {
            val file = java.io.File(path)
            if (!file.exists()) {
                println("Warning: .env file not found at $path")
                return
            }
            println("Loading credentials from $path")
            val mapping = mapOf(
                "LOGIN_URL" to "login.url",
                "LOGIN_USERNAME" to "login.username",
                "LOGIN_PASSWORD" to "login.password",
                "LOGIN_USERNAME_FIELD" to "login.username.field",
                "LOGIN_PASSWORD_FIELD" to "login.password.field",
                "LOGIN_SUBMIT_SELECTOR" to "login.submit.selector",
                "LOGIN_TRIGGER_TEXT" to "login.trigger.text",
                "LOGIN_TRIGGER_SELECTOR" to "login.trigger.selector",
                "LOGIN_MODAL_SELECTOR" to "login.modal.selector",
                "LOGIN_SUCCESS_URL_CONTAINS" to "login.success.url.contains",
                "TARGET_URL" to "target.url",
                "TARGET_PATHS" to "target.paths",
                "TEST_MODE" to "test.mode",
                "LOCALHOST_PORT" to "localhost.port",
                "PRODUCTION_HOST" to "production.host",
            )
            file.readLines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                    val eqIndex = trimmed.indexOf('=')
                    if (eqIndex > 0) {
                        val key = trimmed.substring(0, eqIndex).trim()
                        val value = trimmed.substring(eqIndex + 1).trim()
                            .removeSurrounding("\"")
                            .removeSurrounding("'")
                        val sysProp = mapping[key]
                        if (sysProp != null) {
                            System.setProperty(sysProp, value)
                        }
                    }
                }
            }
        }
    }
    fun login(config: LoginConfig): Boolean {
        val wait = WebDriverWait(driver, Duration.ofSeconds(10))
        println("\n=== AUTHENTICATION ===")
        println("Navigating to: ${config.loginUrl}")
        println("Config: triggerText=${config.triggerText}, modalSelector=${config.modalSelector}, " +
            "usernameField=${config.usernameField}, passwordField=${config.passwordField}, " +
            "successUrlContains=${config.successUrlContains}")
        driver.get(config.loginUrl)
        Thread.sleep(1000)
        val modalContainer: WebElement? = if (config.triggerText != null || config.triggerSelector != null) {
            openLoginModal(config, wait)
        } else {
            null
        }
        val usernameElement = findUsernameField(config.usernameField, modalContainer)
        if (usernameElement == null) {
            println("ERROR: Could not find username field")
            println("Page title: ${driver.title}")
            println("Tip: Set -Dlogin.username.field=<fieldname> (e.g. loginEmail)")
            return false
        }
        println("Found username field: ${usernameElement.getAttribute("id") ?: usernameElement.getAttribute("name")}")
        val passwordElement = findPasswordField(config.passwordField, modalContainer)
        if (passwordElement == null) {
            println("ERROR: Could not find password field")
            println("Tip: Set -Dlogin.password.field=<fieldname> (e.g. loginPassword)")
            return false
        }
        println("Found password field: ${passwordElement.getAttribute("id") ?: passwordElement.getAttribute("name")}")
        fillField(usernameElement, config.username, wait)
        fillField(passwordElement, config.password, wait)
        val urlBeforeSubmit = driver.currentUrl
        submitLogin(config.submitSelector, passwordElement, modalContainer)
        Thread.sleep(1000)
        if (modalContainer != null) {
            try {
                wait.until(ExpectedConditions.invisibilityOf(modalContainer))
                println("Modal closed after login")
            } catch (_: Exception) {
                println("Warning: Modal may not have closed")
            }
        }
        if (config.successUrlContains != null) {
            try {
                wait.until { driver.currentUrl.orEmpty().contains(config.successUrlContains) }
                println("URL now contains '${config.successUrlContains}'")
            } catch (_: Exception) {
                println("WARNING: URL does not contain '${config.successUrlContains}' after login")
                println("Current URL: ${driver.currentUrl}")
            }
        } else {
            Thread.sleep(2000)
        }
        val urlAfterSubmit = driver.currentUrl
        val pageSource = driver.pageSource.lowercase()
        val failureIndicators = listOf("invalid", "incorrect", "wrong password", "login failed", "bad credentials")
        val hasFailure = failureIndicators.any { it in pageSource }
        val stillOnLogin = urlAfterSubmit == urlBeforeSubmit && config.successUrlContains == null
        if (stillOnLogin && hasFailure) {
            println("LOGIN FAILED: Error message detected on page")
            println("Current URL: $urlAfterSubmit")
            return false
        }
        println("Login successful! Current URL: $urlAfterSubmit")
        println("======================\n")
        return true
    }
    private fun openLoginModal(config: LoginConfig, wait: WebDriverWait): WebElement? {
        if (config.triggerText != null) {
            println("Clicking login trigger with text: '${config.triggerText}'")
            try {
                val trigger = wait.until(ExpectedConditions.elementToBeClickable(By.linkText(config.triggerText)))
                trigger.click()
            } catch (_: Exception) {
                try {
                    val trigger = wait.until(ExpectedConditions.elementToBeClickable(By.partialLinkText(config.triggerText)))
                    trigger.click()
                } catch (e: Exception) {
                    println("ERROR: Could not find/click trigger with text '${config.triggerText}': ${e.message}")
                    return null
                }
            }
        } else if (config.triggerSelector != null) {
            println("Clicking login trigger: ${config.triggerSelector}")
            try {
                val trigger = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(config.triggerSelector)))
                trigger.click()
            } catch (e: Exception) {
                println("ERROR: Could not find/click trigger '${config.triggerSelector}': ${e.message}")
                return null
            }
        }
        if (config.modalSelector != null) {
            println("Waiting for modal: ${config.modalSelector}")
            try {
                val modal = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.cssSelector(config.modalSelector))
                )
                println("Modal appeared")
                return modal
            } catch (e: Exception) {
                println("ERROR: Modal '${config.modalSelector}' did not appear: ${e.message}")
                return null
            }
        }
        Thread.sleep(1000)
        return null
    }
    private fun findUsernameField(fieldNameOrId: String?, container: WebElement?): WebElement? {
        if (fieldNameOrId != null) return findField(fieldNameOrId, container)
        val selectors = listOf(
            "input[id='loginEmail']",
            "input[type='email']",
            "input[name='username']",
            "input[name='user']",
            "input[name='email']",
            "input[name='login']",
            "input[id='username']",
            "input[id='email']",
            "input[type='text']",
        )
        return findFirstVisible(selectors, container)
    }
    private fun findPasswordField(fieldNameOrId: String?, container: WebElement?): WebElement? {
        if (fieldNameOrId != null) return findField(fieldNameOrId, container)
        val selectors = listOf(
            "input[id='loginPassword']",
            "input[type='password']",
            "input[name='password']",
            "input[name='pass']",
            "input[id='password']",
        )
        return findFirstVisible(selectors, container)
    }
    private fun findField(nameOrId: String, container: WebElement?): WebElement? {
        val strategies = listOf(By.id(nameOrId), By.name(nameOrId), By.cssSelector("[name='$nameOrId'], [id='$nameOrId']"))
        for (by in strategies) {
            try {
                val el = if (container != null) container.findElement(by) else driver.findElement(by)
                if (el != null) return el
            } catch (_: Exception) { }
        }
        return null
    }
    private fun fillField(element: WebElement, value: String, wait: WebDriverWait) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(element))
            element.clear()
            element.sendKeys(value)
        } catch (_: Exception) {
            println("Standard interaction failed, using JavaScript fallback")
            val js = driver as JavascriptExecutor
            js.executeScript("arguments[0].scrollIntoView(true);", element)
            Thread.sleep(300)
            try {
                element.clear()
                element.sendKeys(value)
            } catch (_: Exception) {
                js.executeScript("arguments[0].value = arguments[1];", element, value)
                js.executeScript("arguments[0].dispatchEvent(new Event('input', {bubbles: true}));", element)
            }
        }
    }
    private fun findFirstVisible(selectors: List<String>, container: WebElement?): WebElement? {
        for (selector in selectors) {
            try {
                val el = if (container != null) container.findElement(By.cssSelector(selector)) else driver.findElement(By.cssSelector(selector))
                if (el.isDisplayed) return el
            } catch (_: Exception) { }
        }
        return null
    }
    private fun submitLogin(customSelector: String?, passwordElement: WebElement, container: WebElement?) {
        if (customSelector != null) {
            try {
                val btn = if (container != null) container.findElement(By.cssSelector(customSelector)) else driver.findElement(By.cssSelector(customSelector))
                btn.click()
                return
            } catch (_: Exception) { }
        }
        val submitSelectors = listOf(
            "button[type='submit']", "input[type='submit']", "button:not([type])",
            "button.login", "button.btn-login",
            "input[value='Login']", "input[value='Sign In']", "input[value='Log In']",
        )
        val searchContexts: List<WebElement> = listOfNotNull(
            container,
            try { passwordElement.findElement(By.xpath("./ancestor::form")) } catch (_: Exception) { null },
        )
        for (ctx in searchContexts) {
            for (selector in submitSelectors) {
                try {
                    val btn = ctx.findElement(By.cssSelector(selector))
                    if (btn.isDisplayed) { btn.click(); return }
                } catch (_: Exception) { }
            }
        }
        for (selector in submitSelectors) {
            try {
                val btn = driver.findElement(By.cssSelector(selector))
                if (btn.isDisplayed) { btn.click(); return }
            } catch (_: Exception) { }
        }
        println("WARNING: No submit button found, trying form.submit()")
        try {
            val form = passwordElement.findElement(By.xpath("./ancestor::form"))
            form.submit()
        } catch (e: Exception) {
            println("ERROR: Could not submit login form: ${e.message}")
        }
    }
}
