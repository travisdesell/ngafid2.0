package sqli

import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeDriverService
import org.openqa.selenium.chrome.ChromeOptions
import java.io.File

/**
 * Usage:
 *   ./gradlew test -Dtarget.url=http://localhost:8080
 *
 * Multiple URLs (comma-separated):
 *   ./gradlew test -Dtarget.url=http://example.com/page1,http://example.com/page2
 *
 * For protected pages requiring login:
 *   ./gradlew test -Dtarget.url=http://example.com/protected/page1,http://example.com/protected/page2 \
 *     -Dlogin.url=http://example.com/ \
 *     -Dlogin.username=admin \
 *     -Dlogin.password=secret
 *
 * Or use a .env file:
 *   ./gradlew test -Denv.file=.env
 * Add to env
 * .env file format:
 *   TEST_MODE=true                          # true = localhost, false = production host
 *   LOCALHOST_PORT=8181                     # port for localhost (used when TEST_MODE=true)
 *   PRODUCTION_HOST=ngafidbeta.rit.edu:8181 # production host (used when TEST_MODE=false)
 *   TARGET_PATHS=/protected/welcome,/protected/uploads,/protected/bug_report
 *   TARGET_URL=http://example.com/page1     # (alternative: full URLs, ignores TEST_MODE)
 *   LOGIN_USERNAME=admin
 *   LOGIN_PASSWORD=secret
 *   LOGIN_TRIGGER_TEXT=Login                # click "Login" link to open modal
 *   LOGIN_MODAL_SELECTOR=.modal-dialog      # wait for modal to appear
 *   LOGIN_USERNAME_FIELD=loginEmail          # optional, auto-detected
 *   LOGIN_PASSWORD_FIELD=loginPassword      # optional, auto-detected
 *   LOGIN_SUCCESS_URL_CONTAINS=/protected   # verify redirect after login
 *
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SqlInjectionTest {

    private lateinit var driver: WebDriver
    private lateinit var targetUrls: List<String>

    @BeforeAll
    fun setUp() {
        //load env if specified
        val envFile = System.getProperty("env.file")?.takeIf { it.isNotBlank() }
            ?: System.getenv("ENV_FILE")?.takeIf { it.isNotBlank() }
        if (envFile != null) {
            Authenticator.loadEnvFile(envFile)
        }

        //determine base URL from TEST_MODE
        val testMode = System.getProperty("test.mode")?.takeIf { it.isNotBlank() }
            ?: System.getenv("TEST_MODE")?.takeIf { it.isNotBlank() }
        val localhostPort = System.getProperty("localhost.port")?.takeIf { it.isNotBlank() }
            ?: System.getenv("LOCALHOST_PORT")?.takeIf { it.isNotBlank() }
            ?: "8181"
        val productionHost = System.getProperty("production.host")?.takeIf { it.isNotBlank() }
            ?: System.getenv("PRODUCTION_HOST")?.takeIf { it.isNotBlank() }
            ?: "ngafidbeta.rit.edu:8181"

        val baseUrl = if (testMode.equals("true", ignoreCase = true)) {
            "http://localhost:$localhostPort"
        } else {
            "http://$productionHost"
        }

        if (testMode != null) {
            println("TEST_MODE=$testMode -> base URL: $baseUrl")
        }

        //check for TARGET_PATHS first (works with TEST_MODE), then fall back to TARGET_URL
        val targetPaths = System.getProperty("target.paths")?.takeIf { it.isNotBlank() }
            ?: System.getenv("TARGET_PATHS")?.takeIf { it.isNotBlank() }
        val rawUrls = System.getProperty("target.url")?.takeIf { it.isNotBlank() }
            ?: System.getenv("TARGET_URL")?.takeIf { it.isNotBlank() }

        targetUrls = if (targetPaths != null) {
            targetPaths.split(",").map { it.trim() }.filter { it.isNotBlank() }.map { path ->
                val normalizedPath = if (path.startsWith("/")) path else "/$path"
                "$baseUrl$normalizedPath"
            }
        } else if (rawUrls != null) {
            rawUrls.split(",").map { it.trim() }.filter { it.isNotBlank() }
        } else {
            error("No target URLs provided. Set TARGET_PATHS or TARGET_URL in .env, or pass -Dtarget.url=<url>")
        }

        require(targetUrls.isNotEmpty()) { "No valid URLs provided." }
        targetUrls.forEach { url ->
            require(url.startsWith("http://") || url.startsWith("https://")) {
                "Target URL must start with http:// or https:// — got: $url"
            }
        }

        val chromeBinary = (System.getProperty("chrome.binary")?.takeIf { it.isNotBlank() })
            ?: System.getenv("CHROME_BINARY")?.takeIf { it.isNotBlank() }

        val chromeDriverPath = (System.getProperty("chromedriver.path")?.takeIf { it.isNotBlank() })
            ?: System.getenv("CHROMEDRIVER_PATH")?.takeIf { it.isNotBlank() }

        val options = ChromeOptions().apply {
            if (chromeBinary != null) setBinary(chromeBinary)
            addArguments("--headless=new")
            addArguments("--no-sandbox")
            addArguments("--disable-dev-shm-usage")
            addArguments("--disable-gpu")
            addArguments("--window-size=1920,1080")
            addArguments("--disable-extensions")
            addArguments("--disable-popup-blocking")
            addArguments("--ignore-certificate-errors")
        }

        if (chromeDriverPath != null) {
            val service = ChromeDriverService.Builder()
                .usingDriverExecutable(File(chromeDriverPath))
                .build()
            driver = ChromeDriver(service, options)
        } else {
            WebDriverManager.chromedriver().setup()
            driver = ChromeDriver(options)
        }

        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(5))
        driver.manage().timeouts().pageLoadTimeout(java.time.Duration.ofSeconds(30))

        println("SQL Injection Tester initialized")
        println("Target URLs (${targetUrls.size}): ${targetUrls.joinToString(", ")}")
        println("Browser: Chrome (headless)")

        //perform login if configured
        val loginConfig = Authenticator.fromConfig()
        if (loginConfig != null) {
            val authenticator = Authenticator(driver)
            val loginSuccess = authenticator.login(loginConfig)
            if (!loginSuccess) {
                error("Login failed! Check your credentials and login configuration.")
            }
        } else {
            println("No login configured — scanning without authentication")
        }
    }

    @AfterAll
    fun tearDown() {
        if (::driver.isInitialized) {
            driver.quit()
        }
    }

    @Test
    @DisplayName("Discover all input sources on the target page(s)")
    fun testDiscoverInputs() {
        val discoverer = InputDiscoverer(driver)
        var totalInputs = 0

        for (url in targetUrls) {
            val inputs = discoverer.discoverAll(url)
            totalInputs += inputs.size

            println("\n=== INPUT DISCOVERY: $url ===")
            println("Found ${inputs.size} input source(s):")
            inputs.forEachIndexed { i, inp ->
                println("  [${i + 1}] ${inp.inputType}: ${inp.name}")
                println("       Location: ${inp.location}")
                inp.formAction?.let { println("       Action: $it") }
                inp.formMethod?.let { println("       Method: $it") }
                inp.fieldType?.let { println("       Field type: $it") }
            }
        }

        if (totalInputs == 0) {
            println("No input sources found across ${targetUrls.size} URL(s) — skipping.")
        }
    }

    @Test
    @DisplayName("Test all inputs for SQL injection vulnerabilities")
    fun testSqlInjection() {
        val tester = SqlInjectionTester(driver)
        val allReports = mutableListOf<ScanReport>()

        for ((index, url) in targetUrls.withIndex()) {
            println("\n>>> Scanning URL ${index + 1}/${targetUrls.size}: $url")
            val report = tester.scan(url)

            if (report.totalInputs == 0) {
                println("No input sources found on $url — skipping.")
                continue
            }

            allReports.add(report)
            report.printSummary()

            val suffix = if (targetUrls.size > 1) "-${index + 1}" else ""
            ReportGenerator.generateHtmlReport(report, "build/reports/sqli-report${suffix}.html")
            ReportGenerator.generateTextReport(report, "build/reports/sqli-report${suffix}.txt")
        }

        val totalInputs = allReports.sumOf { it.totalInputs }
        val totalTests = allReports.sumOf { it.totalTests }
        val totalVulns = allReports.sumOf { it.vulnerabilitiesFound }

        println("\n=== OVERALL RESULTS ===")
        println("URLs scanned: ${targetUrls.size}")
        println("Total inputs tested: $totalInputs")
        println("Total injection tests: $totalTests")
        println("Vulnerabilities found: $totalVulns")

        if (totalVulns > 0) {
            val vulnDetails = allReports.flatMap { report ->
                report.results.filter { it.vulnerable }.map { r ->
                    "  - [${report.targetUrl}] Input '${r.inputSource.name}' (${r.inputSource.inputType}): " +
                        "payload='${r.payload}', evidence='${r.evidence}'"
                }
            }.joinToString("\n")
            fail<Unit>(
                "SQL injection vulnerabilities detected!\n" +
                    "Found $totalVulns vulnerability/vulnerabilities across ${targetUrls.size} URL(s):\n$vulnDetails\n" +
                    "See build/reports/ for full details."
            )
        }
    }

    @Test
    @DisplayName("Test with error-based payloads only (fast scan)")
    fun testErrorBasedOnly() {
        val tester = SqlInjectionTester(driver, SqlPayloads.errorBased)
        val totalVulns = scanAllUrls(tester, "sqli-error-based-report")

        if (totalVulns > 0) {
            fail<Unit>(
                "Error-based SQL injection detected in $totalVulns test(s). " +
                    "See build/reports/ for details."
            )
        }
    }

    @Test
    @DisplayName("Test with tautology-based payloads only")
    fun testTautologyOnly() {
        val tester = SqlInjectionTester(driver, SqlPayloads.tautology)
        val totalVulns = scanAllUrls(tester, "sqli-tautology-report")

        if (totalVulns > 0) {
            fail<Unit>(
                "Tautology-based SQL injection detected in $totalVulns test(s). " +
                    "See build/reports/ for details."
            )
        }
    }

    @Test
    @DisplayName("Test with time-based blind payloads only")
    fun testTimeBasedOnly() {
        val tester = SqlInjectionTester(driver, SqlPayloads.timeBased)
        val totalVulns = scanAllUrls(tester, "sqli-time-based-report")

        if (totalVulns > 0) {
            fail<Unit>(
                "Time-based blind SQL injection detected in $totalVulns test(s). " +
                    "See build/reports/ for details."
            )
        }
    }

    /**
     * Scan all target URLs with the given tester and generate per-URL reports.
     * Returns the total number of vulnerabilities found across all URLs.
     */
    private fun scanAllUrls(tester: SqlInjectionTester, reportPrefix: String): Int {
        var totalVulns = 0
        for ((index, url) in targetUrls.withIndex()) {
            println("\n>>> Scanning URL ${index + 1}/${targetUrls.size}: $url")
            val report = tester.scan(url)

            if (report.totalInputs == 0) {
                println("No input sources found on $url — skipping.")
                continue
            }

            report.printSummary()
            totalVulns += report.vulnerabilitiesFound

            val suffix = if (targetUrls.size > 1) "-${index + 1}" else ""
            ReportGenerator.generateHtmlReport(report, "build/reports/${reportPrefix}${suffix}.html")
        }
        return totalVulns
    }
}
