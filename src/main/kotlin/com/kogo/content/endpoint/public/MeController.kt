package com.kogo.content.endpoint.public

import com.kogo.content.endpoint.common.ApiResponse
import com.kogo.content.endpoint.public.model.UserDto
import com.kogo.content.endpoint.public.model.PostDto
import org.springframework.beans.factory.annotation.Autowired
import com.kogo.content.service.MeService
import com.kogo.content.service.UserService
import com.kogo.content.storage.entity.ProfileImage
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("media")
class MeController @Autowired constructor(
    private val meService : MeService,
    private val userService: UserService
) {
    // TO BE DELETED
    // START
    // create a user with a username of "testUser" for testing
    @RequestMapping(
        path = ["me"],
        method = [RequestMethod.POST],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    ) fun createUser(
        @Valid userDto: UserDto
    ) : ApiResponse {
        val user = userService.createUser(userDto)
        return ApiResponse.success(user)
    }
    // END

    @GetMapping("me")
    fun getMe(
    ): ApiResponse {
        return ApiResponse.success(meService.getUser())
    }

    @RequestMapping(
        path = ["me"],
        method = [RequestMethod.PUT],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    ) fun updateMe(
        @RequestPart("username", required = false) username: String?,
        @RequestPart("profileImage", required = false) profileImage: MultipartFile?,
    ) : ApiResponse {
        val attributes = mapOf(
            "username" to username,
            "profileImage" to profileImage,
        ).filterValues { it != null }

        if (attributes.isEmpty())
            throw IllegalArgumentException("Empty request body")

        return ApiResponse.success(meService.updateUserDetails(attributes))
    }

    @GetMapping("me/ownership/posts")
    fun getMePosts(
    ): ApiResponse {
        return ApiResponse.success(meService.getUserPosts())
    }

    @GetMapping("me/ownership/groups")
    fun getMeGroups(
    ): ApiResponse {
        return ApiResponse.success(meService.getUserGroups())
    }

    @GetMapping("me/following")
    fun getMeFollowing(
    ): ApiResponse {
        return ApiResponse.success(meService.getUserFollowing())
    }

}