plugins {
    id("kompute.kotlin-conventions")
    id("kompute.lwjgl-conventions")
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(project(":kompute-core"))
    implementation(project(":kompute-serialization"))
    ksp(project(":kompute-serialization:processor"))
    runtimeOnly(libs.logback)
    runtimeOnly(project(":kompute-opengl"))

    testImplementation(testFixtures(project(":kompute-opengl")))
}
