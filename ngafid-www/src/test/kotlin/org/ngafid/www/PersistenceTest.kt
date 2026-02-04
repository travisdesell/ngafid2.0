package org.ngafid.www
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeAll
import org.openqa.selenium.By
import org.junit.jupiter.api.Assertions.assertTrue
import org.openqa.selenium.WebDriver
import org.openqa.selenium.edge.EdgeDriver
import org.openqa.selenium.edge.EdgeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration
class PersistenceTest {
    companion object {
        private lateinit var driver: WebDriver
        private const val baseUrl = "http://localhost:8181/"
        private fun requireEnv(name: String): String =
            System.getenv(name)
                ?: throw IllegalStateException("Missing required env var: $name")
        @JvmStatic
        @BeforeAll
        fun setUpDriver() {
            val options = EdgeOptions()
            options.addArguments("--headless=new", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage")
            PersistenceTest.Companion.driver = EdgeDriver(options)
        }
        @JvmStatic
        @AfterAll
        fun tearDown() {
            driver.quit()
        }
    }
    private fun login(wait: WebDriverWait) {
        val email = requireEnv("NGAFID_TEST_EMAIL")
        val password = requireEnv("NGAFID_TEST_PASSWORD")
        wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Login"))).click()
        val modal = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".modal-dialog")))
        modal.findElement(By.id("loginEmail")).sendKeys(email)
        modal.findElement(By.id("loginPassword")).sendKeys(password)
        modal.findElement(By.cssSelector("button[type='submit']")).click()
        println("login successful")
        driver.get(baseUrl)
        println("base url is $baseUrl")
        wait.until(ExpectedConditions.invisibilityOf(modal))
    }
    @Test
    fun userStaysLoggedInAfterRefresh() {
        driver.get(baseUrl)
        println("base url is $baseUrl")
        val wait = WebDriverWait(driver, Duration.ofSeconds(10))
        login(wait)
        driver.navigate().refresh()
         assertTrue(
            driver.pageSource.contains("Account") || driver.pageSource.contains("Status"),
            "Expected logged-in UI after login"
        )
    }
}