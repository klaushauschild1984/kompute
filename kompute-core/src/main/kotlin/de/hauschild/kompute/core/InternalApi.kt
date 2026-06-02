package de.hauschild.kompute.core

@RequiresOptIn(
    message = "This is internal Kompute API and should not be used from outside the library.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
annotation class InternalApi
