plugins {
    id("kompute.kotlin-conventions")
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(project(":kompute-serialization"))
    ksp(project(":kompute-serialization:processor"))
}
