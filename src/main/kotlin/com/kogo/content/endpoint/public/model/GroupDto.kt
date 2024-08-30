package com.kogo.content.endpoint.public.model

import com.kogo.content.endpoint.public.validator.ValidTag
import com.kogo.content.storage.entity.GroupEntity
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
        val transformer: Transformer<GroupDto, GroupEntity> = object : Transformer<GroupDto, GroupEntity>(GroupDto::class, GroupEntity::class) {
            override fun argFor(parameter: KParameter, data: GroupDto): Any? {
                return when (parameter.name) {
                    "tags" -> with(data) { GroupEntity.parseTags(tags) }
                    "userCount" -> 1
                    "profileImage" -> null
                    "owner" -> null
                    else -> super.argFor(parameter, data)
                }
            }
        }
    }

    fun toEntity(): GroupEntity {
        return transformer.transform(this)
    }
}