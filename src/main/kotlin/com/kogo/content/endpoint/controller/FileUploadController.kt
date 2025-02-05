package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.endpoint.model.FileUploaderDto
import com.kogo.content.endpoint.model.GroupDto
import com.kogo.content.endpoint.model.GroupResponse
import com.kogo.content.service.fileuploader.FileUploaderService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("media/files")
class FileUploadController @Autowired constructor(
    private val fileUploaderService: FileUploaderService
) {
    @RequestMapping(
        path = ["images"],
        method = [RequestMethod.POST],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @RequestBody(content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = Schema(implementation = FileUploaderDto.Image::class))])
    @Operation(
        summary = "upload and stale an image file, will return a token indicating the stored image file that lives until expiry.",
        requestBody = RequestBody(),
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = GroupResponse::class))],
        )])
    fun uploadImage(dto: FileUploaderDto.Image): ResponseEntity<*> {
        val imageFile = dto.image

        // upload an image using fileUploaderService(schedule)
        val attachment = fileUploaderService.staleImage(imageFile)

        // return the stale file token
        val stalingToken = attachment.id
        return HttpJsonResponse.successResponse(stalingToken)
    }
}


