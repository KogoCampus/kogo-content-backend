package com.kogo.content.service

//class GroupServiceTest {
//
//    private val groupRepository : GroupRepository = mockk()
//
//    private val fileHandler : LocalStorageFileHandlerService = mockk()
//
//    private val groupService : GroupService = GroupService(groupRepository, fileHandler)
//
//    @BeforeEach
//    fun setup() {
//        clearMocks(groupRepository)
//        every { groupRepository.findByGroupName(any()) } returns null
//    }
//
//    @Nested
//    inner class `when find a group` {
//        @Test
//        fun `should retrieve a group`() {
//            val groupEntity = fixture<GroupEntity>()
//            every { groupRepository.findByIdOrNull(groupEntity.id) } returns groupEntity
//            val result = groupService.find(groupEntity.id!!)
//            assertEquals(groupEntity, result)
//        }
//
//        @Test
//        fun `should throw DocumentNotFound exception if group id is not found`() {
//            every { groupRepository.findByIdOrNull(any()) } returns null
//            assertThrows<DocumentNotFoundException> {
//                groupService.find("1")
//            }
//        }
//    }
//
//    @Nested
//    inner class `when create a group` {
//        @Test
//        fun `should store a group`() {
//            val dto = fixture<GroupDto> {
//                mapOf(
//                    "profileImage" to null,
//                    "tags" to "tag1, tag2, tag3"
//                )
//            }
//            val slot = slot<GroupEntity>()
//            every { groupRepository.save(any()) } returns GroupEntity()
//            groupService.create(dto)
//
//            verify(exactly = 1) { groupRepository.save(capture(slot)) }
//            assertEquals(slot.captured.groupName, dto.groupName)
//            assertEquals(slot.captured.description, dto.description)
//            assertEquals(slot.captured.tags, listOf("tag1", "tag2", "tag3"))
//        }
//
//        @Test
//        fun `should process the profile image parameter`() {
//            val mockMultipartFile = MockMultipartFile("data", "image.jpeg", "image/jpeg", "some image".toByteArray())
//            val dto = fixture<GroupDto> {
//                mapOf(
//                    "profileImage" to mockMultipartFile,
//                )
//            }
//            val slot = slot<GroupEntity>()
//            val mockImageStoreUrl = "mockUrl"
//            every { groupRepository.save(any()) } returns GroupEntity()
//            every { fileHandler.store(mockMultipartFile) } returns FileStoreResult(
//                url = mockImageStoreUrl,
//                fileName = "",
//                metadata = FileMetadata(
//                    originalFileName = mockMultipartFile.originalFilename,
//                    contentType = mockMultipartFile.contentType,
//                    size = mockMultipartFile.size,
//                )
//            )
//            groupService.create(dto)
//
//            verify(exactly = 1) { groupRepository.save(capture(slot)) }
//            assertEquals(slot.captured.profileImage!!.imageUrl, mockImageStoreUrl)
//            assertEquals(slot.captured.profileImage!!.metadata.originalFileName, mockMultipartFile.originalFilename)
//            assertEquals(slot.captured.profileImage!!.metadata.contentType, mockMultipartFile.contentType)
//            assertEquals(slot.captured.profileImage!!.metadata.size, mockMultipartFile.size)
//        }
//
//        @Test
//        fun `should throw UnsupportedMediaTypeException if given file is not an image file`() {
//            val mockMultipartFile = MockMultipartFile("data", "sample.pdf", "application/mdf", "some pdf".toByteArray())
//            val dto = fixture<GroupDto> { mapOf(
//                    "profileImage" to mockMultipartFile,
//                )}
//            every { groupRepository.save(any()) } returns GroupEntity()
//            assertThrows<UnsupportedMediaTypeException> { groupService.create(dto) }
//        }
//
//        @Test
//        fun `should throw IllegalArgumentException if group name is not unique`() {
//            val dto = fixture<GroupDto> { mapOf(
//                "groupName" to "dummy",
//                "profileImage" to null,
//            ) }
//            every { groupRepository.findByGroupName("dummy") } returns GroupEntity()
//            assertThrows<IllegalArgumentException> { groupService.create(dto) }
//        }
//    }
//
//    @Nested
//    inner class `when update a group` {
//        @Test
//        fun `should update group parameters`() {
//            val upsertProperties = mapOf(
//                "groupName" to "testGroup",
//                "tags" to "tag1, tag2, tag3"
//            )
//            val entityToUpdate = fixture<GroupEntity>()
//            val slot = slot<GroupEntity>()
//            every { groupRepository.findByIdOrNull(any()) } returns entityToUpdate
//            every { groupRepository.save(any()) } returns GroupEntity()
//
//            groupService.update(entityToUpdate.id!!, upsertProperties)
//            verify(exactly = 1) { groupRepository.save(capture(slot)) }
//            assertEquals(slot.captured.id, entityToUpdate.id)
//            assertEquals(slot.captured.groupName, upsertProperties["groupName"])
//            assertEquals(slot.captured.tags, listOf("tag1", "tag2", "tag3"))
//        }
//
//        @Test
//        fun `should omit empty tags from group`() {
//            val upsertProperties = mapOf(
//                "tags" to "tag1,tag2,,,,"
//            )
//            val entityToUpdate = fixture<GroupEntity>()
//            val slot = slot<GroupEntity>()
//            every { groupRepository.findByIdOrNull(any()) } returns entityToUpdate
//            every { groupRepository.save(any()) } returns GroupEntity()
//
//            groupService.update(entityToUpdate.id!!, upsertProperties)
//            verify(exactly = 1) { groupRepository.save(capture(slot)) }
//            assertEquals(slot.captured.tags, listOf("tag1", "tag2"))
//        }
//
//        @Test
//        fun `should throw DocumentNotFound exception if group id is not found`() {
//            every { groupRepository.findByIdOrNull(any()) } returns null
//            assertThrows<DocumentNotFoundException> {
//                groupService.update("1", emptyMap())
//            }
//        }
//
//        @Test
//        fun `should process the profile image parameter`() {
//            val mockMultipartFile = MockMultipartFile("data", "image.jpeg", "image/jpeg", "some image".toByteArray())
//            val upsertProperties = mapOf(
//                "profileImage" to mockMultipartFile,
//            )
//            val entityToUpdate = fixture<GroupEntity>()
//            val slot = slot<GroupEntity>()
//            val mockImageStoreUrl = "mockUrl"
//            every { groupRepository.findByIdOrNull(any()) } returns entityToUpdate
//            every { groupRepository.save(any()) } returns GroupEntity()
//            every { fileHandler.store(mockMultipartFile) } returns FileStoreResult(
//                url = mockImageStoreUrl,
//                fileName = "",
//                metadata = FileMetadata(
//                    originalFileName = mockMultipartFile.originalFilename,
//                    contentType = mockMultipartFile.contentType,
//                    size = mockMultipartFile.size,
//                )
//            )
//
//            groupService.update(entityToUpdate.id!!, upsertProperties)
//            verify(exactly = 1) { groupRepository.save(capture(slot)) }
//            assertEquals(slot.captured.id, entityToUpdate.id)
//            assertEquals(slot.captured.profileImage!!.imageUrl, mockImageStoreUrl)
//            assertEquals(slot.captured.profileImage!!.metadata.originalFileName, mockMultipartFile.originalFilename)
//            assertEquals(slot.captured.profileImage!!.metadata.contentType, mockMultipartFile.contentType)
//            assertEquals(slot.captured.profileImage!!.metadata.size, mockMultipartFile.size)
//        }
//
//        @Test
//        fun `should throw UnsupportedMediaTypeException if given file is not an image file`() {
//            val mockMultipartFile = MockMultipartFile("data", "sample.pdf", "application/mdf", "some pdf".toByteArray())
//            val upsertProperties = mapOf(
//                "profileImage" to mockMultipartFile,
//            )
//            val entityToUpdate = fixture<GroupEntity>()
//            every { groupRepository.findByIdOrNull(any()) } returns entityToUpdate
//            every { groupRepository.save(any()) } returns GroupEntity()
//            assertThrows<UnsupportedMediaTypeException> { groupService.update(entityToUpdate.id!!, upsertProperties) }
//        }
//
//        @Test
//        fun `should throw IllegalArgumentException if group name is not unique`() {
//            val upsertProperties = mapOf(
//                "groupName" to "dummy",
//                "profileImage" to null,
//            )
//            val entityToUpdate = fixture<GroupEntity>()
//            every { groupRepository.findByIdOrNull(any()) } returns entityToUpdate
//            every { groupRepository.findByGroupName("dummy") } returns GroupEntity()
//
//            assertThrows<IllegalArgumentException> { groupService.update(entityToUpdate.id!!, upsertProperties) }
//        }
//    }
//
//    @Nested
//    inner class `when delete a group`() {
//        @Test
//        fun `should delete group`() {
//            val entity = fixture<GroupEntity>()
//
//            every { groupRepository.findByIdOrNull(entity.id!!) } returns entity
//            every { groupRepository.deleteById(entity.id!!) } just Runs
//            groupService.delete(entity.id!!)
//            verify(exactly = 1) { groupRepository.deleteById(entity.id!!) }
//        }
//
//        @Test
//        fun `should throw DocumentNotFound exception if group id is not found`() {
//            every { groupRepository.findByIdOrNull(any()) } returns null
//            assertThrows<DocumentNotFoundException> {
//                groupService.delete("1")
//            }
//        }
//    }
//}