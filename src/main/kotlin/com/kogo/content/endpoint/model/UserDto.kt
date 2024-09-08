package com.kogo.content.endpoint.model

import com.kogo.content.storage.entity.TopicEntity
import com.kogo.content.storage.entity.StudentUserEntity
import com.kogo.content.util.Transformer
import jakarta.validation.constraints.NotBlank
import org.springframework.web.multipart.MultipartFile
import kotlin.reflect.KParameter

// Temp file - to be modified or deleted
data class UserDto (
    @field:NotBlank
    var username: String = "",

    var email: String? = "",

    var profileImage: MultipartFile? = null

) : BaseDto() {
    companion object {
        val transformer: Transformer<UserDto, StudentUserEntity> = object : Transformer<UserDto, StudentUserEntity>(UserDto::class, StudentUserEntity::class ){
            override fun argFor(parameter: KParameter, data: UserDto): Any? {
                return when (parameter.name) {
                    "profileImage" -> null
                    "following" -> emptyList<TopicEntity>()
                    else -> super.argFor(parameter, data)
                }
            }
        }
    }

    fun toEntity(): StudentUserEntity {
        return transformer.transform(this)
    }
}