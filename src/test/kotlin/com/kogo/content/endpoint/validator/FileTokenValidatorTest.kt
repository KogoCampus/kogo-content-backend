import com.kogo.content.endpoint.validator.FileTokenValidator
import com.kogo.content.endpoint.validator.FileTokenListValidator
import com.kogo.content.endpoint.validator.ValidFileToken
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.validation.ConstraintValidatorContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileTokenValidatorTest {

    private lateinit var fileTokenValidator: FileTokenValidator
    private lateinit var fileTokenListValidator: FileTokenListValidator
    private lateinit var context: ConstraintValidatorContext
    private val secretKey = "test-secret-key"

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        fileTokenValidator = FileTokenValidator()
        fileTokenListValidator = FileTokenListValidator()
        injectSecretKey(fileTokenValidator)
        injectSecretKey(fileTokenListValidator)
        context = mockk(relaxed = true)
    }

    private fun injectSecretKey(target: Any) {
        val field = target.javaClass.getDeclaredField("secretKey")
        field.isAccessible = true
        field.set(target, secretKey)
    }

    private fun generateTestToken(fileId: String, contentType: String, expiresInMinutes: Long = 20): String {
        val algorithm = Algorithm.HMAC256(secretKey)
        return JWT.create()
            .withSubject("file_token")
            .withClaim("file_id", fileId)
            .withClaim("contentType", contentType)
            .withExpiresAt(java.util.Date(System.currentTimeMillis() + expiresInMinutes * 60 * 1000))
            .sign(algorithm)
    }

    @Test
    fun `should return true for valid file token`() {
        val validToken = generateTestToken("valid-file-id", MediaType.IMAGE_JPEG_VALUE)

        fileTokenValidator.initialize(ValidFileToken(acceptedMediaTypes = arrayOf(MediaType.IMAGE_JPEG_VALUE)))

        assertTrue { fileTokenValidator.isValid(validToken, context) }
    }

    @Test
    fun `should return false for invalid file token`() {
        val invalidToken = "invalid.token.string"

        fileTokenValidator.initialize(ValidFileToken(acceptedMediaTypes = arrayOf(MediaType.IMAGE_JPEG_VALUE)))

        assertFalse { fileTokenValidator.isValid(invalidToken, context) }
        verify { context.buildConstraintViolationWithTemplate("Invalid or expired file token").addConstraintViolation() }
    }

    @Test
    fun `should return false for expired file token`() {
        val expiredToken = generateTestToken("expired-file-id", MediaType.IMAGE_JPEG_VALUE, expiresInMinutes = -10)

        fileTokenValidator.initialize(ValidFileToken(acceptedMediaTypes = arrayOf(MediaType.IMAGE_JPEG_VALUE)))

        assertFalse { fileTokenValidator.isValid(expiredToken, context) }
        verify { context.buildConstraintViolationWithTemplate("Invalid or expired file token").addConstraintViolation() }
    }

    @Test
    fun `should return false for unsupported content type`() {
        val unsupportedToken = generateTestToken("file-id", MediaType.APPLICATION_JSON_VALUE)

        fileTokenValidator.initialize(ValidFileToken(acceptedMediaTypes = arrayOf(MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE)))

        assertFalse { fileTokenValidator.isValid(unsupportedToken, context) }
        verify { context.buildConstraintViolationWithTemplate("Unsupported media type: application/json").addConstraintViolation() }
    }

    @Test
    fun `should return true for valid list of file tokens`() {
        val validToken1 = generateTestToken("file1", MediaType.IMAGE_JPEG_VALUE)
        val validToken2 = generateTestToken("file2", MediaType.IMAGE_PNG_VALUE)

        fileTokenListValidator.initialize(ValidFileToken(acceptedMediaTypes = arrayOf(MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE)))

        assertTrue { fileTokenListValidator.isValid(listOf(validToken1, validToken2), context) }
    }

    @Test
    fun `should return false if at least one file token is invalid in the list`() {
        val validToken = generateTestToken("valid-file-id", MediaType.IMAGE_JPEG_VALUE)
        val invalidToken = "invalid.token.string"

        fileTokenListValidator.initialize(ValidFileToken(acceptedMediaTypes = arrayOf(MediaType.IMAGE_JPEG_VALUE)))

        assertFalse { fileTokenListValidator.isValid(listOf(validToken, invalidToken), context) }
    }

    @Test
    fun `should return false if at least one file token has an unsupported media type`() {
        val validToken = generateTestToken("valid-file-id", MediaType.IMAGE_JPEG_VALUE)
        val unsupportedToken = generateTestToken("unsupported-file-id", MediaType.APPLICATION_JSON_VALUE)

        fileTokenListValidator.initialize(ValidFileToken(acceptedMediaTypes = arrayOf(MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE)))

        assertFalse { fileTokenListValidator.isValid(listOf(validToken, unsupportedToken), context) }
        verify { context.buildConstraintViolationWithTemplate("Unsupported media type: application/json").addConstraintViolation() }
    }

    @Test
    fun `should return true for empty or null file token list`() {
        assertTrue { fileTokenListValidator.isValid(emptyList(), context) }
        assertTrue { fileTokenListValidator.isValid(null, context) }
    }
}
