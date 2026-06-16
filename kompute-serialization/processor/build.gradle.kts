plugins {
    id("kompute.kotlin-conventions")
}

dependencies {
    implementation(libs.ksp.api)
    implementation(project(":kompute-serialization"))
}
