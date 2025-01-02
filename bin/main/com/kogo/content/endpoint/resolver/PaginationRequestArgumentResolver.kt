package com.kogo.content.endpoint.resolver

import com.kogo.content.endpoint.common.PageToken
import com.kogo.content.endpoint.common.PaginationRequest
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

class PaginationRequestArgumentResolver : HandlerMethodArgumentResolver {
    companion object {
        private const val PAGE_TOKEN_PARAM = "page_token"
        private const val PAGE_SIZE_PARAM = "limit"
    }

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.parameterType == PaginationRequest::class.java
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Any {
        val requestParameters = webRequest.parameterMap.mapValues { it.value.firstOrNull() ?: "" }
            .filter { it.value.isNotEmpty() }

        val pageTokenStr = requestParameters[PAGE_TOKEN_PARAM]
        val limit = requestParameters[PAGE_SIZE_PARAM]?.toIntOrNull() ?: 10

        val pageToken = if (pageTokenStr != null) {
            PageToken.fromString(pageTokenStr)
        } else {
            PageToken() // Default empty PageToken
        }

        return PaginationRequest(pageToken, limit)
    }
}
