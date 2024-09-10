package com.kogo.content.storage.converter

import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
/*
@Component
class MultipartFileConverter: MongodbConverter<MultipartFile?, FileDetails> {
    override fun readConverter(): Converter<FileDetails, MultipartFile?> {
        return object : Converter<FileDetails, MultipartFile?> {
            override fun convert(source: FileDetails): MultipartFile? = null
        }
    }

    override fun writeConverter(): Converter<MultipartFile?, FileDetails> {
        return object : Converter<MultipartFile?, FileDetails> {
            override fun convert(source: MultipartFile): FileDetails = FileDetails(source)
        }
    }
}
*/
