package de.hauschild.kompute.serialization.annotation

/**
 * @property value
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Align(val value: Int)
