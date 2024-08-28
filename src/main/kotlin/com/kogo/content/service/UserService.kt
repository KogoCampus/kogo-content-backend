package com.kogo.content.service

import com.kogo.content.endpoint.public.model.UserDto
import com.kogo.content.storage.entity.UserEntity
import com.kogo.content.storage.repository.UserRepository
import com.kogo.content.storage.repository.saveOrThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

// Temporary file - to be modified or deleted
@Service
class UserService @Autowired constructor(
    private val repository: UserRepository,
    private val attachmentService: AttachmentService
) : EntityService<UserEntity, UserDto> {
    fun createUser(userDto: UserDto): UserEntity {
        val newUser = userDto.toEntity()
        val savedUser = repository.saveOrThrow(newUser)

        val profileImage = userDto.profileImage?.takeIf { !it.isEmpty }?.let {
            attachmentService.saveAttachment(it, savedUser.id)
        }
        savedUser.profileImage = profileImage
        return repository.saveOrThrow(newUser)
    }

    fun findUser(userName: String): UserEntity? {
        return repository.findByUsername(userName)
    }
}