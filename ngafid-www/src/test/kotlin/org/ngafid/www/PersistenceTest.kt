package org.ngafid.www
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeAll
import org.openqa.selenium.By
import org.junit.jupiter.api.Assertions.assertTrue
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration
class PersistenceTest {
    private fun requireEnv(name: String): String =
        System.getenv(name)
            ?: throw IllegalStateException("Missing required env var: $name")
    companion object {
        private lateinit var driver: WebDriver
        private fun requireEnv(name: String): String =
            System.getenv(name)
                ?: throw IllegalStateException("Missing required env var: $name")
        @JvmStatic
        @BeforeAll
        fun setUpDriver() {
            val options = ChromeOptions()
            options.addArguments("--headless=new", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage")
            PersistenceTest.Companion.driver = ChromeDriver(options)
        }
        @JvmStatic
        @AfterAll
        fun tearDown() {
            driver.quit()
        }
    }
    private fun baseUrlFromProperties(): String {
        val props = java.util.Properties()
        Thread.currentThread()
            .contextClassLoader
            .getResourceAsStream("ngafid.properties")
            .use { props.load(it) }

        val port = props.getProperty("ngafid.port")
            ?: error("ngafid.port not found in ngafid.properties")

        return "http://localhost:$port/"
    }
    @Test
    fun userStaysLoggedInAfterRefresh() {
        val wait = WebDriverWait(driver, Duration.ofSeconds(10))
        try {
            val baseurl = baseUrlFromProperties()
            val email = requireEnv("NGAFID_TEST_EMAIL")
            val password = requireEnv("NGAFID_TEST_PASSWORD")
            driver.get(baseurl)
            wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Login"))).click()
            val modal = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".modal-dialog")))
            modal.findElement(By.id("loginEmail")).sendKeys(email)
            modal.findElement(By.id("loginPassword")).sendKeys(password)
            modal.findElement(By.cssSelector("button[type='submit']")).click()
            wait.until(ExpectedConditions.invisibilityOf(modal))
            wait.until { driver.currentUrl?.contains("/protected") == true }
            driver.navigate().refresh()
            wait.until { driver.currentUrl?.contains("/protected") == true }
            assertTrue(driver.findElements(By.linkText("Login")).isEmpty(),"User should remain logged in after refresh"
            )
        } finally {
            driver.quit()
        }
    }

}
