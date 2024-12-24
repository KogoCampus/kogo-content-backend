package com.kogo.content.service

import com.kogo.content.endpoint.model.GroupDto
import com.kogo.content.endpoint.model.GroupUpdate
import com.kogo.content.endpoint.common.PaginationRequest
import com.kogo.content.endpoint.common.PaginationSlice
import com.kogo.content.search.index.GroupSearchIndex
import com.kogo.content.storage.model.entity.Group
import com.kogo.content.storage.repository.*
import com.kogo.content.storage.model.entity.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import kotlin.reflect.KClass

@Service
class GroupService(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val groupSearchIndex: GroupSearchIndex
) : BaseEntityService<Group, String>(Group::class, groupRepository) {

    fun findByGroupName(groupName: String): Group? = groupRepository.findByGroupName(groupName)

    fun findByOwner(owner: User) = groupRepository.findAllByOwnerId(owner.id!!)

    fun findAllByFollowerId(userId: String): List<Group> = groupRepository.findAllByFollowerId(userId)

    fun search(
        searchKeyword: String,
        paginationRequest: PaginationRequest
    ): PaginationSlice<Group> {
        return groupSearchIndex.search(
            searchText = searchKeyword,
            paginationRequest = paginationRequest,
        )
    }

    @Transactional
    fun create(dto: GroupDto, owner: User): Group = groupRepository.save(
        Group(
            groupName = dto.groupName,
            description = dto.description,
            owner = owner,
            profileImage = null,
            tags = dto.tags.toMutableList()
        )
    )


    @Transactional
    fun update(group: Group, groupUpdate: GroupUpdate): Group {
        with(groupUpdate) {
            groupName?.let { group.groupName = it }
            description?.let { group.description = it }
            tags?.let { group.tags = it.toMutableList() }
            // TODO
            // profileImage?.let { group.profileImage = attachmentRepository.saveFile(it) }
        }
        group.updatedAt = Instant.now()
        return groupRepository.save(group)
    }

    @Transactional
    fun delete(group: Group) {
        // TODO
        // group.profileImage?.let { attachmentRepository.delete(it) }
        groupRepository.deleteById(group.id!!)
    }

    @Transactional
    fun follow(group: Group, user: User): Boolean {
        if (group.followerIds.contains(user.id))
            return false
        group.followerIds.add(user.id!!)
        user.followingGroupIds.add(group.id!!)
        groupRepository.save(group)
        userRepository.save(user)
        return true
    }

    @Transactional
    fun unfollow(group: Group, user: User): Boolean {
        if (group.followerIds.contains(user.id)) {
            group.followerIds.remove(user.id!!)
            user.followingGroupIds.remove(group.id!!)
            groupRepository.save(group)
            userRepository.save(user)
            return true
        }
        return false
    }

    fun transferOwnership(group: Group, user: User): Group {
        group.owner = user
        groupRepository.save(group)
        return group
    }
}
