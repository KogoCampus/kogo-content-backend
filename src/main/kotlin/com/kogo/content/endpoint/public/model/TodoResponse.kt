package com.kogo.content.endpoint.public.model

data class TodoResponse (
    var id: String? = null,
    var title: String = "",
    var description: String = "",
    var completed: Boolean = false
)