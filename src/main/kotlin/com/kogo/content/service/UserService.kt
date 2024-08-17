package com.kogo.content.service

import com.kogo.content.endpoint.public.model.UserDto
import com.kogo.content.storage.entity.UserEntity
import com.kogo.content.storage.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

// Temporary file - to be modified or deleted
@Service
class UserService @Autowired constructor(
    private val repository: UserRepository,
) : EntityService<UserEntity, UserDto> {
    fun createUser(username: String): UserEntity {
        val newUser = UserEntity(
            username = username,
        )
        return repository.save(newUser)
    }
}