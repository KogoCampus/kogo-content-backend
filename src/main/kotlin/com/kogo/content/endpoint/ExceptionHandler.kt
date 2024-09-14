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
    fun handleIllegalArgumentException(ex: IllegalArgumentException) =
        HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, details = ex.message)

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFoundException(ex: ResourceNotFoundException) =
        HttpJsonResponse.errorResponse(ErrorCode.NOT_FOUND, details = ex.message)

    @ExceptionHandler(UnsupportedMediaTypeException::class)
    fun handleUnsupportedMediaTypeException(ex: UnsupportedMediaTypeException) =
        HttpJsonResponse.errorResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE, details = ex.message)

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any> = run {
        val errorCode = ErrorCode.BAD_REQUEST
        ResponseEntity(
            ErrorResponse(
                status = errorCode.httpStatus,
                message = errorCode.message,
                details = ex.message
            ), errorCode.httpStatus)
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(ex: AuthenticationException) =
        HttpJsonResponse.errorResponse(ErrorCode.UNAUTHORIZED, details = ex.message)

    @ExceptionHandler(Exception::class)
    fun handleUnhandledException(ex: Exception): ResponseEntity<HttpJsonResponse.ErrorResponse> {
        log.error { "Unhandled exception occurred; ${ex.message}" }
        log.error { ex }
        return HttpJsonResponse.errorResponse(ErrorCode.INTERNAL_SERVER_ERROR)
    }
}
