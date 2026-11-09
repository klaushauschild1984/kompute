import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    kotlin("jvm")
    id("com.saveourtool.diktat")
    id("dev.detekt")
    id("jacoco")
}

val libs = the<LibrariesForLibs>()

group = "de.hauschild.kompute"

kotlin {
    jvmToolchain(25)
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation(libs.assertj)
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly(libs.logback)
}

sourceSets {
    test {
        resources {
            srcDir(rootProject.file("src/test/resources"))
        }
    }
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.jar {
    manifest {
        attributes["Implementation-Version"] = project.version
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
    }
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        html.required = true
        csv.required = true
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("detekt.yml"))
}

tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
    reports {
        html.required = true
        sarif.required = true
    }
}

// Run detekt with type resolution on every compilation (detektMain, detektTest, ...). That includes generated
// sources (e.g. KSP output), so they are held to the same standard as hand-written code. The plain `detekt` task
// runs without type resolution and is a subset of this, so it is disabled to avoid duplicate findings.
val detektWithTypeResolution = tasks.withType<dev.detekt.gradle.Detekt>().matching {
    it.name != "detekt" && !it.name.endsWith("SourceSet")
}

tasks.named("detekt") {
    enabled = false
}

// Merges the per-compilation SARIF reports into the single report that CI uploads to GitHub Code Scanning.
// (detekt's own ReportMergeTask fails with a NoSuchMethodError in 2.0.0-alpha.6.) All reports come from the same
// tool with the same rule set, so the results of all runs are simply appended to the first run.
val detektReportMerge = tasks.register("detektReportMerge") {
    val merged = layout.buildDirectory.file("reports/detekt/detekt.sarif")
    outputs.file(merged)
    outputs.upToDateWhen { false }
    doLast {
        @Suppress("UNCHECKED_CAST")
        val sarifs = detektWithTypeResolution
            .map { it.reports.sarif.outputLocation.get().asFile }
            .filter { it.exists() }
            .map { JsonSlurper().parse(it) as Map<String, Any> }
        if (sarifs.isNotEmpty()) {
            @Suppress("UNCHECKED_CAST")
            fun Map<String, Any>.firstRun() = (this["runs"] as List<Map<String, Any>>).first()
            val results = sarifs.flatMap { it.firstRun()["results"] as List<*> }
            val mergedSarif = sarifs.first() + ("runs" to listOf(sarifs.first().firstRun() + ("results" to results)))
            merged.get().asFile.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(mergedSarif)))
        }
    }
}

detektWithTypeResolution.configureEach {
    // also merge when detekt reports findings, otherwise the SARIF upload would miss exactly those
    finalizedBy(detektReportMerge)
}

tasks.check {
    dependsOn(detektWithTypeResolution)
}

diktat {
    reporters {
        sarif()
    }
    inputs {
        include("src/**/*.kt")
        exclude("**/generated/**")
    }
}

tasks.withType<com.saveourtool.diktat.plugin.gradle.tasks.DiktatCheckTask> {
    outputs.upToDateWhen { false }
}
tasks.withType<com.saveourtool.diktat.plugin.gradle.tasks.DiktatFixTask> {
    outputs.upToDateWhen { false }
}

tasks.check {
    dependsOn(tasks.named("diktatCheck"))
}

