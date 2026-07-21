package de.hauschild.kompute.serialization.annotation

/**
 * @property value
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Align(val value: Int)
