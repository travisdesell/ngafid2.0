package org.ngafid.www

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.edge.EdgeDriver
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

class UnauthorizedAccessTest {
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
    fun unauthenticatedUserRedirectedToLogin() {
        val baseurl = baseUrlFromProperties()
        val wait = WebDriverWait(driver, Duration.ofSeconds(10))
        try {
            driver.get("${baseurl}protected/uploads")
            wait.until {
                driver.currentUrl?.contains("access_denied") == true ||
                    (driver.pageSource?.contains("Login") == true)
            }
            assertTrue(
                driver.pageSource?.contains("Login") == true,
                "Expected unauthenticated user to be redirected to login"
            )
        } finally {
            driver.quit()
        }
    }
}
