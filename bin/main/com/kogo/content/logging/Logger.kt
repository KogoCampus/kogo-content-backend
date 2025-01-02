package com.kogo.content.logging

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.sentry.Sentry

abstract class Logger {
    // Create a logger instance for the class extending this
    private val kotlinLogger = KotlinLogging.logger { }

    val log = LoggerInstance(kotlinLogger)

    class LoggerInstance(private val kLogger: KLogger) : KLogger by kLogger {
        // Override the error method to include Sentry reporting
        override fun error(throwable: Throwable?, msg: () -> Any?) {
            kLogger.error(throwable, msg)

            throwable?.let {
                Sentry.captureException(it)
            }
        }
    }
}
