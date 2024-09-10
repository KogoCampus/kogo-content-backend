package com.kogo.content.endpoint.controller
/*
import com.kogo.content.endpoint.common.Response
import org.springframework.beans.factory.annotation.Autowired
import com.kogo.content.service.MeService
import com.kogo.content.service.AuthenticatedUserService
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("media")
class MeController @Autowired constructor(
    private val meService : MeService,
    private val authenticatedUserService: AuthenticatedUserService
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
    ) : Response {
        val user = authenticatedUserService.createUser(userDto)
        return Response.success(user)
    }
    // END

    @GetMapping("me")
    fun getMe(
    ): Response {
        return Response.success(meService.getUser())
    }

    @RequestMapping(
        path = ["me"],
        method = [RequestMethod.PUT],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    ) fun updateMe(
        @RequestPart("username", required = false) username: String?,
        @RequestPart("profileImage", required = false) profileImage: MultipartFile?,
    ) : Response {
        val attributes = mapOf(
            "username" to username,
            "profileImage" to profileImage,
        ).filterValues { it != null }

        if (attributes.isEmpty())
            throw IllegalArgumentException("Empty request body")

        return Response.success(meService.updateUserDetails(attributes))
    }

    @GetMapping("me/ownership/posts")
    fun getMePosts(
    ): Response {
        return Response.success(meService.getUserPosts())
    }

    @GetMapping("me/ownership/groups")
    fun getMeGroups(
    ): Response {
        return Response.success(meService.getUserGroups())
    }

    @GetMapping("me/following")
    fun getMeFollowing(
    ): Response {
        return Response.success(meService.getUserFollowing())
    }

}
*/
