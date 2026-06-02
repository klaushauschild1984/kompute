# Kompute: GPU Compute Shaders for Kotlin

Kompute is a Kotlin library designed to simplify the integration of GPU compute shaders into Kotlin applications. It
provides a high-level API for managing GPU resources, executing compute operations, and handling data transfers between
the CPU and GPU. With Kompute, developers can leverage the power of GPU acceleration for computationally intensive
tasks, such as machine learning inference, physics simulations, and data processing.

## Usage

Buffer names in Kotlin must match the binding names declared in the GLSL shader source.

```kotlin
Kompute.openGL().use { backend ->
    val result = backend.shader(ShaderSource.File(Path.of("shaders/multiply.glsl")))
        .input("data").buffer(floatArrayOf(1f, 2f, 3f, 4f))
        .input("factor").buffer(floatArrayOf(2f))
        .output("result").buffer(FloatArray(4))
        .dispatch(4)
        .execute()
        .output("result")
}
```