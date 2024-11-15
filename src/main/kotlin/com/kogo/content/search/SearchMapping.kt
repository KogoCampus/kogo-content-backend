package com.kogo.content.search

import org.bson.Document

data class SearchMapping(
    val dynamic: Boolean = true,
    val fields: Map<String, FieldMapping> = emptyMap()
) {
    fun toDocument(): Document = Document().apply {
        put("mappings", Document().apply {
            put("dynamic", dynamic)
            if (fields.isNotEmpty()) {
                put("fields", Document().apply {
                    fields.forEach { (field, mapping) ->
                        put(field, mapping.toDocument())
                    }
                })
            }
        })
    }

    companion object {
        fun builder() = Builder()
    }

    class Builder {
        private var dynamic: Boolean = true
        private val fields = mutableMapOf<String, FieldMapping>()

        fun dynamic(dynamic: Boolean) = apply { this.dynamic = dynamic }

        fun addField(path: String, type: FieldType, analyzer: String? = null) = apply {
            fields[path] = FieldMapping(type, analyzer)
        }

        fun build() = SearchMapping(dynamic, fields.toMap())
    }
}

data class FieldMapping(
    val type: FieldType,
    val analyzer: String? = null
) {
    fun toDocument() = Document("type", type.value).apply {
        analyzer?.let { append("analyzer", it) }
    }
}

enum class FieldType(val value: String) {
    STRING("string"),
    NUMBER("number"),
    DATE("date"),
    BOOLEAN("boolean"),
    GEO("geo"),
    AUTOCOMPLETE("autocomplete"),
    TOKEN("token")
}
