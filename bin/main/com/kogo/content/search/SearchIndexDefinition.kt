package com.kogo.content.search

import org.bson.Document

data class SearchIndexDefinition(
    val dynamic: Boolean = true,
    val fields: Map<String, FieldDefinition> = emptyMap()
) {
    fun toDocument(): Document = Document().apply {
        put("mappings", Document().apply {
            put("dynamic", dynamic)
            if (fields.isNotEmpty()) {
                put("fields", Document().apply {
                    fields.forEach { (field, definition) ->
                        put(field, definition.toDocument())
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
        private val fields = mutableMapOf<String, FieldDefinition>()

        fun dynamic(dynamic: Boolean) = apply { this.dynamic = dynamic }

        fun field(path: String, definition: FieldDefinition) = apply {
            fields[path] = definition
        }

        // Helper method for document fields
        fun documentField(path: String, dynamic: Boolean = false, block: Builder.() -> Unit) = apply {
            val subBuilder = Builder().apply {
                dynamic(dynamic)
                block()
            }
            fields[path] = DocumentField(
                dynamic = dynamic,
                fields = subBuilder.build().fields
            )
        }

        // Helper methods for common field types
        fun stringField(path: String, analyzer: String? = "lucene.standard") = apply {
            fields[path] = StringField(analyzer)
        }

        fun dateField(path: String) = apply {
            fields[path] = DateField()
        }

        fun numberField(path: String) = apply {
            fields[path] = NumberField()
        }

        fun build() = SearchIndexDefinition(dynamic, fields.toMap())
    }
}

sealed class FieldDefinition {
    abstract fun toDocument(): Any
}

data class StringField(
    val analyzer: String? = "lucene.standard"
) : FieldDefinition() {
    override fun toDocument(): Any = listOf(
        Document("type", "token"),
        Document("type", "string").apply {
            analyzer?.let { put("analyzer", it) }
        }
    )
}

class DateField : FieldDefinition() {
    override fun toDocument(): Any = listOf(
        Document("type", "date"),
        Document("type", "dateFacet")
    )
}

class NumberField : FieldDefinition() {
    override fun toDocument(): Any = listOf(
        Document("type", "number"),
        Document("type", "numberFacet")
    )
}

data class DocumentField(
    val dynamic: Boolean = false,
    val fields: Map<String, FieldDefinition> = emptyMap()
) : FieldDefinition() {
    override fun toDocument(): Any = Document().apply {
        put("type", "document")
        put("dynamic", dynamic)
        if (fields.isNotEmpty()) {
            put("fields", Document().apply {
                fields.forEach { (field, definition) ->
                    put(field, definition.toDocument())
                }
            })
        }
    }
}
