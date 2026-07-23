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
    implementation(libs.lwjgl.vma)

    testFixturesImplementation(platform(libs.junit.bom))
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api")
}
