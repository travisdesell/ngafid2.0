package sqli

data class InjectionResult(
    val inputSource: InputSource,
    val payload: String,
    val vulnerable: Boolean,
    val evidence: String,
    val statusCode: Int? = null,
    val responseSnippet: String? = null,
)

data class ScanReport(
    val targetUrl: String,
    val inputsFound: List<InputSource>,
    val totalInputs: Int,
    val totalTests: Int,
    val vulnerabilitiesFound: Int,
    val results: List<InjectionResult>,
) {
    fun printSummary() {
        println("=" .repeat(80))
        println("SQL INJECTION SCAN REPORT")
        println("=" .repeat(80))
        println("Target URL: $targetUrl")
        println("Inputs discovered: $totalInputs")
        println("Total tests run: $totalTests")
        println("Vulnerabilities found: $vulnerabilitiesFound")
        println("-".repeat(80))

        if (vulnerabilitiesFound > 0) {
            println("\n⚠ VULNERABLE INPUTS:")
            results.filter { it.vulnerable }.forEach { r ->
                println("  Input: ${r.inputSource.name} (${r.inputSource.inputType})")
                println("  Location: ${r.inputSource.location}")
                println("  Payload: ${r.payload}")
                println("  Evidence: ${r.evidence}")
                println()
            }
        } else {
            println("\nNo SQL injection vulnerabilities detected.")
        }

        println("-".repeat(80))
        println("All tested inputs:")
        inputsFound.forEach { inp ->
            val vulnCount = results.count { it.inputSource == inp && it.vulnerable }
            val status = if (vulnCount > 0) "VULNERABLE ($vulnCount payloads)" else "CLEAN"
            println("  [$status] ${inp.name} (${inp.inputType}) - ${inp.location}")
        }
        println("=" .repeat(80))
    }
}
