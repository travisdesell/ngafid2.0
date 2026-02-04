package org.ngafid.www

import org.junit.jupiter.api.*
import org.openqa.selenium.*
import org.openqa.selenium.edge.*
import org.openqa.selenium.support.ui.*
import java.time.Duration
import org.junit.jupiter.api.Assertions.assertTrue
class InvalidLoginTest {

    companion object {
        private lateinit var driver: WebDriver
        private const val baseUrl = "http://localhost:8181/"

        @BeforeAll
        @JvmStatic
        fun setup() {
            val options = EdgeOptions()
            options.addArguments("--headless=new")
            driver = EdgeDriver(options)
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            driver.quit()
        }
    }
    @Test
    fun invalidPasswordShowsError() {
        driver.get(baseUrl)
        val wait = WebDriverWait(driver, Duration.ofSeconds(10))
        wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Login"))).click()
        val modal = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".modal-dialog")))

        modal.findElement(By.id("loginEmail")).sendKeys("fake@example.com")
        modal.findElement(By.id("loginPassword")).sendKeys("wrongpassword")
        modal.findElement(By.cssSelector("button[type='submit']")).click()
        assertTrue(
            wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), "Invalid")))
    }
}
