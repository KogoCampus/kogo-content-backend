package com.kogo.content.endpoint.common

import org.springframework.http.HttpStatus

enum class ErrorCode(val httpStatus: HttpStatus, val message: String) {
    // 400
    BAD_REQUEST(HttpStatus.BAD_REQUEST, ""),
    ENTITY_NOT_FOUND(HttpStatus.BAD_REQUEST, "Entity not found."),

    // 401
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, ""),

    // 403
    ACCESS_DENIED(HttpStatus.FORBIDDEN, ""),
}