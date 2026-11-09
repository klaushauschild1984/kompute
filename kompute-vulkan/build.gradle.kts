plugins {
    id("kompute.kotlin-conventions")
    id("kompute.vulkan-conventions")
    `java-test-fixtures`
}

dependencies {
    api(project(":kompute-core"))
    implementation(libs.lwjgl)
    implementation(libs.lwjgl.vulkan)
    implementation(libs.lwjgl.shaderc)
    implementation(libs.lwjgl.spvc)
    implementation(libs.lwjgl.vma)

    testFixturesImplementation(platform(libs.junit.bom))
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api")
}

tasks.test {
    // -PvulkanValidation=true runs the tests with the Khronos validation layer and fails them on its errors
    providers.gradleProperty("vulkanValidation").orNull?.let {
        systemProperty("kompute.backend.vulkan.validation", it)
    }
}
