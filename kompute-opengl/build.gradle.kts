plugins {
    id("kompute.kotlin-conventions")
}

dependencies {
    api(project(":kompute-core"))
    implementation(platform(libs.lwjgl.bom))
    implementation(libs.lwjgl)
    implementation(libs.lwjgl.glfw)
    implementation(libs.lwjgl.opengl)
    testRuntimeOnly("org.lwjgl:lwjgl::natives-linux")
    testRuntimeOnly("org.lwjgl:lwjgl-glfw::natives-linux")
    testRuntimeOnly("org.lwjgl:lwjgl-opengl::natives-linux")
}
