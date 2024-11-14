package com.kogo.content.storage.entity.converter

import com.kogo.content.storage.entity.UserIdToken
import org.springframework.stereotype.Component
import org.springframework.core.convert.converter.Converter

@Component
class UserIdTokenConverter: MongodbConverter<UserIdToken, String> {
    override fun readConverter(): Converter<String, UserIdToken> {
        return object : Converter<String, UserIdToken> {
            override fun convert(source: String): UserIdToken = UserIdToken.parse(source)
        }
    }

    override fun writeConverter(): Converter<UserIdToken, String> {
        return object : Converter<UserIdToken, String> {
            override fun convert(source: UserIdToken): String = source.toString()
        }
    }
}
