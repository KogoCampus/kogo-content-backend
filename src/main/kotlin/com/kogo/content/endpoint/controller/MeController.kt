package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.PaginationRequest
import com.kogo.content.endpoint.common.PaginationSlice
import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.endpoint.model.*
import com.kogo.content.logging.Logger
import com.kogo.content.service.PushNotificationService
import com.kogo.content.service.PostService
import com.kogo.content.service.GroupService
import com.kogo.content.service.UserService
import com.kogo.content.storage.model.Notification
import com.kogo.content.storage.model.NotificationType
import com.kogo.content.storage.model.entity.Friend
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class MeController @Autowired constructor(
    private val userService: UserService,
    private val groupService: GroupService,
    private val postService: PostService,
    private val pushNotificationService: PushNotificationService
) {
    companion object: Logger()

    @GetMapping("me")
    @Operation(
        summary = "get my user info",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = UserData.IncludeCredentials::class))]
        )])
    fun getMe() = run {
        val me = userService.findCurrentUser()
        HttpJsonResponse.successResponse(UserData.IncludeCredentials.from(me))
    }

    @RequestMapping(
        path = ["me"],
        method = [RequestMethod.PUT],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @RequestBody(content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = Schema(implementation = UserUpdate::class))])
    @Operation(
        summary = "update my user info",
        requestBody = RequestBody(),
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = UserData.IncludeCredentials::class))]
        )]
    )
    fun updateMe(@Valid meUpdate: UserUpdate): ResponseEntity<*> {
        val me = userService.findCurrentUser()

        if (meUpdate.username != null) {
            val existingUser = userService.findUserByUsername(meUpdate.username!!)
            if (existingUser != null && existingUser.id != me.id) {
                return HttpJsonResponse.errorResponse(ErrorCode.DUPLICATED, "User with the given username already exists")
            }
        }

        val updatedUser = userService.update(me, meUpdate)
        return HttpJsonResponse.successResponse(UserData.IncludeCredentials.from(updatedUser))
    }

    @GetMapping("me/ownership/posts")
    @Operation(
        summary = "get my posts",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", array = ArraySchema(
                schema = Schema(implementation = PostResponse::class)))],
        )])
    fun getPostsAuthoredByUser() = run {
        val me = userService.findCurrentUser()
        HttpJsonResponse.successResponse(postService.findAllByAuthor(me).map {
            PostResponse.from(it, me)
        })
    }

    @GetMapping("me/ownership/groups")
    @Operation(
        summary = "get my groups",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", array = ArraySchema(
                schema = Schema(implementation = GroupResponse::class)))],
        )])
    fun getGroupOwnedByUser() = run {
        val me = userService.findCurrentUser()
        HttpJsonResponse.successResponse(groupService.findByOwner(me).map {
            GroupResponse.from(it, me)
        })
    }

    @RequestMapping(
        path = ["me/ownership/groups/{groupId}/transfer"],
        method = [RequestMethod.POST],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        summary = "transfer the group ownership",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = GroupResponse::class))]
        )]
    )
    fun transferOwner(
        @PathVariable("groupId") groupId: String,
        @RequestParam("transfer_to") newOwnerId: String): ResponseEntity<*> = run {
        val group = groupService.findOrThrow(groupId)
        val newOwner = userService.findOrThrow(newOwnerId)
        val originalOwner = userService.findCurrentUser()

        if(group.owner?.id != originalOwner.id)
            return HttpJsonResponse.errorResponse(errorCode = ErrorCode.USER_ACTION_DENIED, "user is not the owner of this group")

        if(originalOwner.id == newOwnerId)
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "You cannot transfer ownership to yourself")

        groupService.follow(group, newOwner)
        val transferredGroup = groupService.transferOwnership(group, newOwner)

        HttpJsonResponse.successResponse(GroupResponse.from(transferredGroup, originalOwner))
    }

    @GetMapping("me/following")
    @Operation(
        summary = "get my following groups",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = GroupResponse::class))]
        )])
    fun getMeFollowing() = run {
        val me = userService.findCurrentUser()
        val followingGroups = groupService.findAllByFollowerId(me.id!!)
        HttpJsonResponse.successResponse(followingGroups.map{
            GroupResponse.from(it, me)
        })
    }

    @GetMapping(path = ["me/notifications"])
    @Operation(
        summary = "get notifications whose recipient is me",
        parameters = [
            Parameter(
                name = PaginationRequest.PAGE_TOKEN_PARAM,
                description = "page token",
                schema = Schema(type = "string"),
                required = false
            ),
            Parameter(
                name = PaginationRequest.PAGE_SIZE_PARAM,
                description = "limit for pagination",
                schema = Schema(type = "integer", defaultValue = "10"),
                required = false
            )],
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            headers = [
                Header(name = PaginationSlice.HEADER_PAGE_TOKEN, schema = Schema(type = "string")),
                Header(name = PaginationSlice.HEADER_PAGE_SIZE, schema = Schema(type = "string")),
                    ],
            content = [Content(mediaType = "application/json", array = ArraySchema(
                schema = Schema(implementation = Notification::class)
            ))],
        )]
    ) fun getNotifications(
        paginationRequest: PaginationRequest
    ): ResponseEntity<*> = run {
        val me = userService.findCurrentUser()
        val paginationResponse = pushNotificationService.getNotificationsByRecipientId(me.id!!, paginationRequest)

        HttpJsonResponse.successResponse(
            data = paginationResponse.items,
            headers = paginationResponse.toHttpHeaders()
        )
    }

    @DeleteMapping("me/notifications/{notificationId}")
    @Operation(
        summary = "delete a notification",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok"
        )]
    )
    fun deleteNotification(@PathVariable("notificationId") notificationId: String): ResponseEntity<*> = run {
        val me = userService.findCurrentUser()

        val notification = pushNotificationService.find(notificationId)
            ?: return HttpJsonResponse.errorResponse(ErrorCode.NOT_FOUND, "Notification not found for the given notificationId $notificationId")

        if (notification.recipient.id != me.id) {
            return HttpJsonResponse.errorResponse(ErrorCode.UNAUTHORIZED, "Notification does not belong to the user")
        }

        pushNotificationService.deleteNotification(notificationId, me.id!!)
        HttpJsonResponse.successResponse(null)
    }

    @PostMapping("me/blacklist")
    @Operation(
        summary = "add a user to my blacklist",
        parameters = [
            Parameter(
                name = "user_id",
                description = "ID of the user to blacklist",
                schema = Schema(type = "string"),
                required = true
            )
        ],
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = UserData.IncludeCredentials::class))]
        )]
    )
    fun addUserToBlacklist(@RequestParam("user_id") targetUserId: String): ResponseEntity<*> = run {
        val me = userService.findCurrentUser()
        val targetUser = userService.findOrThrow(targetUserId)

        if (me.id == targetUserId) {
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "You cannot add yourself to blacklist")
        }

        val updatedUser = userService.addUserToBlacklist(me, targetUser)

        HttpJsonResponse.successResponse(UserData.IncludeCredentials.from(updatedUser))
    }

    @DeleteMapping("me/blacklist/{user_id}")
    @Operation(
        summary = "remove a user from my blacklist",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = UserData.IncludeCredentials::class))]
        )]
    )
    fun removeUserFromBlacklist(@PathVariable("user_id") targetUserId: String): ResponseEntity<*> = run {
        val me = userService.findCurrentUser()
        val targetUser = userService.findOrThrow(targetUserId)

        val updatedUser = userService.removeUserFromBlacklist(me, targetUser)

        HttpJsonResponse.successResponse(UserData.IncludeCredentials.from(updatedUser))
    }

    @RequestMapping(
        path = ["me/friends"],
        method = [RequestMethod.POST],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        summary = "send a friend request to user",
        requestBody = RequestBody(content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = Schema(implementation = FriendRequest::class))]),
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = UserData.IncludeCredentials::class))]
        )]
    )
    fun sendFriendRequest(@Valid friendRequest: FriendRequest): ResponseEntity<*> = run {
        val me = userService.findCurrentUser()
        val targetUser = userService.findUserByEmail(friendRequest.friendEmail)
            ?: return HttpJsonResponse.errorResponse(ErrorCode.NOT_FOUND, "User not found for the given email ${friendRequest.friendEmail}")

        if (targetUser.id == me.id)
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "You cannot send yourself to friend")

        val updatedUser = userService.sendFriendRequest(me, targetUser, friendRequest.friendNickname)

        HttpJsonResponse.successResponse(UserData.IncludeCredentials.from(updatedUser))
    }

    @RequestMapping(
        path = ["me/friends/accept"],
        method = [RequestMethod.PUT],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        summary = "accept friend request",
        requestBody = RequestBody(content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = Schema(implementation = AcceptFriendRequest::class))]),
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = UserData.IncludeCredentials::class))]
        )]
    )
    fun acceptFriendRequest(@Valid acceptRequest: AcceptFriendRequest): ResponseEntity<*> = run {
        val me = userService.findCurrentUser()
        val requestedUser = userService.find(acceptRequest.requestedUserId)
            ?: return HttpJsonResponse.errorResponse(ErrorCode.NOT_FOUND, "User not found for the given id ${acceptRequest.requestedUserId}")

        if (requestedUser.friends.any { it.user.id == me.id }) {
            if (requestedUser.friends.any { it.user.id == me.id && it.status == Friend.FriendStatus.ACCEPTED }) {
                return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "User has already accepted a friend request for you")
            }
        } else {
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "This user has not made a friend request for you")
        }

        val updatedUser = userService.acceptFriendRequest(me, requestedUser, acceptRequest.friendNickname)

        HttpJsonResponse.successResponse(UserData.IncludeCredentials.from(updatedUser))
    }

    @DeleteMapping("me/profileImage")
    @Operation(
        summary = "delete my profile image",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = UserData.IncludeCredentials::class))]
        )]
    )
    fun deleteProfileImage(): ResponseEntity<*> = run {
        val me = userService.findCurrentUser()
        val updatedUser = userService.deleteProfileImage(me)
        HttpJsonResponse.successResponse(UserData.IncludeCredentials.from(updatedUser))
    }
}

