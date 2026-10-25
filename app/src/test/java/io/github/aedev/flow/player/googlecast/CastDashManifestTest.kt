package io.github.aedev.flow.player.googlecast

import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.data.shorts.ShortsDashManifest
import io.github.aedev.flow.innertube.models.response.PlayerResponse.StreamingData.Format
import io.github.aedev.flow.innertube.models.response.PlayerResponse.StreamingData.Format.Range
import org.junit.Test
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

class CastDashManifestTest {
    private fun mpd(
        mime: String,
        id: String,
        url: String,
    ) = """
        <MPD xmlns="urn:mpeg:dash:schema:mpd:2011" type="static" mediaPresentationDuration="PT60S">
          <Period><AdaptationSet id="0" mimeType="$mime"><Representation id="$id">
            <BaseURL>$url</BaseURL><SegmentBase indexRange="100-200"><Initialization range="0-99"/></SegmentBase>
          </Representation></AdaptationSet></Period>
        </MPD>
        """.trimIndent()

    @Test
    fun `receiver manifest contains video and audio with original byte ranges and relayed URLs`() {
        val seen = mutableListOf<Pair<String, String>>()
        val xml =
            CastDashManifest.combine(
                mpd("video/mp4", "137", "https://video.example/play?v=1&amp;n=2"),
                mpd("audio/mp4", "140", "https://audio.example/play"),
            ) { url, mime ->
                seen += url to mime
                "http://192.168.1.10:1234/s/${seen.size}.mp4"
            }
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val doc = factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
        assertThat(doc.getElementsByTagNameNS("*", "AdaptationSet").length).isEqualTo(2)
        assertThat(doc.getElementsByTagNameNS("*", "SegmentBase").length).isEqualTo(2)
        assertThat(xml).contains("indexRange=\"100-200\"")
        assertThat(xml).contains("range=\"0-99\"")
        assertThat(xml).doesNotContain("https://")
        assertThat(seen)
            .containsExactly(
                "https://video.example/play?v=1&n=2" to "video/mp4",
                "https://audio.example/play" to "audio/mp4",
            ).inOrder()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unsupported containers are rejected rather than sent as a silent or broken MP4`() {
        CastDashManifest.combine(
            mpd("video/webm", "248", "https://video.example"),
            mpd("audio/mp4", "140", "https://audio.example"),
        ) { url, _ -> url }
    }

    private val url = "https://rr3---sn-example.googlevideo.com/videoplayback?expire=1&itag=137"

    private fun videoFormat(
        itag: Int = 137,
        initRange: Range? = Range(start = "0", end = "740"),
        indexRange: Range? = Range(start = "741", end = "1560"),
        contentLength: Long? = 4_812_345L,
    ) = Format(
        itag = itag,
        url = url,
        mimeType = "video/mp4; codecs=\"avc1.640028\"",
        bitrate = 2_500_000,
        width = 1080,
        height = 1920,
        contentLength = contentLength,
        quality = "hd1080",
        fps = 30,
        qualityLabel = "1080p",
        averageBitrate = 2_100_000,
        audioQuality = null,
        approxDurationMs = "28000",
        audioSampleRate = null,
        audioChannels = null,
        loudnessDb = null,
        lastModified = 1_700_000_000_000_000L,
        signatureCipher = null,
        initRange = initRange,
        indexRange = indexRange,
    )

    @Test
    fun `real extractor manifests combine into one ranged video and audio presentation`() {
        val video = videoFormat()
        val audio =
            video.copy(
                itag = 140,
                width = null,
                height = null,
                mimeType = "audio/mp4; codecs=\"mp4a.40.2\"",
                audioSampleRate = 44100,
                audioChannels = 2,
            )
        val videoMpd = requireNotNull(ShortsDashManifest.forVideo(video, url, 28_000))
        val audioMpd = requireNotNull(ShortsDashManifest.forAudio(audio, url, 28_000))
        val xml =
            CastDashManifest.combine(
                videoMpd,
                audioMpd,
            ) { _, mime -> "http://192.168.1.10/s/${if (mime.startsWith("audio")) "audio" else "video"}.mp4" }
        assertThat(xml).contains("audio/mp4")
        assertThat(xml).contains("video/mp4")
        assertThat(xml).contains("indexRange")
        assertThat(xml).doesNotContain("googlevideo.com")
    }
}
