package com.pfitzer.tootsie.presentation.components.status

import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.util.ShareUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    status: Status,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.share_status)) },
                leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                modifier = Modifier.clickable(role = Role.Button) {
                    ShareUtils.shareStatus(context, status.url, status.content)
                    onDismissRequest()
                }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.share_copy_link)) },
                leadingContent = { Icon(Icons.Default.Link, contentDescription = null) },
                modifier = Modifier.clickable(role = Role.Button) {
                    ShareUtils.copyLinkToClipboard(context, status.url)
                    onDismissRequest()
                }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.share_copy_text)) },
                leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                modifier = Modifier.clickable(role = Role.Button) {
                    ShareUtils.copyTextToClipboard(context, status.content)
                    onDismissRequest()
                }
            )
        }
    }
}
