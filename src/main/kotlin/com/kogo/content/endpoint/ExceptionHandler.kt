package com.kogo.content.endpoint

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.endpoint.common.HttpJsonResponse.ErrorResponse
import com.kogo.content.exception.*
import com.kogo.content.logging.Logger
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler: ResponseEntityExceptionHandler() {

    companion object : Logger()

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(ex: AuthenticationException): ResponseEntity<ErrorResponse> {
        log.error { "Authentication Failed: ${ex.message}" }
        return HttpJsonResponse.errorResponse(ErrorCode.UNAUTHORIZED, details = "Your access token is invalid or missing.")
    }

    // i.e. throw ResourceNotFound("Comment", commendId)
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFoundException(ex: ResourceNotFoundException): ResponseEntity<ErrorResponse> {
        val message = "${ex.resourceName} not found for id: ${ex.resourceId}"
        log.error { message }
        return HttpJsonResponse.errorResponse(ErrorCode.NOT_FOUND, details = message)
    }

    @ExceptionHandler(UnsupportedMediaTypeException::class)
    fun handleUnsupportedMediaTypeException(ex: UnsupportedMediaTypeException): ResponseEntity<ErrorResponse> {
        val message = "${ex.mediaType} is not supported. Please upload ${ex.acceptedMediaType}."
        log.error { message }
        return HttpJsonResponse.errorResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE, details = message)
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any> {
        val errorCode = ErrorCode.BAD_REQUEST
        val bindingResult = ex.bindingResult
        val fieldErrors = bindingResult.fieldErrors

        val message = "Request validation failed: ${ex.body.detail} in ${ex.objectName} : $bindingResult : $fieldErrors"
        log.error { message }

        return ResponseEntity(
            ErrorResponse(
                error = errorCode.name,
                details = message
            ), errorCode.httpStatus)
    }

    override fun handleNoHandlerFoundException(
        ex: NoHandlerFoundException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val errorCode = ErrorCode.NOT_FOUND
        val message = "Unhandled request: ${ex.message}"
        log.error (ex) { message }
        return ResponseEntity(
            ErrorResponse(
                error = errorCode.name,
                details = message
            ), errorCode.httpStatus)
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleUnhandledRuntimeException(ex: RuntimeException): ResponseEntity<ErrorResponse> {
        val message = "Unhandled exception occurred: ${ex.message}"
        log.error (ex) { message }
        return HttpJsonResponse.errorResponse(ErrorCode.INTERNAL_SERVER_ERROR)
    }
}
