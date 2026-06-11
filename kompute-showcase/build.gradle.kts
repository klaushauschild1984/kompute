plugins {
    id("kompute.kotlin-conventions")
    id("kompute.lwjgl-conventions")
}

dependencies {
    implementation(project(":kompute-core"))
    runtimeOnly(libs.logback)
    runtimeOnly(project(":kompute-opengl"))

    testImplementation(testFixtures(project(":kompute-opengl")))
}
