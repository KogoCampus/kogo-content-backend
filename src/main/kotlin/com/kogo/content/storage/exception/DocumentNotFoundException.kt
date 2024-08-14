package com.kogo.content.storage.exception

import kotlin.reflect.KClass

class DocumentNotFoundException (
    val documentType: KClass<*>,
    val documentId: Any,
) : RuntimeException("")