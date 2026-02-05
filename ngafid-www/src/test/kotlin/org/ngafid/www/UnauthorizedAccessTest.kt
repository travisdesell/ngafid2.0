package org.ngafid.www

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.openqa.selenium.WebDriver
import org.openqa.selenium.edge.EdgeDriver
import org.openqa.selenium.edge.EdgeOptions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

class UnauthorizedAccessTest {
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
    fun unauthenticatedUserRedirectedToLogin() {
        val options = EdgeOptions()
        options.addArguments("--headless=new")
        val driver = EdgeDriver(options)
        val wait = WebDriverWait(driver, Duration.ofSeconds(10))
        try {
            driver.get("http://localhost:8181/protected/uploads")
            driver.get(baseUrl)
            println(driver.currentUrl)

            wait.until {
                driver.currentUrl.contains("login") || driver.pageSource.contains("Login")
            }
            assertTrue(driver.pageSource.contains("Login"), "Expected unauthenticated user to be redirected to login")
        } finally {
            driver.quit()
        }
    }
}
