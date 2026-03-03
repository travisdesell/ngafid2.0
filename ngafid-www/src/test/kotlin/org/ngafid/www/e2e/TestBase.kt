package org.ngafid.www.e2e

import org.junit.jupiter.api.*
import org.openqa.selenium.WebDriver
import org.openqa.selenium.edge.ChromeDrivre
import org.openqa.selenium.edge.ChromeOptions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class TestBase {
    protected lateinit var driver: WebDriver
    protected lateinit var wait: WebDriverWait
    protected val baseUrl: String by lazy {
        val port = System.getProperty("ngafid.port")
            ?: System.getenv("NGAFID_PORT")
            ?: "8181"
        "http://localhost:$port/"
    }
    protected fun requireEnv(name: String): String =
        System.getenv(name)
            ?: throw IllegalStateException("Missing required env var: $name")
    @BeforeAll
    fun setupDriver() {
        val options = ChromeOptions()
        options.addArguments("--headless=new")
        options.addArguments("--disable-gpu")
        options.addArguments("--no-sandbox")
        driver = ChromeDriver(options)
        wait = WebDriverWait(driver, Duration.ofSeconds(10))
    }
    @AfterAll
    fun tearDownDriver() {
        driver.quit()
    }
}