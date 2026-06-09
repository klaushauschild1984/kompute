package de.hauschild.kompute.core.exception

/**
 * Thrown when shader data or dispatch parameters are configured incorrectly.
 *
 * @param message
 */
class KomputeConfigurationException(
    message: String,
) : KomputeException(message)
