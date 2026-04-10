package sqli

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.WebDriverWait
import java.net.HttpURLConnection
import java.net.URI
import java.time.Duration

/**
 * tests discovered input sources for SQL injection vulnerabilities
 * uses both Selenium-based form submission and direct HTTP requests for URL params
 */
class SqlInjectionTester(
    private val driver: WebDriver,
    private val payloads: List<String> = SqlPayloads.all,
) {

    /**
     *run a full scan: discover inputs and test each one
     */
    fun scan(targetUrl: String): ScanReport {
        val discoverer = InputDiscoverer(driver)
        val inputs = discoverer.discoverAll(targetUrl)

        println("Discovered ${inputs.size} input source(s) on $targetUrl")
        inputs.forEachIndexed { i, inp ->
            println("  [${i + 1}] ${inp.inputType}: ${inp.name} — ${inp.location}")
        }

        val allResults = mutableListOf<InjectionResult>()

        for (input in inputs) {
            println("\nTesting input: ${input.name} (${input.inputType})...")
            val results = when (input.inputType) {
                InputType.FORM_FIELD -> testFormField(targetUrl, input)
                InputType.URL_PARAM -> testUrlParam(input)
                InputType.COOKIE -> testCookie(targetUrl, input)
                InputType.STANDALONE_INPUT -> testStandaloneInput(targetUrl, input)
            }
            allResults.addAll(results)
            val vulnCount = results.count { it.vulnerable }
            if (vulnCount > 0) {
                println("  ⚠ Found $vulnCount potential vulnerability/vulnerabilities!")
            } else {
                println("  ✓ No vulnerabilities detected.")
            }
        }

        return ScanReport(
            targetUrl = targetUrl,
            inputsFound = inputs,
            totalInputs = inputs.size,
            totalTests = allResults.size,
            vulnerabilitiesFound = allResults.count { it.vulnerable },
            results = allResults,
        )
    }

    /**
     * test a form field by navigating to the page, injecting payloads into the field,
     * and submitting the form via Selenium
     */
    private fun testFormField(pageUrl: String, input: InputSource): List<InjectionResult> {
        val results = mutableListOf<InjectionResult>()

        //get baseline response by submitting with normal value
        val baselinePageSource = submitFormAndGetResponse(pageUrl, input, "normalvalue123")
        val baselineLength = baselinePageSource?.length ?: 0

        for (payload in payloads) {
            try {
                val startTime = System.currentTimeMillis()
                val responseSource = submitFormAndGetResponse(pageUrl, input, payload)
                val elapsed = System.currentTimeMillis() - startTime

                if (responseSource == null) {
                    results.add(
                        InjectionResult(
                            inputSource = input,
                            payload = payload,
                            vulnerable = false,
                            evidence = "Could not submit form or retrieve response",
                        )
                    )
                    continue
                }

                val (vulnerable, evidence) = analyzeResponse(
                    responseText = responseSource,
                    baselineText = baselinePageSource ?: "",
                    baselineLength = baselineLength,
                    payload = payload,
                    elapsedMs = elapsed,
                )

                results.add(
                    InjectionResult(
                        inputSource = input,
                        payload = payload,
                        vulnerable = vulnerable,
                        evidence = evidence,
                        responseSnippet = responseSource.take(300),
                    )
                )
            } catch (e: Exception) {
                results.add(
                    InjectionResult(
                        inputSource = input,
                        payload = payload,
                        vulnerable = false,
                        evidence = "Error during test: ${e.message}",
                    )
                )
            }
        }

        return results
    }

    /**
     * submit a form via Selenium by finding the input, clearing it, typing the payload, and submitting
     */
    private fun submitFormAndGetResponse(pageUrl: String, input: InputSource, value: String): String? {
        return try {
            driver.get(pageUrl)
            Thread.sleep(500) // Wait for page to load

            val element: WebElement? = try {
                driver.findElement(By.name(input.name))
            } catch (_: Exception) {
                try {
                    driver.findElement(By.id(input.name))
                } catch (_: Exception) {
                    null
                }
            }

            if (element == null) return null

            element.clear()
            element.sendKeys(value)

            //try to find and click the submit button within the same form
            val form = try {
                element.findElement(By.xpath("./ancestor::form"))
            } catch (_: Exception) {
                null
            }

            if (form != null) {
                val submitBtn = try {
                    form.findElement(By.cssSelector("input[type='submit'], button[type='submit'], button:not([type])"))
                } catch (_: Exception) {
                    null
                }
                if (submitBtn != null) {
                    submitBtn.click()
                } else {
                    form.submit()
                }
            } else {
                element.submit()
            }

            Thread.sleep(1000) //wait for response
            driver.pageSource
        } catch (e: Exception) {
            null
        }
    }

    /**
     *test a URL parameter by making direct HTTP requests with injected payloads
     */
    private fun testUrlParam(input: InputSource): List<InjectionResult> {
        val results = mutableListOf<InjectionResult>()
        val baseUrl = input.formAction ?: return results
        val baselineResponse = httpGet(baseUrl, mapOf(input.name to "normalvalue123"))
        val baselineLength = baselineResponse?.second?.length ?: 0
        val baselineStatus = baselineResponse?.first ?: 200

        for (payload in payloads) {
            try {
                val startTime = System.currentTimeMillis()
                val response = httpGet(baseUrl, mapOf(input.name to payload))
                val elapsed = System.currentTimeMillis() - startTime

                if (response == null) {
                    results.add(
                        InjectionResult(
                            inputSource = input,
                            payload = payload,
                            vulnerable = false,
                            evidence = "Request failed",
                        )
                    )
                    continue
                }

                val (statusCode, responseBody) = response
                val evidenceParts = mutableListOf<String>()
                var vulnerable = false
                val errorMatch = checkSqlErrors(responseBody)
                if (errorMatch != null) {
                    vulnerable = true
                    evidenceParts.add("SQL error detected: $errorMatch")
                }
                if (isTimePayload(payload) && elapsed >= 2500) {
                    vulnerable = true
                    evidenceParts.add("Response delayed ${elapsed}ms (possible time-based blind SQLi)")
                }

                if (isBooleanPayload(payload)) {
                    val diffRatio = kotlin.math.abs(responseBody.length - baselineLength).toDouble() / maxOf(baselineLength, 1)
                    if (diffRatio > 0.15 && responseBody.length > baselineLength) {
                        vulnerable = true
                        evidenceParts.add("Response length changed: $baselineLength → ${responseBody.length} (${(diffRatio * 100).toInt()}% diff)")
                    }
                }
                if (statusCode == 500 && baselineStatus != 500) {
                    vulnerable = true
                    evidenceParts.add("Server returned 500 (baseline was $baselineStatus)")
                }

                val evidence = if (evidenceParts.isEmpty()) "No vulnerability indicators detected" else evidenceParts.joinToString("; ")

                results.add(
                    InjectionResult(
                        inputSource = input,
                        payload = payload,
                        vulnerable = vulnerable,
                        evidence = evidence,
                        statusCode = statusCode,
                        responseSnippet = responseBody.take(300),
                    )
                )
            } catch (e: Exception) {
                results.add(
                    InjectionResult(
                        inputSource = input,
                        payload = payload,
                        vulnerable = false,
                        evidence = "Error: ${e.message}",
                    )
                )
            }
        }

        return results
    }

    /**
     * test a cookie by sending requests with injected cookie values
     */
    private fun testCookie(pageUrl: String, input: InputSource): List<InjectionResult> {
        val results = mutableListOf<InjectionResult>()

        for (payload in payloads) {
            try {
                driver.get(pageUrl)
                val script = "document.cookie = '${input.name}=${payload.replace("'", "\\'")}; path=/';"
                try {
                    (driver as org.openqa.selenium.JavascriptExecutor).executeScript(script)
                } catch (_: Exception) { }

                driver.navigate().refresh()
                Thread.sleep(1000)

                val responseSource = driver.pageSource
                val errorMatch = checkSqlErrors(responseSource)

                results.add(
                    InjectionResult(
                        inputSource = input,
                        payload = payload,
                        vulnerable = errorMatch != null,
                        evidence = if (errorMatch != null) "SQL error detected: $errorMatch" else "No vulnerability indicators detected",
                        responseSnippet = responseSource.take(300),
                    )
                )
            } catch (e: Exception) {
                results.add(
                    InjectionResult(
                        inputSource = input,
                        payload = payload,
                        vulnerable = false,
                        evidence = "Error: ${e.message}",
                    )
                )
            }
        }

        return results
    }
    private fun testStandaloneInput(pageUrl: String, input: InputSource): List<InjectionResult> {
        return testFormField(pageUrl, input)
    }
    private fun analyzeResponse(
        responseText: String,
        baselineText: String,
        baselineLength: Int,
        payload: String,
        elapsedMs: Long,
    ): Pair<Boolean, String> {
        val evidenceParts = mutableListOf<String>()
        var vulnerable = false

        val errorMatch = checkSqlErrors(responseText)
        if (errorMatch != null) {
            vulnerable = true
            evidenceParts.add("SQL error detected: $errorMatch")
        }
        if (isTimePayload(payload) && elapsedMs >= 2500) {
            vulnerable = true
            evidenceParts.add("Response delayed ${elapsedMs}ms (possible time-based blind SQLi)")
        }
        if (isBooleanPayload(payload) && baselineLength > 0) {
            val diffRatio = kotlin.math.abs(responseText.length - baselineLength).toDouble() / baselineLength
            if (diffRatio > 0.15 && responseText.length > baselineLength) {
                vulnerable = true
                evidenceParts.add("Response length changed: $baselineLength → ${responseText.length} (${(diffRatio * 100).toInt()}% diff)")
            }
        }

        val evidence = if (evidenceParts.isEmpty()) "No vulnerability indicators detected" else evidenceParts.joinToString("; ")
        return Pair(vulnerable, evidence)
    }

    /** check response text for SQL error patterns */
    private fun checkSqlErrors(text: String): String? {
        for (pattern in SqlPayloads.errorPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val start = maxOf(0, match.range.first - 30)
                val end = minOf(text.length, match.range.last + 30)
                return text.substring(start, end).trim()
            }
        }
        return null
    }

    private fun isTimePayload(payload: String): Boolean {
        val upper = payload.uppercase()
        return "SLEEP" in upper || "WAITFOR" in upper || "pg_sleep" in payload
    }

    private fun isBooleanPayload(payload: String): Boolean {
        return "AND 1=1" in payload || "OR '1'='1" in payload || "OR 1=1" in payload || "OR 'x'='x" in payload
    }
    private fun httpGet(baseUrl: String, params: Map<String, String>): Pair<Int, String>? {
        return try {
            val queryString = params.entries.joinToString("&") { "${it.key}=${java.net.URLEncoder.encode(it.value, "UTF-8")}" }
            val fullUrl = if (queryString.isNotEmpty()) "$baseUrl?$queryString" else baseUrl
            val connection = URI(fullUrl).toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("User-Agent", "SQLi-Tester/1.0")

            val statusCode = connection.responseCode
            val body = try {
                connection.inputStream.bufferedReader().readText()
            } catch (_: Exception) {
                try {
                    connection.errorStream?.bufferedReader()?.readText() ?: ""
                } catch (_: Exception) {
                    ""
                }
            }
            connection.disconnect()
            Pair(statusCode, body)
        } catch (_: Exception) {
            null
        }
    }
}
