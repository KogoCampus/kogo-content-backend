package com.kogo.content.storage.repository

<<<<<<< HEAD
import com.kogo.content.common.PaginationRequest
import com.kogo.content.common.PaginationSlice
import com.kogo.content.storage.MongoPaginationQueryBuilder
import com.kogo.content.storage.entity.Notification
import com.kogo.content.storage.entity.User
import org.springframework.beans.factory.annotation.Autowired
=======
import com.kogo.content.storage.model.entity.User
>>>>>>> 83a4b20 (fix: refactor entity structure and remove views)
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface UserRepository : MongoRepository<User, String> {
    fun findByUsername(username: String): User?

    @Query("{ '_id': ?0 }")
    fun findUserById(id: String): User?
}
