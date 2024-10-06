package com.kogo.content.searchengine

data class QueryOptions(
    val queryString: String?,           // keyword
    val filters: List<Filter>,
)

interface Filter {
    fun build(): String
}

class PostFilter(val timestamp: Long?) : Filter {
    override fun build(): String {
        return if (timestamp != null) {
            "createdAt < '$timestamp'"
        } else {
            ""
        }
    }
}

class CommentFilter(val timestamp: Long?) : Filter {
    override fun build(): String {
        return if (timestamp != null) {
            "parentType = 'POST' AND parentCreatedAt < '$timestamp'"
        } else {
            "parentType = 'POST'"
        }
    }
}
