plugins {
    id("kompute.kotlin-conventions")
    alias(libs.plugins.ksp)
}

dependencies {
    api(project(":kompute-serialization"))
    ksp(project(":kompute-serialization:processor"))
}
