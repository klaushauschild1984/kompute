plugins {
    base
    jacoco
    id("com.saveourtool.diktat")
    alias(libs.plugins.ben.manes.versions)
}

allprojects {
    version = "0.10.2"
}

repositories {
    mavenCentral()
}

tasks.named<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask>("dependencyUpdates") {
    revision = "release"
    rejectVersionIf {
        candidate.version.contains(Regex("(?i)[-.](alpha|beta|rc|m|eap|dev)[0-9.]*$"))
    }
    outputFormatter = "json,html"
    outputDir = layout.buildDirectory.dir("dependencyUpdates").get().asFile.path
    reportfileName = "report"
}
