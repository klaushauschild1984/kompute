plugins {
    id("kompute.kotlin-conventions")
    id("kompute.opengl-conventions")
    alias(libs.plugins.jmh)
}

dependencies {
    implementation(project(":kompute-opengl"))
    implementation(libs.oshi)
}
