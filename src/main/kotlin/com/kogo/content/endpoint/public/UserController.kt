package com.kogo.content.endpoint.public

import com.kogo.content.logging.Logger
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/user")
class UserController(
    // val todoService: TodoService
) {
    companion object : Logger() {}

    //@GetMapping("/me")
    //fun me(@PathVariable id: String): ResponseEntity<TodoResponse> {
    //    val todo = todoService.find(id)
    //    return ResponseEntity.ok(todo.toTodoResponse())
    //}
}