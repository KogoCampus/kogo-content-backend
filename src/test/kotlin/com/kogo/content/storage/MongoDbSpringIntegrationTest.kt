package com.kogo.content.storage

import com.mongodb.BasicDBObjectBuilder
import com.mongodb.DBObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.assertj.core.api.Assertions.assertThat
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
}