package com.kogo.content.endpoint.common

import org.springframework.http.HttpStatus

open class Response {
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
    ) : Response()

    data class Error(
        val status: HttpStatus,
        val message: String,
        val details: String?
    ) : Response()
}
