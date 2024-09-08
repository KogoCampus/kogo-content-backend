package com.kogo.content.storage

import com.kogo.content.storage.entity.StudentUserEntity
import com.kogo.content.util.fixture
import com.mongodb.BasicDBObjectBuilder
import com.mongodb.DBObject
import jakarta.validation.ValidationException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

@DataMongoTest
@Import(MongoDBConfig::class)
class MongoDbSpringIntegrationTest @Autowired constructor(
    private val mongoTemplate : MongoTemplate
) {

    @Test
    fun `should save object using mongo template`() {
        val objectBuilder = BasicDBObjectBuilder.start()
        objectBuilder.add("key", "value")
        val dbObject : DBObject = objectBuilder.get()

        mongoTemplate.save(dbObject, "collection")

        mongoTemplate.findAll(DBObject::class.java, "collection")

        assertThat(mongoTemplate.findAll(DBObject::class.java, "collection"))
            .extracting("key").containsOnly("value")
    }

    @Test
    fun `should validate entity before creation`() {
        val userWithoutUsername = fixture<StudentUserEntity> { mapOf("username" to "") }

        assertThrows<ValidationException> { mongoTemplate.save(userWithoutUsername, "collection") }
    }
}