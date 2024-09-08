package com.kogo.content.endpoint.model

import com.kogo.content.validator.ValidTag
import com.kogo.content.storage.entity.TopicEntity
import com.kogo.content.util.Transformer
import jakarta.validation.constraints.NotBlank
import org.springframework.web.multipart.MultipartFile
import kotlin.reflect.KParameter

data class GroupDto (
    @field:NotBlank
    var groupName: String = "",

    var description: String = "",

    @ValidTag
    var tags: String = "",

    var profileImage: MultipartFile? = null

) : BaseDto() {
    companion object {
        val transformer: Transformer<GroupDto, TopicEntity> = object : Transformer<GroupDto, TopicEntity>(GroupDto::class, TopicEntity::class) {
            override fun argFor(parameter: KParameter, data: GroupDto): Any? {
                return when (parameter.name) {
                    "tags" -> with(data) { TopicEntity.parseTags(tags) }
                    "userCount" -> 1
                    "profileImage" -> null
                    "owner" -> null
                    else -> super.argFor(parameter, data)
                }
            }
        }
    }

    fun toEntity(): TopicEntity {
        return transformer.transform(this)
    }
}