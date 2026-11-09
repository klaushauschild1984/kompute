plugins {
    id("kompute.kotlin-conventions")
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(project(":kompute-serialization"))
    implementation(project(":kompute-common-types"))
    kspTest(project(":kompute-serialization:processor"))
}
