package com.example.springboot.restapi.interceptor

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView
import java.lang.Exception
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class UserInterceptor: HandlerInterceptor {
    private val log = KotlinLogging.logger {}

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        val startTime = System.currentTimeMillis()
        log.debug { "Request URL::" + request.requestURL.toString() + ":: " + "Start Time=" + startTime }
        request.setAttribute("startTime", startTime)
        return super.preHandle(request, response, handler)
    }

    override fun postHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        modelAndView: ModelAndView?
    ) {
        log.debug { "Request URL::" + request.requestURL.toString() + " Sent to Handler :: " + "Current Time=" + System.currentTimeMillis() }
        super.postHandle(request, response, handler, modelAndView)
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        val startTime = request.getAttribute("startTime") as Long
        log.info {
            "Request URL::" + request.requestURL.toString() + ":: " + "End Time=" + System.currentTimeMillis()
        }
        log.info {
            "Request URL::" + request.requestURL.toString() + ":: " + "Time Taken=" + (System.currentTimeMillis() - startTime)
        }
        super.afterCompletion(request, response, handler, ex)
    }
}