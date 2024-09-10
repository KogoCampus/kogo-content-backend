package com.kogo.content.endpoint

import org.springframework.boot.actuate.health.AbstractHealthIndicator
import org.springframework.boot.actuate.health.Health
import org.springframework.stereotype.Component

@Component
class HealthIndicator : AbstractHealthIndicator() {

    @Override
    override fun doHealthCheck(builder: Health.Builder) {
        builder.up()
    }
}