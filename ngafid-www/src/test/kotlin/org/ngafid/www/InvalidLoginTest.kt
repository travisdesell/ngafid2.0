package org.ngafid.www

import org.junit.jupiter.api.*
import org.openqa.selenium.*
import org.openqa.selenium.edge.*


class InvalidLoginTest {

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


}
