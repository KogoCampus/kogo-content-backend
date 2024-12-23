package com.kogo.content.search.index

import com.kogo.content.search.*
import com.kogo.content.storage.model.entity.User
import org.springframework.stereotype.Service

@Service
class UserSearchIndex : SearchIndex<User>(User::class)
