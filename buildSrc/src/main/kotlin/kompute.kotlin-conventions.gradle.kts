plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
    id("jacoco")
}

group = "de.hauschild.kompute"

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testRuntimeOnly("ch.qos.logback:logback-classic:1.5.18")
}

sourceSets {
    test {
        resources {
            srcDir(rootProject.file("src/test/resources"))
        }
    }
}

val generateBuildInfo by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/buildInfo")
    val projectVersion = project.version
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("de/hauschild/kompute/core/BuildInfo.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package de.hauschild.kompute.core

            internal object BuildInfo {
                const val VERSION: String = "$projectVersion"
            }
            """.trimIndent(),
        )
    }
}

sourceSets.main {
    kotlin.srcDir(tasks.named("generateBuildInfo"))
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
        html.required.set(true)
    }
}

ktlint {
    version.set("1.4.1")
}
