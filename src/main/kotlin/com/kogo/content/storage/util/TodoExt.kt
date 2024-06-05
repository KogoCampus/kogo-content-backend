package com.kogo.content.storage.util

import com.kogo.content.endpoint.public.model.TodoRequest
import com.kogo.content.endpoint.public.model.TodoResponse
import com.kogo.content.storage.entity.Todo
import kotlin.reflect.full.memberProperties

fun Todo.toTodoRequest(): TodoRequest = with(::TodoRequest) {
    val propertiesByName = Todo::class.memberProperties.associateBy { it.name }
    callBy(parameters.associateWith { parameter -> propertiesByName[parameter.name]?.get(this@toTodoRequest) })
}

fun Todo.toTodoResponse(): TodoResponse = with(::TodoResponse) {
    val propertiesByName = Todo::class.memberProperties.associateBy { it.name }
    callBy(parameters.associateWith { parameter -> propertiesByName[parameter.name]?.get(this@toTodoResponse) })
}