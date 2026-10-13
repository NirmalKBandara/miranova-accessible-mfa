package com.miranova.mfa.ui.accessibility

/**
 * Discrete steps for guided accessible device enrollment complying with docs/05-accessibility-design.md §5.1.
 */
enum class EnrollmentStep(
    val stepIndex: Int,
    val totalSteps: Int = 3,
    val stepTitle: String,
    val stepInstructions: String
) {
    /** Step 1: User Account Identifier */
    IDENTITY(
        stepIndex = 1,
        stepTitle = "Account Identity",
        stepInstructions = "Enter your username or email address to bind with this device."
    ),

    /** Step 2: Biometric Protection Preference */
    SECURITY_POLICY(
        stepIndex = 2,
        stepTitle = "Biometric Protection Preference",
        stepInstructions = "Choose whether biometric authentication is required before each cryptographic signature."
    ),

    /** Step 3: Hardware Key Generation and Registration */
    CONFIRMATION(
        stepIndex = 3,
        stepTitle = "Generate Key and Register",
        stepInstructions = "Generate a non-exportable hardware key in Android Keystore and complete device enrollment."
    )
}
