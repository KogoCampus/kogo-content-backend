package com.kogo.content.endpoint.common

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

open class HttpJsonResponse {

    data class SuccessResponse (
        val status: HttpStatus,
        val message: String?,
        val data: Any?
    ) : HttpJsonResponse()

    data class ErrorResponse (
        val status: HttpStatus,
        val message: String,
        val details: String?
    ) : HttpJsonResponse()

    companion object {
        fun successResponse(data: Any?, message: String? = "", httpStatus: HttpStatus = HttpStatus.OK) = ResponseEntity(SuccessResponse(
            status = httpStatus,
            message = message,
            data = data
        ), httpStatus)

        fun errorResponse(errorCode: ErrorCode, details: String? = null) = ResponseEntity(ErrorResponse(
            status = errorCode.httpStatus,
            message = errorCode.message,
            details = details
        ), errorCode.httpStatus)
    }
}
