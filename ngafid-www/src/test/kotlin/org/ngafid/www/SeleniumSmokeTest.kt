package org.ngafid.www

import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import java.time.Duration

class SeleniumSmokeTest {
    @Test
    fun homePageHasTitle() {
        driver.get(baseUrl)
        val title = driver.title
        assertTrue(!title.isNullOrBlank(), "Expected a page title when navigating to $baseUrl")
    }

    companion object {
        private lateinit var driver: WebDriver
        private val baseUrl: String = run {
            val url = System.getProperty("ngafid.baseUrl")
                ?: System.getenv("NGAFID_BASE_URL")
                ?: "http://localhost:8181/"
            if (url.endsWith("/")) url else "$url/"
        }
        @BeforeAll
        @JvmStatic
        fun verifyServerIsUp() {
            try {
                java.net.URL(baseUrl).openConnection().connect()
            } catch (e: Exception) {
                throw IllegalStateException(
                    "NGAFID server is not reachable at $baseUrl, check if the server is up first"
                )
            }
        }
        @JvmStatic
        @BeforeAll
        fun setUpDriver() {
            WebDriverManager.chromedriver().setup()
            val options = ChromeOptions()
            options.addArguments("--headless=new", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage")
            driver = ChromeDriver(options)
        }

        @JvmStatic
        @AfterAll
        fun tearDownDriver() {
            if (::driver.isInitialized) {
                driver.quit()
            }
        }
    }
}
