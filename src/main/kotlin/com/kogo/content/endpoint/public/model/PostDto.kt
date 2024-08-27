package com.kogo.content.endpoint.public.model

import com.kogo.content.storage.entity.PostEntity
import com.kogo.content.storage.entity.Attachment
import com.kogo.content.util.Transformer
import jakarta.validation.constraints.NotBlank
import org.springframework.web.multipart.MultipartFile
import kotlin.reflect.KParameter

class PostDto (
    @field:NotBlank
    var title: String = "",

    @field:NotBlank
    var content: String = "",

    var attachments: List<MultipartFile>? = null

) : BaseDto() {
    companion object {
        val transformer: Transformer<PostDto, PostEntity> = object : Transformer<PostDto, PostEntity>(PostDto::class, PostEntity::class ){
            override fun argFor(parameter: KParameter, data: PostDto): Any? {
                return when (parameter.name) {
                    "comments" -> emptyList<String>()
                    "commentCount" -> 0
                    "group" -> null
                    "viewed" -> false
                    "liked" -> false
                    "attachments" -> emptyList<Attachment>()
                    "author" -> null
                    else -> super.argFor(parameter, data)
                }
            }
        }
    }

    fun toEntity(): PostEntity {
        return transformer.transform(this)
    }
}