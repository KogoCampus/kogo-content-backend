package com.kogo.content.storage.converter

import com.kogo.content.filesystem.FileStoreKey
import org.springframework.stereotype.Component
import org.springframework.core.convert.converter.Converter

@Component
class FileStoreKeyConverter: MongodbConverter<FileStoreKey, String> {
    override fun readConverter(): Converter<String, FileStoreKey> {
        return object : Converter<String, FileStoreKey> {
            override fun convert(source: String): FileStoreKey = FileStoreKey(source)
        }
    }

    override fun writeConverter(): Converter<FileStoreKey, String> {
        return object : Converter<FileStoreKey, String> {
            override fun convert(source: FileStoreKey): String = source.toString()
        }
    }
}
