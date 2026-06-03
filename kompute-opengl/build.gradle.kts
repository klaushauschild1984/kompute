plugins {
    id("kompute.kotlin-conventions")
}

dependencies {
    api(project(":kompute-core"))
    implementation(libs.lwjgl)
    implementation(libs.lwjgl.glfw)
    implementation(libs.lwjgl.opengl)
    runtimeOnly("org.lwjgl:lwjgl:${libs.versions.lwjgl.get()}:natives-linux")
    runtimeOnly("org.lwjgl:lwjgl-glfw:${libs.versions.lwjgl.get()}:natives-linux")
    runtimeOnly("org.lwjgl:lwjgl-opengl:${libs.versions.lwjgl.get()}:natives-linux")
}
