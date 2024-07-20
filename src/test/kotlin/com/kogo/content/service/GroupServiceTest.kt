package com.kogo.content.service

import com.kogo.content.endpoint.public.model.GroupDto
import com.kogo.content.exception.DocumentNotFoundException
import com.kogo.content.storage.entity.GroupEntity
import com.kogo.content.storage.repository.GroupRepository
import com.kogo.content.util.fixture
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.repository.findByIdOrNull
import kotlin.test.assertEquals

class GroupServiceTest {

    private val groupRepository : GroupRepository = mockk()

    private val groupService : GroupService = GroupService(groupRepository)

    @BeforeEach
    fun setup() {
        clearMocks(groupRepository)
    }

    @Nested
    inner class `when attempt to find a group` {
        @Test
        fun `should retrieve a group`() {
            val groupEntity = fixture<GroupEntity>()
            every { groupRepository.findByIdOrNull(groupEntity.id) } returns groupEntity
            val result = groupService.find(groupEntity.id!!)
            assertEquals(groupEntity, result)
        }

        @Test
        fun `should throw DocumentNotFound exception if group id is not found`() {
            every { groupRepository.findByIdOrNull(any()) } returns null
            assertThrows<DocumentNotFoundException> {
                groupService.find("1")
            }
        }
    }

    @Nested
    inner class `when attempt to create a group` {
        @Test
        fun `should store a group`() {
            val dto = fixture<GroupDto>()
            val entity = fixture<GroupEntity>()
            val slot = slot<GroupEntity>()

            every { groupRepository.save(any()) } returns entity
            groupService.create(dto)

            verify(exactly = 1) { groupRepository.save(capture(slot)) }
            assertEquals(slot.captured.groupName, dto.groupName)
        }
    }

    @Nested
    inner class `when attempt to update a group` {
        @Test
        fun `should upsert group`() {
            val upsertProperties = mapOf(
                "groupName" to fixture<String>()
            )
            val entityToUpdate = fixture<GroupEntity>()
            val slot = slot<GroupEntity>()

            every { groupRepository.findByIdOrNull(any()) } returns entityToUpdate
            every { groupRepository.save(any()) } returns entityToUpdate

            groupService.update(entityToUpdate.id!!, upsertProperties)

            verify(exactly = 1) { groupRepository.save(capture(slot)) }
            assertEquals(slot.captured.groupName, upsertProperties["groupName"])
        }

        @Test
        fun `should throw DocumentNotFound exception if group id is not found`() {
            every { groupRepository.findByIdOrNull(any()) } returns null

            assertThrows<DocumentNotFoundException> {
                groupService.update("1", emptyMap())
            }
        }
    }

    @Nested
    inner class `when attempt to delete a group`() {
        @Test
        fun `should delete group`() {
            val entity = fixture<GroupEntity>()

            every { groupRepository.findByIdOrNull(entity.id!!) } returns entity
            every { groupRepository.deleteById(entity.id!!) } just Runs

            groupService.delete(entity.id!!)

            verify(exactly = 1) { groupRepository.deleteById(entity.id!!) }
        }

        @Test
        fun `should throw DocumentNotFound exception if group id is not found`() {
            every { groupRepository.findByIdOrNull(any()) } returns null

            assertThrows<DocumentNotFoundException> {
                groupService.delete("1")
            }
        }
    }
}