package com.kogo.content.endpoint.public.model

data class TodoRequest (
    var id: String? = null,
    var title: String = "",
    var description: String = "",
    var completed: Boolean = false
)