package com.kogo.content.endpoint.public.model

import com.kogo.content.endpoint.common.ApiStatus

data class ApiResponse(
    val status: ApiStatus,
    val message: String?,
    val data: Any?
) {
    companion object {
        fun success(data: Any?, message: String? = ""): ApiResponse {
            return ApiResponse(ApiStatus.SUCCESS, message, data)
        }

        fun error(message: String?): ApiResponse {
            return ApiResponse(ApiStatus.ERROR, message, null)
        }
    }
}