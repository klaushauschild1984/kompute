package de.hauschild.kompute.serialization.annotation

/**
 * Marks a class as GPU struct compatible. All fields annotated with [GpuField] will be serialized and deserialized.
 *
 * If there are multiple [GpuStruct]s present in a class hierachy, the outest one will specify the layout.
 *
 * @property layout specifies the desired memory layout within the GPU - defaults to [Layout.STD140]
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class GpuStruct(val layout: Layout = Layout.STD140)
