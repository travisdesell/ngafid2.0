package org.ngafid.www

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.edge.EdgeDriver
import org.openqa.selenium.edge.EdgeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait

import java.time.Duration

class LoginFlowTest {
    companion object {
        private lateinit var driver: WebDriver
        private fun requireEnv(name: String): String =
            System.getenv(name)
                ?: throw IllegalStateException("Missing required env var: $name")
        @JvmStatic
        @BeforeAll
        fun setUpDriver() {
            val options = EdgeOptions()
            options.addArguments("--headless=new", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage")
            LoginFlowTest.Companion.driver = EdgeDriver(options)
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
    fun loginViaModalSucceeds() {
        val baseurl = baseUrlFromProperties()
        val email = requireEnv("NGAFID_TEST_EMAIL")
        val password = requireEnv("NGAFID_TEST_PASSWORD")
        driver.get(baseurl)
        val wait = WebDriverWait(driver, Duration.ofSeconds(10))
        wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Login"))).click()
        val modal = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".modal-dialog")))
        modal.findElement(By.id("loginEmail")).sendKeys(email)
        modal.findElement(By.id("loginPassword")).sendKeys(password)
        modal.findElement(By.cssSelector("button[type='submit']")).click()
        wait.until(ExpectedConditions.invisibilityOf(modal))
        wait.until { driver.currentUrl.contains("/protected") }
        println(driver.currentUrl)
        assertTrue(driver.currentUrl.contains("/protected"), "Expected redirect to protected area after login")
    }
}
