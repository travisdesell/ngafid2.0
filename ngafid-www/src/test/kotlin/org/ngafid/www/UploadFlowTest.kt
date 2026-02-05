package org.ngafid.www

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.openqa.selenium.WebDriver
import org.openqa.selenium.edge.EdgeDriver
import org.openqa.selenium.edge.EdgeOptions
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import java.time.Duration
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.junit.jupiter.api.Assertions.assertTrue
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebElement


class UploadFlowTest {
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
            UploadFlowTest.Companion.driver = EdgeDriver(options)
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
    fun uploadSmallFlightFileSucceeds() {
        val baseUrl = baseUrlFromProperties()
        val email = requireEnv("NGAFID_TEST_EMAIL")
        val password = requireEnv("NGAFID_TEST_PASSWORD")
        val wait = WebDriverWait(driver, Duration.ofSeconds(15))
        driver.get(baseUrl)
        wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Login"))).click()
        val modal = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".modal-dialog"))
        )
        modal.findElement(By.id("loginEmail")).sendKeys(email)
        modal.findElement(By.id("loginPassword")).sendKeys(password)
        modal.findElement(By.cssSelector("button[type='submit']")).click()
        wait.until(ExpectedConditions.invisibilityOf(modal))
        try {
            val notNowBtn = WebDriverWait(driver, Duration.ofSeconds(3)).until(
                ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[normalize-space()='Not Now']")
                )
            )
            notNowBtn.click()
        } catch (e: Exception) {
        }
        driver.get("${baseUrl}protected/uploads")

        val filePath = this::class.java.classLoader.getResource("ProximityTestFlights.zip")?.toURI()?.let {
            Paths.get(it).toString()
        } ?: error("Test file not found")
        val fileInput = wait.until {
            val js = driver as JavascriptExecutor
            val inputs = js.executeScript("""
            return Array.from(document.querySelectorAll('input[type="file"]')).filter(i => i.offsetParent !== null || i.type === 'file');
            """
            ) as List<WebElement>
            inputs.firstOrNull()
        } ?: error("No file input found")
        fileInput.sendKeys(filePath)
        assertTrue(wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), "Uploaded")), "Expected uploaded flight to be processed"
        )
    }


}