package com.kogo.content.endpoint.resolver

import com.kogo.content.endpoint.common.PageToken
import com.kogo.content.endpoint.common.PaginationRequest
import com.kogo.content.endpoint.common.CursorValue
import com.kogo.content.endpoint.common.CursorValueType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

class PaginationRequestArgumentResolverTest {
    private val resolver = PaginationRequestArgumentResolver()
    private lateinit var mockMvc: MockMvc

    @RestController
    @RequestMapping("/test")
    class TestController {
        @GetMapping
        fun test(paginationRequest: PaginationRequest): PaginationRequest {
            return paginationRequest
        }
    }

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(TestController())
            .setCustomArgumentResolvers(resolver)
            .build()
    }

    @Test
    fun `should resolve pagination request with default values`() {
        mockMvc.get("/test")
            .andExpect {
                status { isOk() }
                jsonPath("$.limit") { value(10) }
                jsonPath("$.pageToken.cursors") { isEmpty() }
            }
    }

    @Test
    fun `should resolve pagination request with custom limit`() {
        mockMvc.get("/test") {
            param("limit", "20")
        }.andExpect {
            status { isOk() }
            jsonPath("$.limit") { value(20) }
        }
    }

    @Test
    fun `should resolve pagination request with existing page token`() {
        val existingToken = PageToken(
            cursors = mapOf(
                "id" to CursorValue("last-id", CursorValueType.STRING),
                "createdAt" to CursorValue("2024-01-01T00:00:00Z", CursorValueType.DATE)
            )
        ).encode()

        mockMvc.get("/test") {
            param("page_token", existingToken)
        }.andExpect {
            status { isOk() }
            jsonPath("$.pageToken.cursors.id.value") { value("last-id") }
            jsonPath("$.pageToken.cursors.id.type") { value("STRING") }
            jsonPath("$.pageToken.cursors.createdAt.type") { value("DATE") }
        }
    }

    @Test
    fun `should handle invalid page token gracefully`() {
        mockMvc.get("/test") {
            param("page_token", "invalid-token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.pageToken.cursors") { isEmpty() }
        }
    }

    @Test
    fun `should handle invalid limit parameter gracefully`() {
        mockMvc.get("/test") {
            param("limit", "invalid")
        }.andExpect {
            status { isOk() }
            jsonPath("$.limit") { value(10) }  // Should use default value
        }
    }
}
