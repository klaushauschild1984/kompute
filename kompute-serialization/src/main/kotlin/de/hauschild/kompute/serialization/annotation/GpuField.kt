package de.hauschild.kompute.serialization.annotation

/**
 * Marks a field as part of [GpuStruct].
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class GpuField
