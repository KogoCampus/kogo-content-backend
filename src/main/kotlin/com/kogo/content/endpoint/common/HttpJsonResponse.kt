package com.kogo.content.endpoint.common

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.util.MultiValueMap

open class HttpJsonResponse {

    data class SuccessResponse (
        val data: Any?,
        val message: String?
    ) : HttpJsonResponse()

    data class ErrorResponse(
        val error: String,
        val details: String?
    ) : HttpJsonResponse()

    companion object {
        fun successResponse(data: Any?, message: String? = null, httpStatus: HttpStatus = HttpStatus.OK, headers: HttpHeaders = HttpHeaders())
            = ResponseEntity.status(httpStatus)
                .headers(headers)
                .body(SuccessResponse(
                    data = data,
                    message = message
                ))

        fun errorResponse(errorCode: ErrorCode, details: String = "", headers: HttpHeaders = HttpHeaders())
            = ResponseEntity.status(errorCode.httpStatus)
            .headers(headers)
            .body(ErrorResponse(
                error = errorCode.name,
                details = details
            ))
    }
}
