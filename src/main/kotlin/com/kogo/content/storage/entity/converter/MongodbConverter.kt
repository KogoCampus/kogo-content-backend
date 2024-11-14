package com.kogo.content.storage.entity.converter

import org.springframework.core.convert.converter.Converter

interface MongodbConverter<READ, WRITE> {
    fun readConverter(): Converter<WRITE, READ>
    fun writeConverter(): Converter<READ, WRITE>
}
