plugins {
    id("kompute.kotlin-conventions")
}

dependencies {
    api(project(":kompute-core"))
    api(libs.kotlinx.coroutines)
    testImplementation(libs.kotlinx.coroutines.test)
}
