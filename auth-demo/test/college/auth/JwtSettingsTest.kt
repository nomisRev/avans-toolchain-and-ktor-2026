package college.auth

import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertFailsWith

class JwtSettingsTest {
    @Test
    fun `reject absent malformed and short signing keys`() {
        assertFailsWith<IllegalArgumentException> { JwtSettings.fromEnvironment(emptyMap()) }
        assertFailsWith<IllegalStateException> {
            JwtSettings.fromEnvironment(mapOf("JWT_SECRET_BASE64" to "not base64!"))
        }
        assertFailsWith<IllegalArgumentException> {
            JwtSettings.fromEnvironment(
                mapOf("JWT_SECRET_BASE64" to Base64.getEncoder().encodeToString(ByteArray(8)))
            )
        }
    }
}
