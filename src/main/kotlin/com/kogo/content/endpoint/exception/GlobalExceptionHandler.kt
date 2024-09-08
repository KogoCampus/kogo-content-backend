package com.kogo.content.endpoint.exception

import com.kogo.content.service.exception.DBAccessException
import com.kogo.content.service.exception.MaxFileSizeExceededException
import com.kogo.content.service.exception.UnsupportedMediaTypeException
import com.kogo.content.storage.exception.DocumentNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

class ErrorModel(
    var status: HttpStatus? = null,
    var message: String? = null
)

@ControllerAdvice
class GlobalExceptionHandler: ResponseEntityExceptionHandler() {
    // set the value to which exception we want to catch
    // i.e. IllegalArgumentException(message)
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ErrorModel> {
        val errorMessage = ErrorModel(
            HttpStatus.BAD_REQUEST,
            ex.message
        )
        return ResponseEntity(errorMessage, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(DocumentNotFoundException::class)
    fun handleDocumentNotFoundException(ex: DocumentNotFoundException): ResponseEntity<ErrorModel> {
        val errorMessage = ErrorModel(
            HttpStatus.NOT_FOUND,
            ex.message
        )
        return ResponseEntity(errorMessage, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(UnsupportedMediaTypeException::class)
    fun handleUnsupportedMediaTypeException(ex: UnsupportedMediaTypeException): ResponseEntity<ErrorModel> {
        val errorMessage = ErrorModel(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            ex.message
        )
        return ResponseEntity(errorMessage, HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    }

    @ExceptionHandler(MaxFileSizeExceededException::class)
    fun handleMaxFileSizeExceededException(ex: MaxFileSizeExceededException): ResponseEntity<ErrorModel> {
        val errorMessage = ErrorModel(
            HttpStatus.PAYLOAD_TOO_LARGE,
            ex.message
        )
        return ResponseEntity(errorMessage, HttpStatus.PAYLOAD_TOO_LARGE)
    }

    @ExceptionHandler(DBAccessException::class)
    fun handleDBAccessException(ex: DBAccessException): ResponseEntity<ErrorModel> {
        val errorMessage = ErrorModel(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ex.message
        )
        return ResponseEntity(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    // for unhandled exceptions
    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ErrorModel> {
        val errorMessage = ErrorModel(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ex.message
        )
        return ResponseEntity(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}