plugins {
    id("kompute.kotlin-conventions")
    id("kompute.vulkan-conventions")
}

dependencies {
    api(project(":kompute-core"))
    implementation(libs.lwjgl.vulkan)
    implementation(libs.lwjgl.shaderc)
    implementation(libs.lwjgl.vma)
}
