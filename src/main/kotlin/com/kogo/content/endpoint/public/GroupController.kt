package com.kogo.content.endpoint.public

import com.kogo.content.endpoint.public.model.GroupDto
import com.kogo.content.logging.Logger
import com.kogo.content.service.EntityService
import com.kogo.content.storage.entity.GroupEntity
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/media")
class GroupController(
    private val entityService : EntityService<GroupEntity, GroupDto>
) {
    companion object : Logger()

    @GetMapping("/groups/{id}")
    fun getGroup(@PathVariable("id") groupId: String) {
        /*ResponseEntity<GroupEntity> {
            return groupService.find(groupId)?.let {
                ResponseEntity.ok(it)
            } ?: ResponseEntity.notFound().build()
        }*/
    }

    @PostMapping("/groups")
    fun createGroup(@Valid @RequestBody groupDto: GroupDto) {
        /*ResponseEntity<GroupEntity> {
            return groupService.create(groupDto)?.let {
                ResponseEntity.ok(it)
            } ?: ResponseEntity.unprocessableEntity().build()
        }*/
    }
}