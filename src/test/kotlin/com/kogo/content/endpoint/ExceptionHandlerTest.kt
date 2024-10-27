package com.kogo.content.endpoint

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.exception.*
import org.springframework.http.HttpStatus
import com.kogo.content.exception.ResourceNotFoundException
import com.kogo.content.storage.entity.Topic
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import org.springframework.security.core.AuthenticationException
import kotlin.test.assertEquals

class ExceptionHandlerTest {
    private lateinit var exceptionHandler: GlobalExceptionHandler

    @BeforeEach
    fun setup() {
        exceptionHandler = GlobalExceptionHandler()
    }

    @Test
    fun `handleResourceNotFoundException should return NOT_FOUND`() {
        val ex = ResourceNotFoundException.of<Topic>("123")

        val response: ResponseEntity<HttpJsonResponse.ErrorResponse> = exceptionHandler.handleResourceNotFoundException(ex)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals(ErrorCode.NOT_FOUND.name, response.body?.error)
        assertEquals("Topic not found for id: 123", response.body?.details)
    }

    @Test
    fun `handleUnsupportedMediaTypeException should return UNSUPPORTED_MEDIA_TYPE`() {
        // Use the 'of' method to create the exception
        val ex = UnsupportedMediaTypeException.of("text/plain", "application/json")

        val response: ResponseEntity<HttpJsonResponse.ErrorResponse> = exceptionHandler.handleUnsupportedMediaTypeException(ex)

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.statusCode)
        assertEquals(ErrorCode.UNSUPPORTED_MEDIA_TYPE.name, response.body?.error)
        assertEquals("text/plain is not supported. Please upload application/json.", response.body?.details)
    }

    @Test
    fun `handleAuthenticationException should return UNAUTHORIZED`() {
        val ex = mockk<AuthenticationException>()

        val response: ResponseEntity<HttpJsonResponse.ErrorResponse> = exceptionHandler.handleAuthenticationException(ex)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals(ErrorCode.UNAUTHORIZED.name, response.body?.error)
        assertEquals("Your access token is invalid or missing.", response.body?.details)
    }

    @Test
    fun `handleUnhandledException should return INTERNAL_SERVER_ERROR`() {
        val ex = Exception("Unknown error")

        val response: ResponseEntity<HttpJsonResponse.ErrorResponse> = exceptionHandler.handleUnhandledException(ex)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.name, response.body?.error)
        assertEquals("Unhandled exception occurred; ${ex.message}", response.body?.details)
    }
}
