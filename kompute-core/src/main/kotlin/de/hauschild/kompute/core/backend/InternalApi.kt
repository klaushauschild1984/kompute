package de.hauschild.kompute.core.backend

/**
 * Marks declarations as internal Kompute API not intended for public use.
 *
 * Annotated APIs are unstable and may change between versions without notice.
 * Code depending on internal API is not supported.
 */
@RequiresOptIn(
    message = "This is internal Kompute API and should not be used from outside the library.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
annotation class InternalApi
