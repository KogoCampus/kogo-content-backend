package com.kogo.content.exception

import kotlin.reflect.KClass

data class MongoViewException (
    val viewId: String,
    val viewClass: KClass<out Any>,
    override val message: String
) : RuntimeException(message)
