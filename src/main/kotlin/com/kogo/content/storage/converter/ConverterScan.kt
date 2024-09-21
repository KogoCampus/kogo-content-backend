package com.kogo.content.storage.converter

import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class ConverterScan(mongodbConverters: List<MongodbConverter<*, *>>) {
    var convertersToRegister: List<Converter<*, *>> =
        mongodbConverters.map { listOf(it.readConverter(), it.writeConverter()) }.flatten()
}
