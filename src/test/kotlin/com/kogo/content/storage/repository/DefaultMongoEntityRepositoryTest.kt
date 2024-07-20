package com.kogo.content.storage.repository

import com.kogo.content.exception.DBAccessException
import com.kogo.content.storage.entity.GroupEntity
import com.kogo.content.util.fixture
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.core.MongoTemplate
import kotlin.test.assertEquals

@DataMongoTest
class DefaultMongoEntityRepositoryTest @Autowired constructor(
    private val entityRepository : GroupRepository
) {
    @MockkBean
    private lateinit var mongoTemplate: MongoTemplate

    @Nested
    inner class `when saving a group` {
        @Test
        fun `should save and return the saved group successfully`() {
            val entityToSave : GroupEntity = fixture<GroupEntity>()
            every { mongoTemplate.save(entityToSave) } returns entityToSave

            val result : GroupEntity = entityRepository.save(entityToSave)
            assertEquals(entityToSave, result)
        }

        @Test
        fun `should throw DBAccessException when MongoDB operation fails`() {
            val entityToSave : GroupEntity = fixture<GroupEntity>()
            Mockito.`when`(mongoTemplate.save(entityToSave)).thenThrow(RuntimeException())

            assertThrows<DBAccessException> {
                entityRepository.save(entityToSave)
            }
        }
    }

    @Nested
    inner class `when trying to find by id` {

        @Test
        fun `should throw DBAccessException when MongoDB operation fails`() {
            Mockito.`when`(mongoTemplate.findById("1", Mockito.any<Class<GroupEntity>>())).thenThrow(RuntimeException())

            assertThrows<DBAccessException> {
                entityRepository.findById("1")
            }
        }
    }
}