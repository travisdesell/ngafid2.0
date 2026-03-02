package org.ngafid.www

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.nio.file.Paths
import java.time.Duration

class FullEndToEndTest {

    @Test
    fun userCanLoginUploadAndLogout() {
        val options = ChromeOptions()
        val driver = ChromeDriver(options)
        val wait = WebDriverWait(driver, Duration.ofSeconds(15))
        try {
            val email = System.getenv("NGAFID_TEST_EMAIL")!!
            val password = System.getenv("NGAFID_TEST_PASSWORD")!!
            driver.get("http://localhost:8181")
            wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Login"))).click()
            val modal = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".modal-dialog")))
            modal.findElement(By.id("loginEmail")).sendKeys(email)
            modal.findElement(By.id("loginPassword")).sendKeys(password)
            modal.findElement(By.cssSelector("button[type='submit']")).click()
            wait.until(ExpectedConditions.invisibilityOf(modal))
            wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Uploads"))).click()
            wait.until {
                driver.currentUrl.contains("/protected/uploads")
            }
            val fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("upload-file-input")))
            val filePath = Paths.get("src/test/resources/ProximityTestFlights.zip").toAbsolutePath().toString()
            fileInput.sendKeys(filePath)
            wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[type='submit']"))).click()
            wait.until {
                driver.pageSource.contains("Processed") ||
                        driver.pageSource.contains("Uploaded")
            }
            assertTrue(driver.pageSource.contains("ProximityTestFlights"))
            wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Account"))).click()
            wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Logout"))).click()
            wait.until {
                driver.pageSource.contains("Login")
            }
            assertTrue(driver.pageSource.contains("Login"))

        } finally {
            driver.quit()
        }
    }
}