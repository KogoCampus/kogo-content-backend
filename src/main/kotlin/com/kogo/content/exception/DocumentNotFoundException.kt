package com.kogo.content.exception

import kotlin.reflect.KClass

class DocumentNotFoundException (
    val documentType: KClass<*>,
    val documentId: String,
) : RuntimeException("")