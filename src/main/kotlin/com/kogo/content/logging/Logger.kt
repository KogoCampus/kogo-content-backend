package com.kogo.content.logging

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.sentry.Sentry

abstract class Logger(loggerName: String? = null) {
    // Create a logger instance for the class extending this
    private val kotlinLogger = if (loggerName == null) KotlinLogging.logger { } else KotlinLogging.logger(loggerName)

    val log = LoggerInstance(kotlinLogger)

    class LoggerInstance(private val kLogger: KLogger) : KLogger by kLogger {
        override fun error(throwable: Throwable?, message: () -> Any?) {
            kLogger.error(throwable, message)

            throwable?.let {
                Sentry.captureException(it)
            }
        }

        override fun error(message: () -> Any?) {
            kLogger.error(message)
            Sentry.captureMessage(message().toString())
        }
    }
}
