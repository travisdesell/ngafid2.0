package sqli

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 *generates HTML and text reports from scan results
 */
object ReportGenerator {

    fun generateHtmlReport(report: ScanReport, outputPath: String = "sqli-report.html") {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val vulnerableResults = report.results.filter { it.vulnerable }
        val cleanResults = report.results.filter { !it.vulnerable }

        val html = buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html lang=\"en\">")
            appendLine("<head>")
            appendLine("<meta charset=\"UTF-8\">")
            appendLine("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
            appendLine("<title>SQL Injection Scan Report</title>")
            appendLine("<style>")
            appendLine("""
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; padding: 20px; background: #0f172a; color: #e2e8f0; }
                .container { max-width: 1200px; margin: 0 auto; }
                h1 { color: #f1f5f9; border-bottom: 2px solid #3b82f6; padding-bottom: 10px; }
                h2 { color: #93c5fd; }
                .summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin: 20px 0; }
                .stat-card { background: #1e293b; border-radius: 12px; padding: 20px; text-align: center; border: 1px solid #334155; }
                .stat-card .value { font-size: 2.5em; font-weight: bold; }
                .stat-card .label { color: #94a3b8; margin-top: 4px; }
                .vulnerable .value { color: #ef4444; }
                .clean .value { color: #22c55e; }
                .inputs .value { color: #3b82f6; }
                .tests .value { color: #a78bfa; }
                table { width: 100%; border-collapse: collapse; margin: 20px 0; }
                th { background: #1e293b; color: #93c5fd; padding: 12px; text-align: left; border-bottom: 2px solid #3b82f6; }
                td { padding: 10px 12px; border-bottom: 1px solid #334155; }
                tr:hover { background: #1e293b; }
                .badge { display: inline-block; padding: 4px 10px; border-radius: 9999px; font-size: 0.8em; font-weight: 600; }
                .badge-vuln { background: #7f1d1d; color: #fca5a5; }
                .badge-safe { background: #14532d; color: #86efac; }
                .payload { font-family: 'Fira Code', monospace; background: #334155; padding: 2px 6px; border-radius: 4px; font-size: 0.9em; word-break: break-all; }
                .evidence { font-size: 0.9em; color: #cbd5e1; }
                .section { margin-top: 30px; }
                .collapsible { cursor: pointer; background: #1e293b; border: 1px solid #334155; border-radius: 8px; padding: 12px 16px; margin: 8px 0; }
                .collapsible:hover { background: #334155; }
            """.trimIndent())
            appendLine("</style>")
            appendLine("</head>")
            appendLine("<body>")
            appendLine("<div class=\"container\">")
            appendLine("<h1>SQL Injection Scan Report</h1>")
            appendLine("<p>Target: <code>${escapeHtml(report.targetUrl)}</code> | Scanned: $timestamp</p>")
            //summary cards
            appendLine("<div class=\"summary\">")
            appendLine("""<div class="stat-card inputs"><div class="value">${report.totalInputs}</div><div class="label">Inputs Found</div></div>""")
            appendLine("""<div class="stat-card tests"><div class="value">${report.totalTests}</div><div class="label">Tests Run</div></div>""")
            appendLine("""<div class="stat-card vulnerable"><div class="value">${report.vulnerabilitiesFound}</div><div class="label">Vulnerabilities</div></div>""")
            appendLine("""<div class="stat-card clean"><div class="value">${cleanResults.size}</div><div class="label">Clean Tests</div></div>""")
            appendLine("</div>")

            //discovered inputs
            appendLine("<div class=\"section\">")
            appendLine("<h2>Discovered Inputs</h2>")
            appendLine("<table><tr><th>#</th><th>Type</th><th>Name</th><th>Location</th><th>Status</th></tr>")
            report.inputsFound.forEachIndexed { i, inp ->
                val vulnCount = report.results.count { it.inputSource == inp && it.vulnerable }
                val badge = if (vulnCount > 0) "<span class=\"badge badge-vuln\">VULNERABLE ($vulnCount)</span>" else "<span class=\"badge badge-safe\">CLEAN</span>"
                appendLine("<tr><td>${i + 1}</td><td>${escapeHtml(inp.inputType.toString())}</td><td><code>${inp.name}</code></td><td>${inp.location}</td><td>$badge</td></tr>")
            }
            appendLine("</table></div>")

            //vulnerable results
            if (vulnerableResults.isNotEmpty()) {
                appendLine("<div class=\"section\">")
                appendLine("<h2>Vulnerabilities Found</h2>")
                appendLine("<table><tr><th>Input</th><th>Type</th><th>Payload</th><th>Evidence</th><th>Status</th></tr>")
                for (r in vulnerableResults) {
                    appendLine("<tr>")
                    appendLine("<td><code>${escapeHtml(r.inputSource.name)}</code></td>")
                    appendLine("<td>${escapeHtml(r.inputSource.inputType.toString())}</td>")
                    appendLine("<td><span class=\"payload\">${escapeHtml(r.payload)}</span></td>")
                    appendLine("<td class=\"evidence\">${escapeHtml(r.evidence)}</td>")
                    appendLine("<td>${r.statusCode ?: "N/A"}</td>")
                    appendLine("</tr>")
                }
                appendLine("</table></div>")
            }

            appendLine("</div></body></html>")
        }

        File(outputPath).writeText(html)
        println("HTML report saved to: $outputPath")
    }

    fun generateTextReport(report: ScanReport, outputPath: String = "sqli-report.txt") {
        val text = buildString {
            appendLine("=" .repeat(80))
            appendLine("SQL INJECTION SCAN REPORT")
            appendLine("=" .repeat(80))
            appendLine("Target URL: ${report.targetUrl}")
            appendLine("Inputs discovered: ${report.totalInputs}")
            appendLine("Total tests run: ${report.totalTests}")
            appendLine("Vulnerabilities found: ${report.vulnerabilitiesFound}")
            appendLine("-".repeat(80))

            appendLine("\nDISCOVERED INPUTS:")
            report.inputsFound.forEachIndexed { i, inp ->
                val vulnCount = report.results.count { it.inputSource == inp && it.vulnerable }
                val status = if (vulnCount > 0) "VULNERABLE ($vulnCount payloads)" else "CLEAN"
                appendLine("  [${i + 1}] [$status] ${inp.inputType}: ${inp.name}")
                appendLine("      Location: ${inp.location}")
            }

            val vulnResults = report.results.filter { it.vulnerable }
            if (vulnResults.isNotEmpty()) {
                appendLine("\nVULNERABILITIES:")
                vulnResults.forEachIndexed { i, r ->
                    appendLine("  ${i + 1}. Input: ${r.inputSource.name} (${r.inputSource.inputType})")
                    appendLine("     Payload: ${r.payload}")
                    appendLine("     Evidence: ${r.evidence}")
                    appendLine("     Status: ${r.statusCode ?: "N/A"}")
                    appendLine()
                }
            } else {
                appendLine("\nNo SQL injection vulnerabilities detected.")
            }

            appendLine("=" .repeat(80))
        }

        File(outputPath).writeText(text)
        println("Text report saved to: $outputPath")
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
