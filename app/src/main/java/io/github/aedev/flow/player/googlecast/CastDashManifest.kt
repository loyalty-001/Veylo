package io.github.aedev.flow.player.googlecast

import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/** Combines the extractor's complete video/audio MPDs without inventing segment or byte ranges. */
internal object CastDashManifest {
    fun combine(
        videoManifest: String,
        audioManifest: String,
        relayUrl: (String, String) -> String,
    ): String {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val builder = factory.newDocumentBuilder()
        val video = builder.parse(InputSource(StringReader(videoManifest)))
        val audio = builder.parse(InputSource(StringReader(audioManifest)))
        val period = video.getElementsByTagNameNS("*", "Period").item(0) ?: error("Missing video period")
        val audioSets = audio.getElementsByTagNameNS("*", "AdaptationSet")
        require(audioSets.length > 0) { "Missing audio adaptation set" }
        for (index in 0 until audioSets.length) {
            period.appendChild(video.importNode(audioSets.item(index), true))
        }
        val sets = video.getElementsByTagNameNS("*", "AdaptationSet")
        for (index in 0 until sets.length) {
            val set = sets.item(index) as Element
            set.setAttribute("id", index.toString())
            val representations = set.getElementsByTagNameNS("*", "Representation")
            for (r in 0 until representations.length) {
                val representation = representations.item(r) as Element
                val mime = representation.getAttribute("mimeType").ifBlank { set.getAttribute("mimeType") }
                require(mime == "video/mp4" || mime == "audio/mp4") { "Unsupported Cast container" }
                val bases = representation.getElementsByTagNameNS("*", "BaseURL")
                require(bases.length > 0) { "Missing stream URL" }
                for (b in 0 until bases.length) {
                    val base = bases.item(b)
                    base.textContent = relayUrl(base.textContent.trim(), mime)
                }
            }
        }
        return StringWriter()
            .also { output ->
                TransformerFactory.newInstance().newTransformer().transform(DOMSource(video), StreamResult(output))
            }.toString()
    }
}
