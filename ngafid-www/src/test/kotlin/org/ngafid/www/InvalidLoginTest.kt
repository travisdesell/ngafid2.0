package org.ngafid.www

import org.junit.jupiter.api.*
import org.openqa.selenium.*
import org.openqa.selenium.edge.*
import org.openqa.selenium.support.ui.*
import java.time.Duration
import org.junit.jupiter.api.Assertions.assertTrue
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions

class InvalidLoginTest {

    companion object {
        private lateinit var driver: WebDriver
        @BeforeAll
        @JvmStatic
        fun setup() {
            val options = ChromeOptions()
            options.addArguments("--headless=new")
            driver = ChromeDriver(options)
        }

        @AfterAll
        @JvmStatic
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
    fun invalidPasswordShowsError() {
        val baseurl = baseUrlFromProperties()
        val fakeemail = ("fake@gmail.com")
        val fakepassword = ("aaaaaaa")
        driver.get(baseurl)
        val wait = WebDriverWait(driver, Duration.ofSeconds(10))
        wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Login"))).click()
        val modal = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".modal-dialog")))
        modal.findElement(By.id("loginEmail")).sendKeys(fakeemail)
        modal.findElement(By.id("loginPassword")).sendKeys(fakepassword)
        modal.findElement(By.cssSelector("button[type='submit']")).click()
        assertTrue(
            wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), "Invalid")) || (driver.currentUrl.contains("/#!") ))
    }
}
