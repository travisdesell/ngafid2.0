package org.ngafid.www

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.openqa.selenium.edge.EdgeDriver
import org.openqa.selenium.edge.EdgeOptions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

class UnauthorizedAccessTest {

    @Test
    fun unauthenticatedUserRedirectedToLogin() {
        val options = EdgeOptions()
        options.addArguments("--headless=new")
        val driver = EdgeDriver(options)
        val wait = WebDriverWait(driver, Duration.ofSeconds(10))
        try {
            driver.get("http://localhost:8181/protected/uploads")

            wait.until {
                driver.currentUrl.contains("login") || driver.pageSource.contains("Login")
            }
            assertTrue(driver.pageSource.contains("Login"), "Expected unauthenticated user to be redirected to login")
        } finally {
            driver.quit()
        }
    }
}
