package com.kogo.content.endpoint.model

data class FriendRequest (
    var friendEmail: String,
    var friendNickname: String,
)

data class AcceptFriendRequest (
    var requestedUserId: String,
    var friendNickname: String,
)
