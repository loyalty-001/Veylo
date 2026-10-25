package io.github.aedev.flow.data.operations

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OperationsPolicyTest {
    private val note = AppAnnouncement("test", 1, "Title", "Message", "popup", 100, 200, 22, 30)

    @Test
    fun `announcement targeting excludes expired future dismissed and unsupported versions`() {
        assertThat(note.isEligible(22, 100, emptySet())).isTrue()
        assertThat(note.isEligible(21, 100, emptySet())).isFalse()
        assertThat(note.isEligible(22, 99, emptySet())).isFalse()
        assertThat(note.isEligible(22, 200, emptySet())).isFalse()
        assertThat(note.isEligible(22, 150, setOf(note.dismissalKey))).isFalse()
        assertThat(note.copy(revision = 2).isEligible(22, 150, setOf(note.dismissalKey))).isTrue()
    }

    @Test
    fun `actions accept only Veylo HTTPS links`() {
        assertThat(allowedAnnouncementLink("https://veylo-lra9.onrender.com/")).isTrue()
        assertThat(allowedAnnouncementLink("https://github.com/loyalty-001/Veylo/releases")).isTrue()
        for (link in listOf(
            "javascript:alert(1)",
            "http://veylo-lra9.onrender.com",
            "https://attacker.example/",
            "https://github.com/another/repo",
            "https://user:pass@veylo-lra9.onrender.com/",
        )) {
            assertThat(allowedAnnouncementLink(link)).isFalse()
        }
    }
}
