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
        private const val baseUrl = "http://localhost:8181/"
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

}
