plugins {
    kotlin("jvm") version "2.1.10"
}

group = "com.sqli.tester"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.seleniumhq.selenium:selenium-java:4.18.1")
    implementation("io.github.bonigarcia:webdrivermanager:5.7.0")
    
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
    systemProperty("target.url", System.getProperty("target.url") ?: "")
    systemProperty("chrome.binary", System.getProperty("chrome.binary") ?: "")
    systemProperty("chromedriver.path", System.getProperty("chromedriver.path") ?: "")
    systemProperty("login.url", System.getProperty("login.url") ?: "")
    systemProperty("login.username", System.getProperty("login.username") ?: "")
    systemProperty("login.password", System.getProperty("login.password") ?: "")
    systemProperty("login.username.field", System.getProperty("login.username.field") ?: "")
    systemProperty("login.password.field", System.getProperty("login.password.field") ?: "")
    systemProperty("login.submit.selector", System.getProperty("login.submit.selector") ?: "")
    systemProperty("login.trigger.text", System.getProperty("login.trigger.text") ?: "")
    systemProperty("login.trigger.selector", System.getProperty("login.trigger.selector") ?: "")
    systemProperty("login.modal.selector", System.getProperty("login.modal.selector") ?: "")
    systemProperty("login.success.url.contains", System.getProperty("login.success.url.contains") ?: "")
    systemProperty("test.mode", System.getProperty("test.mode") ?: "")
    systemProperty("localhost.port", System.getProperty("localhost.port") ?: "")
    systemProperty("production.host", System.getProperty("production.host") ?: "")
    systemProperty("target.paths", System.getProperty("target.paths") ?: "")
    systemProperty("env.file", System.getProperty("env.file") ?: "")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}
