package com.kogo.content.logging

import io.github.oshai.kotlinlogging.KotlinLogging

abstract class Logger {
    val log = KotlinLogging.logger() {}
}