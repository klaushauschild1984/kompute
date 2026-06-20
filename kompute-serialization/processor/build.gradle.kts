plugins {
    id("kompute.kotlin-conventions")
}

dependencies {
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    implementation(project(":kompute-serialization"))
}
