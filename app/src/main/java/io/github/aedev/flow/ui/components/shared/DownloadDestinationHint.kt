package io.github.aedev.flow.ui.components.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import io.github.aedev.flow.R
import io.github.aedev.flow.data.local.PlayerPreferences
import io.github.aedev.flow.data.video.storage.DownloadLocation

@Composable
internal fun DownloadDestinationHint(
    preferences: PlayerPreferences,
    audioOnly: Boolean = false,
) {
    val video by preferences.downloadLocation.collectAsState(initial = DownloadLocation.DEFAULT)
    val music by preferences.musicDownloadLocation.collectAsState(initial = DownloadLocation.DEFAULT)
    val location = DownloadLocation.forDownload(audioOnly, video, music)
    val folder =
        location.path
            ?: if (location.treeUri !=
                null
            ) {
                stringResource(R.string.download_picked_folder)
            } else {
                stringResource(R.string.download_default_location)
            }
    Column {
        Text(stringResource(R.string.download_location_hint, folder), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.download_queue_hint), style = MaterialTheme.typography.bodySmall)
    }
}
