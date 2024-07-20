package com.kogo.content.endpoint.public.model

import com.kogo.content.storage.entity.GroupEntity
import com.kogo.content.util.Transformer
import jakarta.validation.constraints.NotBlank

data class GroupDto (
    @field:NotBlank
    var groupName: String = "",
) : BaseDto() {
    companion object {
        val transformer: Transformer<GroupDto, GroupEntity> = object :
            Transformer<GroupDto, GroupEntity>(GroupDto::class, GroupEntity::class) {}
    }

    fun toEntity(): GroupEntity {
        return transformer.transform(this)
    }
}