# Kompute: GPU Compute Shaders for Kotlin

Kompute is a Kotlin library designed to simplify the integration of GPU compute shaders into Kotlin applications. It
provides a high-level API for managing GPU resources, executing compute operations, and handling data transfers between
the CPU and GPU. With Kompute, developers can leverage the power of GPU acceleration for computationally intensive
tasks, such as machine learning inference, physics simulations, and data processing.

## CI Status

|                                                                               Build                                                                               |                  Core Coverage                   |                   OpenGL Coverage                    |                     Coroutines Coverage                      |                                     Last Commit                                      |                                Open Issues                                 |                                    Repo Size                                     |
|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------:|:------------------------------------------------:|:----------------------------------------------------:|:------------------------------------------------------------:|:------------------------------------------------------------------------------------:|:--------------------------------------------------------------------------:|:--------------------------------------------------------------------------------:|
| [![CI](https://github.com/klaushauschild1984/kompute/actions/workflows/ci.yml/badge.svg)](https://github.com/klaushauschild1984/kompute/actions/workflows/ci.yml) | ![Coverage Core](.github/badges/jacoco-core.svg) | ![Coverage OpenGL](.github/badges/jacoco-opengl.svg) | ![Coverage Coroutines](.github/badges/jacoco-coroutines.svg) | ![Last Commit](https://img.shields.io/github/last-commit/klaushauschild1984/kompute) | ![Issues](https://img.shields.io/github/issues/klaushauschild1984/kompute) | ![Repo Size](https://img.shields.io/github/repo-size/klaushauschild1984/kompute) |

## Requirements

| Requirement | Version                                                                                  |
|-------------|------------------------------------------------------------------------------------------|
| JDK         | ![JDK](https://img.shields.io/badge/JDK-21+-orange)                                      |
| Kotlin      | ![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-purple?logo=kotlin&logoColor=white) |
| OpenGL      | ![OpenGL](https://img.shields.io/badge/OpenGL-4.3+-blue)                                 |
| OS          | ![OS](https://img.shields.io/badge/OS-Linux%20%7C%20Windows-lightgrey)                   |

## Getting Started

| JitPack                                                                                                           | License                                                                         |
|-------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------|
| [![Release](https://jitpack.io/v/klaushauschild1984/kompute.svg)](https://jitpack.io/#klaushauschild1984/kompute) | [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE) |

Add the JitPack repository and the `kompute-core` dependency to your build.

### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.klaushauschild1984.kompute:kompute-core:v0.9.0")
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.klaushauschild1984.kompute</groupId>
        <artifactId>kompute-core</artifactId>
        <version>v0.9.0</version>
    </dependency>
</dependencies>
```

To run compute shaders a backend module and matching LWJGL natives are required — see [Wiki: Backends](https://github.com/klaushauschild1984/kompute/wiki/Backends).

### Optional: Coroutines support

For asynchronous dispatch via Kotlin Coroutines, add `kompute-coroutines`:

```kotlin
dependencies {
    implementation("com.github.klaushauschild1984.kompute:kompute-coroutines:v0.8.0")
}
```

```xml
<dependency>
    <groupId>com.github.klaushauschild1984.kompute</groupId>
    <artifactId>kompute-coroutines</artifactId>
    <version>v0.9.0</version>
</dependency>
```

## Usage

Select a backend, attach a compute shader, configure storage buffers, dispatch, and read results.

```kotlin
Kompute.openGL().use { openGL ->
    val output = StorageBuffer<FloatArray>(1).size(128).asOutput()
    val result = openGL
        .shader(ShaderSource.Code(glslCode))
        .compile()
        .use { shader ->
            shader.dispatch(64, StorageBuffer<FloatArray>(0).data(input), output)
                .use { it[output] }
        }
    println(result.contentToString())
}
```

### Async dispatch

With `kompute-coroutines`, dispatch suspends the caller instead of blocking the thread:

```kotlin
Kompute.openGL().use { openGL ->
    val output = StorageBuffer<FloatArray>(1).size(128).asOutput()
    val result = openGL
        .shader(ShaderSource.Code(glslCode))
        .compile()
        .async()
        .use { shader ->
            shader.dispatch(64, StorageBuffer<FloatArray>(0).data(input), output)
                .use { it[output] }
        }
    println(result.contentToString())
}
```

Full API documentation is available in the [Wiki](https://github.com/klaushauschild1984/kompute/wiki).

## Contributing

Contributions are welcome. If you face a bug or see the need for an enhancement, feel free to open an issue. For changes, please open an issue first to discuss what you would like to change.
