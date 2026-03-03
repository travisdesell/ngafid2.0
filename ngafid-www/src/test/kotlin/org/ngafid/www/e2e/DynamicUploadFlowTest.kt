package org.ngafid.www.e2e

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebElement
import java.nio.file.Paths
import kotlin.test.assertTrue

class DynamicUploadFlowTest : TestBase() {

    @ParameterizedTest
    @ValueSource(strings = [
        "ProximityTestFlights.zip"
    ])
    fun uploadFileSucceeds(filename: String) {

        val email = requireEnv("NGAFID_TEST_EMAIL")
        val password = requireEnv("NGAFID_TEST_PASSWORD")

        AuthHelper.login(this, email, password)

        driver.get("${baseUrl}protected/uploads")

        val fileInput = wait.until {
            val js = driver as JavascriptExecutor
            js.executeScript(
                """
                const inputs = Array.from(document.querySelectorAll('#upload-file-input'));
                return inputs.find(i => i.offsetParent !== null && !i.disabled) || null;
                """
            ) as WebElement?
        } ?: error("Upload input not found")

        val filePath = Paths.get(
            "src/test/resources/$filename"
        ).toAbsolutePath().toString()

        fileInput.sendKeys(filePath)

        wait.until {
            driver.pageSource.contains("Uploaded") ||
                    driver.pageSource.contains("Processed")
        }

        assertTrue(
            driver.pageSource.contains(filename.removeSuffix(".zip")),
            "Expected uploaded file to appear"
        )
    }
}