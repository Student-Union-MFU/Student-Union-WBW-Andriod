package th.ac.mfu.su.wbw.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The base card shows a check-in count the server does not send yet, so the only thing that
 * can be tested today is the half that has to be right when it starts: that a payload
 * without the field decodes to null rather than to zero, and that one with it decodes to
 * the number — no app change on the day the endpoint grows it.
 */
class CheckpointDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `absent checkin_count is null, not zero`() {
        val cp = json.decodeFromString<ParticipantCheckpoint>(
            """{"id":5,"sequence":5,"name":"ลานสวนสน","name_en":"Pine Grove Plaza"}""",
        )
        // Zero would be the card claiming nobody has reached the base, which is a different
        // and much stronger statement than the server not having told us.
        assertNull(cp.checkinCount)
    }

    @Test
    fun `checkin_count decodes when the server sends it`() {
        val cp = json.decodeFromString<ParticipantCheckpoint>(
            """{"id":5,"name":"ลานสวนสน","checkin_count":42}""",
        )
        assertEquals(42, cp.checkinCount)
    }

    @Test
    fun `zero is carried through as zero`() {
        val cp = json.decodeFromString<ParticipantCheckpoint>(
            """{"id":5,"name":"ลานสวนสน","checkin_count":0}""",
        )
        assertEquals(0, cp.checkinCount)
    }
}
