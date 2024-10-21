package com.kogo.content.endpoint.common

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.util.MultiValueMap

open class HttpJsonResponse {

    data class SuccessResponse (
        val data: Any?
    ) : HttpJsonResponse()

    data class ErrorResponse(
        val error: ErrorDetails
    ) : HttpJsonResponse()

    data class ErrorDetails(
        val reason: String,
        val details: String?
    )

    companion object {
        fun successResponse(data: Any?, message: String? = "", httpStatus: HttpStatus = HttpStatus.OK, headers: HttpHeaders = HttpHeaders())
            = ResponseEntity.status(httpStatus)
                .headers(headers)
                .body(SuccessResponse(
                    data = data
                ))

        fun errorResponse(errorCode: ErrorCode, details: String = "", headers: HttpHeaders = HttpHeaders())
            = ResponseEntity.status(errorCode.httpStatus)
            .headers(headers)
            .body(ErrorResponse(
                error = ErrorDetails(
                    reason = errorCode.name,
                    details = details
                )
            ))
    }
}
