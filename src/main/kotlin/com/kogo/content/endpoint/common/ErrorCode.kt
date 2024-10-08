package com.kogo.content.endpoint.common

import org.springframework.http.HttpStatus

enum class ErrorCode(val httpStatus: HttpStatus, val message: String) {
    // 40X
    BAD_REQUEST(HttpStatus.BAD_REQUEST, ""),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, ""),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, ""),
    NOT_FOUND(HttpStatus.NOT_FOUND, ""),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ""),

    // 50X
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "")
}
