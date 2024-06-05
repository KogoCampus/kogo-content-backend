package com.kogo.content.endpoint.public

import com.example.springboot.restapi.domain.TodoRequest
import com.example.springboot.restapi.domain.TodoResponse
import com.example.springboot.restapi.model.toTodoResponse
import com.example.springboot.restapi.service.TodoService
import com.kogo.content.logging.Logger
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/todos")
class UserController(
    val todoService: TodoService
) {
    companion object : Logger() {}

    @GetMapping("/{id}")
    fun getTodo(@PathVariable id: String): ResponseEntity<TodoResponse> {
        val todo = todoService.find(id)
        return ResponseEntity.ok(todo.toTodoResponse())
    }

    @PostMapping
    fun createTodo(@RequestBody todoRequest: TodoRequest) : ResponseEntity<TodoResponse> {
        val todo = todoService.create(todoRequest)
        return ResponseEntity.ok(todo.toTodoResponse())
    }
}