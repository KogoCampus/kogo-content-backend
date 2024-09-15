package com.kogo.content.endpoint

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.endpoint.common.HttpJsonResponse.ErrorResponse
import com.kogo.content.exception.ResourceNotFoundException
import com.kogo.content.exception.UnsupportedMediaTypeException
import com.kogo.content.logging.Logger
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler: ResponseEntityExceptionHandler() {

    companion object : Logger()

    // set the value to which exception we want to catch
    // i.e. IllegalArgumentException(message)
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        log.error { ex }
        return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, details = "")
    }

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFoundException(ex: ResourceNotFoundException): ResponseEntity<ErrorResponse> {
        log.error { ex }
        return HttpJsonResponse.errorResponse(ErrorCode.NOT_FOUND, details = "")
    }

    @ExceptionHandler(UnsupportedMediaTypeException::class)
    fun handleUnsupportedMediaTypeException(ex: UnsupportedMediaTypeException): ResponseEntity<ErrorResponse> {
        log.error { ex }
        return HttpJsonResponse.errorResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE, details = "")
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any> {
        val errorCode = ErrorCode.BAD_REQUEST
        log.error { ex }
        return ResponseEntity(
            ErrorResponse(
                status = errorCode.httpStatus,
                message = errorCode.message,
                details = ""
            ), errorCode.httpStatus)
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(ex: AuthenticationException): ResponseEntity<ErrorResponse> {
        log.error { ex }
        return HttpJsonResponse.errorResponse(ErrorCode.UNAUTHORIZED, details = "")
    }

    @ExceptionHandler(Exception::class)
    fun handleUnhandledException(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error { "Unhandled exception occurred; ${ex.message}" }
        log.error { ex.stackTraceToString() }
        return HttpJsonResponse.errorResponse(ErrorCode.INTERNAL_SERVER_ERROR, details = "")
    }
}
