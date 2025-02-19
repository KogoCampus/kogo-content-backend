package com.kogo.content.endpoint.validator

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.beans.factory.annotation.Value

class FileTokenValidator : ConstraintValidator<ValidFileToken, String> {

    @Value("\${security.secret-key}")
    private lateinit var secretKey: String

    private var acceptedMediaTypes: Array<String> = arrayOf()

    override fun initialize(constraintAnnotation: ValidFileToken) {
        acceptedMediaTypes = constraintAnnotation.acceptedMediaTypes
    }

    override fun isValid(token: String?, context: ConstraintValidatorContext?): Boolean {
        if (token.isNullOrBlank()) return true // Skip null or empty values

        return try {
            val algorithm = Algorithm.HMAC256(secretKey)
            val verifier = JWT.require(algorithm)
                .withSubject("file_token")
                .build()

            val decodedJWT = verifier.verify(token)
            val contentType = decodedJWT.getClaim("contentType").asString()
            print(contentType)

            // Ensure the token's content type is in the allowed list
            if (contentType !in acceptedMediaTypes) {
                context?.buildConstraintViolationWithTemplate("Unsupported media type: $contentType")
                    ?.addConstraintViolation()
                return false
            }
            true
        } catch (e: Exception) {
            context?.buildConstraintViolationWithTemplate("Invalid or expired file token")
                ?.addConstraintViolation()
            false
        }
    }
}

class FileTokenListValidator : ConstraintValidator<ValidFileToken, List<String>?> {

    @Value("\${security.secret-key}")
    private lateinit var secretKey: String

    private var acceptedMediaTypes: Array<String> = arrayOf()
    private val singleValidator = FileTokenValidator()

    override fun initialize(constraintAnnotation: ValidFileToken) {
        acceptedMediaTypes = constraintAnnotation.acceptedMediaTypes
        singleValidator.initialize(constraintAnnotation)
    }

    override fun isValid(tokens: List<String>?, context: ConstraintValidatorContext?): Boolean {
        if (tokens.isNullOrEmpty()) return true // Skip validation if empty

        return tokens.all { token ->
            try {
                val algorithm = Algorithm.HMAC256(secretKey)
                val verifier = JWT.require(algorithm).withSubject("file_token").build()
                val decodedJWT = verifier.verify(token)
                val contentType = decodedJWT.getClaim("contentType").asString()

                // Ensure contentType is allowed
                if (contentType !in acceptedMediaTypes) {
                    context?.disableDefaultConstraintViolation()
                    context?.buildConstraintViolationWithTemplate("Unsupported media type: $contentType")
                        ?.addConstraintViolation()
                    return false
                }
                true
            } catch (e: JWTVerificationException) {
                context?.disableDefaultConstraintViolation()
                context?.buildConstraintViolationWithTemplate("Invalid or expired file token")
                    ?.addConstraintViolation()
                false
            }
        }
    }
}
