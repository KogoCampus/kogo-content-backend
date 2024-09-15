package com.kogo.content.validator

import io.mockk.MockKAnnotations
import io.mockk.mockk
import jakarta.validation.ConstraintValidatorContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TagValidatorTest {
    private lateinit var tagValidator: TagValidator
    private lateinit var tagListValidator: TagListValidator
    private lateinit var context: ConstraintValidatorContext

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        tagValidator = TagValidator()
        tagListValidator = TagListValidator()
        context = mockk(relaxed = true)
    }

    @Test
    fun `should return true for valid tag`() {
        val validTag = "ValidTag123"
        assertTrue { tagValidator.isValid(validTag, context) }
    }

    @Test
    fun `should return false for invalid tag with special characters`() {
        val invalidTag = "Invalid@Tag!"
        assertFalse { tagValidator.isValid(invalidTag, context) }
    }

    @Test
    fun `should return false for null or blank tag`() {
        assertFalse { tagValidator.isValid(null, context) }
        assertFalse { tagValidator.isValid("", context) }
    }

    @Test
    fun `should return true for valid tag list`() {
        val validTags = listOf("Tag1", "Tag2", "Tag3")
        assertTrue { tagListValidator.isValid(validTags, context) }
    }

    @Test
    fun `should return false for invalid tag list with special characters`() {
        val invalidTags = listOf("Tag1", "Invalid@Tag", "Tag3")
        assertFalse { tagListValidator.isValid(invalidTags, context) }
    }

    @Test
    fun `should return true for empty or null tag list`() {
        assertTrue { tagListValidator.isValid(emptyList(), context) }
        assertTrue { tagListValidator.isValid(null, context) }
    }
}
