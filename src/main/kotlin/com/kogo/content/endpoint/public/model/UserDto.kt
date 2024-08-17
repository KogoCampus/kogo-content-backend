package com.kogo.content.endpoint.public.model

import com.kogo.content.storage.entity.UserEntity
import com.kogo.content.util.Transformer
import jakarta.validation.constraints.NotBlank
import kotlin.reflect.KParameter

// Temp file - to be modified or deleted
data class UserDto (
    @field:NotBlank
    var username: String = "",

) : BaseDto() {
    companion object {
        val transformer: Transformer<UserDto, UserEntity> = object : Transformer<UserDto, UserEntity>(UserDto::class, UserEntity::class ){
            override fun argFor(parameter: KParameter, data: UserDto): Any? {
                return when (parameter.name) {
                    "id" -> null
                    "email" -> null
                    else -> super.argFor(parameter, data)
                }
            }
        }
    }

    fun toEntity(): UserEntity {
        return transformer.transform(this)
    }
}