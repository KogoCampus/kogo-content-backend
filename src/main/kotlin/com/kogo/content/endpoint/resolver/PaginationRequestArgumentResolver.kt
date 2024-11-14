package com.kogo.content.endpoint.resolver

import com.kogo.content.lib.FilterField
import com.kogo.content.lib.FilterOperator
import com.kogo.content.lib.PageToken
import com.kogo.content.lib.PaginationRequest
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

class PaginationRequestArgumentResolver : HandlerMethodArgumentResolver {
    companion object {
        private const val PAGE_TOKEN_PARAM = "page_token"
        private const val PAGE_SIZE_PARAM = "limit"
        private const val SORT_PARAM = "sort"
        private const val FILTER_PARAM = "filter"
    }

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.parameterType == PaginationRequest::class.java
    }

    private fun parseFilter(filterStr: String): Pair<String, String> {
        val (field, value) = filterStr.split(":")
        return field to value
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

        // Parse sort parameters (format: field1:asc,field2:desc)
        val sortBy = requestParameters[SORT_PARAM]?.split(",")?.associate {
            val (field, direction) = it.split(":")
            field to direction
        } ?: emptyMap()

        // Parse filter parameters (format: field1:value1,field2:in(value2,value3))
        val filterBy = requestParameters[FILTER_PARAM]?.split(",")
            ?.map { parseFilter(it) }
            ?.toMap() ?: emptyMap()

        val pageToken = if (pageTokenStr != null) {
            PageToken.fromString(pageTokenStr)
        } else {
            PageToken.fromRequest(sortBy, filterBy)
        }

        return PaginationRequest(pageToken, limit)
    }
}
