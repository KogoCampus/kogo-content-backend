package com.kogo.content.storage.repository

import java.util.*

interface MongoEntityRepository<T, ID> {

    fun <S : T> save(entity: S): S

    fun findById(id: ID): Optional<T>?
}