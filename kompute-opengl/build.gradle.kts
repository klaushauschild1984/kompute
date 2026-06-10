plugins {
    id("kompute.kotlin-conventions")
    id("kompute.lwjgl-conventions")
}

dependencies {
    api(project(":kompute-core"))
    implementation(libs.lwjgl)
    implementation(libs.lwjgl.glfw)
    implementation(libs.lwjgl.opengl)
}
