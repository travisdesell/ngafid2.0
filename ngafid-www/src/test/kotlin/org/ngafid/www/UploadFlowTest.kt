package org.ngafid.www

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.openqa.selenium.WebDriver
import org.openqa.selenium.edge.EdgeDriver
import org.openqa.selenium.edge.EdgeOptions


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
}