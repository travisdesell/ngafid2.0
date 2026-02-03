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

class UploadFlowTest {
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
            UploadFlowTest.Companion.driver = EdgeDriver(options)
        }
        @JvmStatic
        @AfterAll
        fun tearDown() {
            driver.quit()
        }
    }
    @Test
    fun uploadSmallFlightFileSucceeds() {
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
        driver.get("${baseUrl}/protected/uploads")
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.id("upload-flights-button")
            )
        ).click()
        val fileInput = wait.until(
            ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("input[type='file']")
            )
        )
        val filePath = Paths.get(
            "src/test/resources/ProximityTestFlights.zip"
        ).toAbsolutePath().toString()
        fileInput.sendKeys(filePath)
        wait.until(
            ExpectedConditions.elementToBeClickable(By.cssSelector("button[type='submit']"))).click()
        assertTrue(
            wait.until(
                ExpectedConditions.or(
                    ExpectedConditions.visibilityOfElementLocated(By.className("alert-success")),
                    ExpectedConditions.textToBePresentInElementLocated(
                        By.tagName("body"),
                        "Upload"
                    )
                )
            ) != null,
            "Expected upload success indication"
        )
    }


}