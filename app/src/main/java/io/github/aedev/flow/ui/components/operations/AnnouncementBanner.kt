package io.github.aedev.flow.ui.components.operations

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.R
import io.github.aedev.flow.data.operations.AppAnnouncement
import io.github.aedev.flow.data.operations.allowedAnnouncementLink

@Composable
internal fun AnnouncementBanner(
    note: AppAnnouncement,
    onDismiss: () -> Unit,
    onAction: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(note.title, style = MaterialTheme.typography.titleMedium)
            Text(note.body, style = MaterialTheme.typography.bodyMedium)
            Row {
                if (allowedAnnouncementLink(note.actionUrl)) {
                    TextButton(onClick = onAction) { Text(note.actionLabel) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.operations_dismiss)) }
            }
        }
    }
}
