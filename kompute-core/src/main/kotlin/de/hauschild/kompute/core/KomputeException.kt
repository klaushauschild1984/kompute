package de.hauschild.kompute.core

abstract class KomputeException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class KomputeConfigurationException(
    message: String,
) : KomputeException(message)

fun requireConfiguration(
    value: Boolean,
    lazyMessage: () -> String,
) {
    if (!value) throw KomputeConfigurationException(lazyMessage())
}

abstract class KomputeBackendException(
    message: String,
    cause: Throwable? = null,
) : KomputeException(message, cause)

class KomputeBackendInitializationException(
    message: String,
    cause: Throwable? = null,
) : KomputeBackendException(message, cause)

fun requireBackendInitialization(
    value: Boolean,
    lazyMessage: () -> String,
) {
    if (!value) throw KomputeBackendInitializationException(lazyMessage())
}

class KomputeBackendDispatchException(
    message: String,
    cause: Throwable? = null,
) : KomputeBackendException(message, cause)
