plugins {
    id("kompute.kotlin-conventions")
}

dependencies {
    api(libs.kotlin.logging)
    testImplementation(kotlin("reflect"))
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
