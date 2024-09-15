package com.kogo.content.storage

import com.mongodb.BasicDBObjectBuilder
import com.mongodb.DBObject
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.MongoTemplate
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile

@SpringBootTest
class MongoDbSpringIntegrationTest @Autowired constructor(
    private val mongoTemplate : MongoTemplate
) {

    @Test
    fun `should save object using mongo template`() {
        val objectBuilder = BasicDBObjectBuilder.start()
        objectBuilder.add("key", "value")
        val dbObject : DBObject = objectBuilder.get()

        mongoTemplate.save(dbObject, "test-collection")
        mongoTemplate.findAll(DBObject::class.java, "test-collection")

        assertThat(mongoTemplate.findAll(DBObject::class.java, "test-collection"))
            .extracting("key").containsOnly("value")
    }

    @Test
    fun `should employ customer converter defined`() {
        val objectBuilder = BasicDBObjectBuilder.start()
        val mockMultipartFile = MockMultipartFile("data", "image.jpeg", "image/jpeg", "some image".toByteArray())
        objectBuilder.add("mockMultipartFile", mockMultipartFile)
        val dbObject : DBObject = objectBuilder.get()
        mongoTemplate.save(dbObject, "collection")
    }
}
