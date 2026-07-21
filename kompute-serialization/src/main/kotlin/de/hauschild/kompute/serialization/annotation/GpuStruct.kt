package de.hauschild.kompute.serialization.annotation

/**
 * Marks a class as GPU struct compatible. All fields annotated with [GpuField] will be serialized and deserialized.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class GpuStruct
