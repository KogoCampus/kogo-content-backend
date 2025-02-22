package com.kogo.content.endpoint.model

data class FriendResponse (
    var friend: UserData.Public,
    var nickname: String,
    var appLocalData: String?,
)
