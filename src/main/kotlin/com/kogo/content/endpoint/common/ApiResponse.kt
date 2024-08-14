package com.kogo.content.endpoint.common

import org.springframework.http.HttpStatus

open class ApiResponse {

    companion object {
        fun success(data: Any?, message: String? = ""): Success {
            return Success(HttpStatus.OK, message, data)
        }

        fun error(errorCode: ErrorCode, details: String? = null): Error {
            return Error(errorCode.httpStatus, errorCode.message, details)
        }
    }

    data class Success(
        val status: HttpStatus,
        val message: String?,
        val data: Any?
    ) : ApiResponse()

    data class Error(
        val status: HttpStatus,
        val message: String,
        val details: String?
    ) : ApiResponse()
}