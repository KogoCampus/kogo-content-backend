package com.kogo.content.endpoint.common

import org.springframework.http.HttpStatus

enum class ErrorCode(val httpStatus: HttpStatus) {
    // 40X
    BAD_REQUEST(HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    USER_ACTION_DENIED(HttpStatus.FORBIDDEN),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    DUPLICATED(HttpStatus.CONFLICT),

    // 50X
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR)
}
