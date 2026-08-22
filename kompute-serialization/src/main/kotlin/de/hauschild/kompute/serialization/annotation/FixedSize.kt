package de.hauschild.kompute.serialization.annotation

/**
 * Declares a statically-sized array field with [value] elements.
 *
 * Unlike an unsized array — which may only be the last [GpuField] of a struct serialized at
 * the top level — a field with a declared fixed size has a compile-time-known total size and
 * may appear anywhere in a [GpuStruct], including nested inside another struct.
 *
 * @property value the fixed element count
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class FixedSize(val value: Int)
