plugins {
    `kotlin-dsl`
    alias(libs.plugins.ben.manes.versions)
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    val catalog = versionCatalogs.named("libs")
    fun version(alias: String) = catalog.findVersion(alias).get().requiredVersion
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${version("kotlin")}")
    implementation("com.saveourtool.diktat:diktat-gradle-plugin:${version("diktat")}")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:${version("detekt")}")
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
