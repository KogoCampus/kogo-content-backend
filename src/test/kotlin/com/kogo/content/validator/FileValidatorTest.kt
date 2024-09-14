package com.kogo.content.validator
/*
class FileValidatorTest {

    private lateinit var validator: FileValidator
    private lateinit var context: ConstraintValidatorContext

    @BeforeEach
    fun setup() {
        validator = FileValidator()
        context = mockk(relaxed = true) // Mocked context (relaxed so it doesn't need stubbing)
    }

    @Test
    fun `test valid image file`() {
        val validFileAnnotation = mockk<ValidFile>()
        every { validFileAnnotation.sizeLimit } returns 128000000 // 128MB
        every { validFileAnnotation.acceptedMediaTypes } returns arrayOf("image/png", "image/jpeg")

        validator.initialize(validFileAnnotation)

        // Mock MultipartFile
        val mockFile = mockk<MultipartFile>()
        every { mockFile.size } returns 5000000 // 5MB
        every { mockFile.contentType } returns "image/png"

        assertTrue(validator.isValid(listOf(mockFile), context))
    }

    @Test
    fun `test invalid image file size`() {
        val validFileAnnotation = mockk<ValidFile>()
        every { validFileAnnotation.sizeLimit } returns 1000000 // 1MB size limit
        every { validFileAnnotation.acceptedMediaTypes } returns arrayOf("image/png", "image/jpeg")

        validator.initialize(validFileAnnotation)

        val mockFile = mockk<MultipartFile>()
        every { mockFile.size } returns 5000000 // 5MB (exceeds 1MB)
        every { mockFile.contentType } returns "image/png"

        assertFalse(validator.isValid(listOf(mockFile), context))
    }

    @Test
    fun `test invalid media type`() {
        val validFileAnnotation = mockk<ValidFile>()
        every { validFileAnnotation.sizeLimit } returns 128000000 // 128MB
        every { validFileAnnotation.acceptedMediaTypes } returns arrayOf("image/png", "image/jpeg")

        validator.initialize(validFileAnnotation)

        val mockFile = mockk<MultipartFile>()
        every { mockFile.size } returns 5000000 // 5MB
        every { mockFile.contentType } returns "application/pdf" // Invalid media type

        assertFalse(validator.isValid(listOf(mockFile), context))
    }

    @Test
    fun `test null or empty file list`() {
        val validFileAnnotation = mockk<ValidFile>()
        every { validFileAnnotation.sizeLimit } returns 128000000 // 128MB
        every { validFileAnnotation.acceptedMediaTypes } returns arrayOf("image/png", "image/jpeg")

        validator.initialize(validFileAnnotation)

        // Test with null list
        assertTrue(validator.isValid(null, context))

        // Test with an empty list
        assertTrue(validator.isValid(emptyList(), context))
    }
}
*/
