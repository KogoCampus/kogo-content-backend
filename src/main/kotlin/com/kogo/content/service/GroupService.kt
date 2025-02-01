package com.kogo.content.service

import com.kogo.content.endpoint.model.GroupUpdate
import com.kogo.content.endpoint.common.PaginationRequest
import com.kogo.content.endpoint.common.PaginationSlice
import com.kogo.content.endpoint.model.Enrollment
import com.kogo.content.search.index.GroupSearchIndex
import com.kogo.content.service.PushNotificationService.Companion.log
import com.kogo.content.service.fileuploader.FileUploaderService
import com.kogo.content.storage.model.entity.*
import com.kogo.content.storage.repository.*
import com.kogo.content.util.convertTo12BytesHexString
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.repository.findByIdOrNull

@Service
class GroupService(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val groupSearchIndex: GroupSearchIndex,
    private val fileService: FileUploaderService,
    private val courseListingService: CourseListingService,
) : BaseEntityService<Group, String>(Group::class, groupRepository) {

    fun findByGroupName(groupName: String): Group? = groupRepository.findByGroupName(groupName)

    fun findByOwner(owner: User) = groupRepository.findAllByOwnerId(owner.id!!)

    fun findByCourseCode(courseCodeBase64: String) = groupRepository.findByIdOrNull(courseGroupId(courseCodeBase64))

    fun findSchoolGroup(schoolKey: String): Group? {
        return mongoTemplate.findOne(
            Query.query(Criteria.where("_id").`is`(schoolGroupId(schoolKey))),
            Group::class.java
        )
    }

    fun findAllByFollowerId(userId: String): List<Group> = groupRepository.findAllByFollowerId(userId)

    //fun findAllTrending(paginationRequest: PaginationRequest, user: User) = run {
    //    val matchOperation = Aggregation.match(
    //        Criteria().orOperator(
    //            Criteria.where("type").ne(GroupType.SCHOOL_GROUP),
    //            Criteria.where("_id").`is`(ObjectId(user.schoolInfo.schoolGroupId))
    //        )
    //    )
    //
    //    // Add field for follower count
    //    val addFieldsOperation = Aggregation.addFields()
    //        .addField("followerCount")
    //        .withValue(Document("\$size", "\$followers"))
    //        .build()
    //
    //    mongoPaginationQueryBuilder.getPage(
    //        entityClass = Group::class,
    //        paginationRequest = paginationRequest.withSort("followerCount", SortDirection.DESC),
    //        preAggregationOperations = listOf(matchOperation, addFieldsOperation),
    //        allowedDynamicFields = setOf("followerCount")
    //    )
    //}

    fun search(
        searchKeyword: String,
        paginationRequest: PaginationRequest
    ): PaginationSlice<Group> {
        return groupSearchIndex.search(
            searchText = searchKeyword,
            paginationRequest = paginationRequest,
        )
    }

    fun createSchoolGroup(schoolGroupOwner: User, schoolInfo: SchoolInfo): Group {
        return groupRepository.save(
            Group(
                id = schoolGroupId(schoolKey = schoolInfo.schoolKey),
                groupName = schoolInfo.schoolName,
                description = "Official group for ${schoolInfo.schoolName}",
                owner = schoolGroupOwner,
                type = GroupType.SCHOOL_GROUP
            )
        )
    }

    @Transactional
    fun updateCourseEnrollment(courseGroupOwner: User, user: User, enrollment: Enrollment): List<Group> {
        val courseListing = courseListingService.retrieveCourseListing(enrollment.schoolKey)
        val newCourseIds = enrollment.base64CourseCodes.map { courseGroupId(it) }

        // Find and unfollow old course groups
        val coursesToDrop = user.followingGroupIds
            .mapNotNull { find(it) }
            .filter { followingGroup ->
                followingGroup.type == GroupType.COURSE_GROUP && !newCourseIds.contains(followingGroup.id)
            }

        coursesToDrop.forEach {
            unfollow(it, user)
        }

        return enrollment.base64CourseCodes.map {
            val courseInfo = courseListing.getCourse(it)

            val courseGroup = findByCourseCode(it) ?: groupRepository.save(
                Group(
                    id = courseGroupId(it),
                    groupName = courseInfo["courseCode"].asText(),
                    description = courseInfo["courseName"].asText(),
                    owner = courseGroupOwner,
                    profileImage = null,
                    tags = mutableListOf(),
                    type = GroupType.COURSE_GROUP
                )
            )

            if (!courseGroup.isFollowedBy(user)) {
                follow(courseGroup, user)
                userRepository.save(user)
            }
            courseGroup
        }
    }

    // fun createUserGroup(dto: GroupDto, owner: User): Group {
    //     val profileImage = dto.profileImage?.let { fileService.uploadImage(it) }
    //     return groupRepository.save(
    //         Group(
    //             groupName = dto.groupName,
    //             description = dto.description,
    //             owner = owner,
    //             profileImage = profileImage,
    //             tags = dto.tags.toMutableList(),
    //             type = GroupType.USER_GROUP
    //         )
    //     )
    // }

    @Transactional
    fun update(group: Group, groupUpdate: GroupUpdate): Group {
        with(groupUpdate) {
            groupName?.let { group.groupName = it }
            description?.let { group.description = it }
            tags?.let { group.tags = it.toMutableList() }
            profileImage?.let {
                group.profileImage?.let { profileImage ->
                    runCatching { fileService.deleteImage(profileImage.id!!) }
                        .onFailure { log.error(it) { "Failed to delete old profile image: ${profileImage.id}" } }
                }
                group.profileImage = fileService.uploadImage(it)
            }
        }
        group.updatedAt = System.currentTimeMillis()
        return groupRepository.save(group)
    }

    @Transactional
    fun delete(group: Group) {
        group.profileImage?.let { profileImage ->
            fileService.deleteImage(profileImage.id!!)
        }
        groupRepository.deleteById(group.id!!)
    }

    @Transactional
    fun follow(group: Group, user: User): Boolean {
        if (group.followers.any { it.follower.id == user.id })
            return false
        group.followers.add(Follower(user))
        user.followingGroupIds.add(group.id!!)
        groupRepository.save(group)
        userRepository.save(user)
        return true
    }

    @Transactional
    fun unfollow(group: Group, user: User): Boolean {
        if (group.followers.any { it.follower.id == user.id }) {
            group.followers.removeIf { it.follower.id == user.id }
            user.followingGroupIds.remove(group.id!!)
            groupRepository.save(group)
            userRepository.save(user)
            return true
        }
        return false
    }

    @Transactional
    fun deleteProfileImage(group: Group): Group {
        group.profileImage?.let { fileService.deleteImage(it.id!!) }
        group.profileImage = null
        return groupRepository.save(group)
    }

    fun transferOwnership(group: Group, user: User): Group {
        group.owner = user
        groupRepository.save(group)
        return group
    }

    private fun courseGroupId(courseCodeBase64: String) = convertTo12BytesHexString(courseCodeBase64)
    private fun schoolGroupId(schoolKey: String) = convertTo12BytesHexString("school_group_${schoolKey}")
}
