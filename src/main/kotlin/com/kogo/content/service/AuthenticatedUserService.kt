package com.kogo.content.service

import com.kogo.content.endpoint.model.UserDto
import com.kogo.content.storage.entity.StudentUserEntity
import com.kogo.content.storage.repository.StudentUserRepository
import com.kogo.content.storage.repository.saveOrThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

// Temporary file - to be modified or deleted
@Service
class AuthenticatedUserService @Autowired constructor(
    private val repository: StudentUserRepository,
    private val attachmentService: AttachmentService
) : EntityService<StudentUserEntity, UserDto> {
    fun createUser(userDto: UserDto): StudentUserEntity {
        val newUser = userDto.toEntity()
        val savedUser = repository.saveOrThrow(newUser)

        val profileImage = userDto.profileImage?.takeIf { !it.isEmpty }?.let {
            attachmentService.saveAttachment(it, savedUser.id)
        }
        savedUser.profileImage = profileImage
        return repository.saveOrThrow(newUser)
    }

    fun findUser(userName: String): StudentUserEntity? {
        return repository.findByUsername(userName)
    }
}