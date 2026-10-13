package com.miranova.mfa.crypto

/**
 * Exceptions thrown during cryptographic operations in Android Keystore.
 */
sealed class CryptoException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class KeyNotFoundException(val alias: String) : 
        CryptoException("Cryptographic key with alias '$alias' was not found in Android Keystore.")
        
    class KeyPermanentlyInvalidatedException(message: String = "Key permanently invalidated due to lock screen security changes.", cause: Throwable? = null) : 
        CryptoException(message, cause)
        
    class UserNotAuthenticatedException(message: String = "User authentication required to access this key.") : 
        CryptoException(message)
        
    class KeyGenerationException(message: String, cause: Throwable? = null) : 
        CryptoException(message, cause)
        
    class SignatureException(message: String, cause: Throwable? = null) : 
        CryptoException(message, cause)
}
