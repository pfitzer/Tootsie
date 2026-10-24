package com.pfitzer.tootsie.presentation.screens.compose

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Drafts
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.Quote
import com.pfitzer.tootsie.domain.model.QuoteState
import com.pfitzer.tootsie.domain.util.AutocompleteToken
import com.pfitzer.tootsie.presentation.components.status.QuotedStatusCard
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfitzer.tootsie.domain.model.MediaAttachment
import com.pfitzer.tootsie.presentation.components.common.AvatarImage
import com.pfitzer.tootsie.presentation.components.common.ErrorMessage
import com.pfitzer.tootsie.presentation.components.compose.AltTextDialog
import com.pfitzer.tootsie.presentation.components.compose.EmojiPickerDialog
import com.pfitzer.tootsie.presentation.components.compose.MediaPreviewGrid
import com.pfitzer.tootsie.presentation.components.compose.ComposePollComponent
import com.pfitzer.tootsie.util.toFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Compose Screen - for creating new posts.
 *
 * @param onNavigateBack Callback to navigate back
 * @param onNavigateToDrafts Callback to navigate to drafts screen
 * @param viewModel ViewModel for managing compose state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDrafts: () -> Unit = {},
    viewModel: ComposeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Text with cursor position; the ViewModel only keeps the text
    var contentValue by remember { mutableStateOf(TextFieldValue(uiState.content)) }
    // Content set by the ViewModel (e.g. a loaded draft) replaces the field, cursor at the end
    LaunchedEffect(uiState.content) {
        if (uiState.content != contentValue.text) {
            contentValue = TextFieldValue(uiState.content, TextRange(uiState.content.length))
        }
    }

    fun updateContent(value: TextFieldValue) {
        contentValue = value
        viewModel.onAction(ComposeAction.UpdateContent(value.text))
        val token = if (value.selection.collapsed) {
            AutocompleteToken.find(value.text, value.selection.start)
        } else {
            null
        }
        viewModel.onAction(ComposeAction.UpdateAutocomplete(token))
    }

    // Replaces [start, end) with [replacement] and puts the cursor after it
    fun replaceText(start: Int, end: Int, replacement: String) {
        val text = contentValue.text
        val newText = text.substring(0, start) + replacement + text.substring(end)
        updateContent(TextFieldValue(newText, TextRange(start + replacement.length)))
    }

    val contentFocusRequester = remember { FocusRequester() }

    // Dialog state
    var showAltTextDialog by remember { mutableStateOf(false) }
    var selectedMediaForAltText by remember { mutableStateOf<MediaAttachment?>(null) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                // Convert URI to File in background thread
                val file = withContext(Dispatchers.IO) {
                    it.toFile(context)
                }
                file?.let { f ->
                    viewModel.onAction(ComposeAction.AddMedia(f))
                }
            }
        }
    }

    // Start typing right away
    LaunchedEffect(Unit) {
        contentFocusRequester.requestFocus()
    }

    // Navigate back when the post is sent, or once the draft is saved after leaving
    LaunchedEffect(uiState.isPosted, uiState.isExited) {
        if (uiState.isPosted || uiState.isExited) {
            onNavigateBack()
        }
    }

    // System back saves the draft first, like the back arrow
    BackHandler(enabled = !uiState.isPosted && !uiState.isExited) {
        viewModel.onAction(ComposeAction.Exit)
    }

    // Alt text dialog
    if (showAltTextDialog && selectedMediaForAltText != null) {
        AltTextDialog(
            media = selectedMediaForAltText!!,
            onDismiss = {
                showAltTextDialog = false
                selectedMediaForAltText = null
            },
            onSave = { attachmentId, description ->
                viewModel.onAction(ComposeAction.UpdateMediaDescription(attachmentId, description))
            }
        )
    }

    // Emoji picker dialog: inserts at the cursor
    if (showEmojiPicker) {
        EmojiPickerDialog(
            emojis = uiState.customEmojis,
            onEmojiSelected = { shortcode ->
                val selection = contentValue.selection
                replaceText(selection.min, selection.max, ":$shortcode: ")
            },
            onDismiss = { showEmojiPicker = false },
            isLoading = uiState.isLoadingEmojis
        )
    }

    if (showLanguagePicker) {
        LanguagePickerDialog(
            selected = uiState.language,
            onSelect = {
                viewModel.onAction(ComposeAction.SelectLanguage(it))
                showLanguagePicker = false
            },
            onDismiss = { showLanguagePicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(
                        when {
                            uiState.isReply -> R.string.compose_title_reply
                            uiState.isQuote -> R.string.compose_title_quote
                            else -> R.string.compose
                        }
                    ))
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onAction(ComposeAction.Exit) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.onAction(ComposeAction.Post) },
                        enabled = uiState.canPost
                    ) {
                        Text(
                            stringResource(
                                if (uiState.isPosting) R.string.compose_button_posting
                                else R.string.compose_button_post
                            )
                        )
                    }
                    ComposeOverflowMenu(onNavigateToDrafts = onNavigateToDrafts)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                // Keeps the toolbar above the keyboard (edge-to-edge)
                .imePadding()
        ) {
            if (uiState.isPosting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Posting account and visibility
            ComposeHeader(
                account = uiState.account,
                accounts = uiState.accounts,
                canSwitchAccount = uiState.canSwitchAccount,
                onSwitchAccount = { viewModel.onAction(ComposeAction.SwitchAccount(it)) },
                visibility = uiState.visibility,
                onVisibilitySelected = { viewModel.onAction(ComposeAction.UpdateVisibility(it)) }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                // Reply context (if replying)
                uiState.replyToAccount?.let { account ->
                    if (uiState.isReply) {
                        Text(
                            text = stringResource(R.string.compose_replying_to, account),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                // Content warning (if enabled)
                if (uiState.showContentWarning) {
                    OutlinedTextField(
                        value = uiState.contentWarning,
                        onValueChange = {
                            viewModel.onAction(ComposeAction.UpdateContentWarning(it))
                        },
                        label = { Text(stringResource(R.string.content_warning)) },
                        placeholder = { Text(stringResource(R.string.compose_placeholder_warning)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        singleLine = true
                    )
                }

                // Main content field, without a box around it
                TextField(
                    value = contentValue,
                    onValueChange = ::updateContent,
                    placeholder = { Text(stringResource(R.string.compose_placeholder_content)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .focusRequester(contentFocusRequester)
                )

                // Preview of the quoted status
                uiState.quotedStatus?.let { quoted ->
                    Spacer(modifier = Modifier.height(8.dp))
                    QuotedStatusCard(
                        quote = Quote(state = QuoteState.ACCEPTED, quotedStatus = quoted),
                        onClick = null
                    )
                }

                // Media preview grid
                if (uiState.mediaAttachments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    MediaPreviewGrid(
                        mediaAttachments = uiState.mediaAttachments,
                        onRemoveMedia = { attachmentId ->
                            viewModel.onAction(ComposeAction.RemoveMedia(attachmentId))
                        },
                        onEditAltText = { media ->
                            selectedMediaForAltText = media
                            showAltTextDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    )
                }

                // Poll Creator
                uiState.pollConfig?.let { pollConfig ->
                    Spacer(modifier = Modifier.height(8.dp))
                    ComposePollComponent(
                        pollConfig = pollConfig,
                        onUpdateOption = { index, text ->
                            viewModel.onAction(ComposeAction.UpdatePollOption(index, text))
                        },
                        onAddOption = {
                            viewModel.onAction(ComposeAction.AddPollOption)
                        },
                        onRemoveOption = { index ->
                            viewModel.onAction(ComposeAction.RemovePollOption(index))
                        },
                        onUpdateExpiry = { expiresIn ->
                            viewModel.onAction(ComposeAction.UpdatePollExpiry(expiresIn))
                        },
                        onToggleMultipleChoice = {
                            viewModel.onAction(ComposeAction.TogglePollMultipleChoice)
                        },
                        onToggleHideTotals = {
                            viewModel.onAction(ComposeAction.TogglePollHideTotals)
                        },
                        onRemovePoll = {
                            viewModel.onAction(ComposeAction.RemovePoll)
                        }
                    )
                }

                // Upload progress indicator
                if (uiState.isUploadingMedia) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.uploading_media),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Error message
                uiState.errorMessage?.let { error ->
                    ErrorMessage(message = error, modifier = Modifier.padding(top = 8.dp))
                }
            }

            // Autocomplete suggestions for the word at the cursor
            if (uiState.suggestions.isNotEmpty()) {
                SuggestionList(
                    suggestions = uiState.suggestions,
                    onSelect = { suggestion ->
                        val selection = contentValue.selection
                        AutocompleteToken.find(contentValue.text, selection.start)?.let { token ->
                            replaceText(token.start, token.end, suggestion.replacement)
                        }
                    }
                )
            }

            HorizontalDivider()

            // Toolbar
            ComposeToolbar(
                uiState = uiState,
                onAttachMedia = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                onAddPoll = { viewModel.onAction(ComposeAction.AddPoll) },
                onToggleContentWarning = { viewModel.onAction(ComposeAction.ToggleContentWarning) },
                onEmoji = { showEmojiPicker = true },
                onLanguage = { showLanguagePicker = true }
            )
        }
    }
}

/**
 * Overflow menu of the top bar.
 */
@Composable
private fun ComposeOverflowMenu(onNavigateToDrafts: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.compose_more_options)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.drafts_title)) },
                leadingIcon = { Icon(Icons.Outlined.Drafts, contentDescription = null) },
                onClick = {
                    expanded = false
                    onNavigateToDrafts()
                }
            )
        }
    }
}

/**
 * Posting account (switchable for new posts with several accounts) and visibility.
 */
@Composable
private fun ComposeHeader(
    account: Account?,
    accounts: List<Account>,
    canSwitchAccount: Boolean,
    onSwitchAccount: (String) -> Unit,
    visibility: StatusVisibility,
    onVisibilitySelected: (StatusVisibility) -> Unit
) {
    var showAccounts by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(enabled = canSwitchAccount) { showAccounts = true }
            ) {
                if (account != null) {
                    AvatarImage(
                        url = account.avatar,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = account.displayName.ifBlank { account.username },
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "@${account.acct}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (canSwitchAccount) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = stringResource(R.string.compose_switch_account)
                        )
                    }
                }
            }
            DropdownMenu(expanded = showAccounts, onDismissRequest = { showAccounts = false }) {
                accounts.forEach { option ->
                    DropdownMenuItem(
                        text = { Text("@${option.acct}") },
                        leadingIcon = {
                            AsyncImage(
                                model = option.avatar,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        onClick = {
                            showAccounts = false
                            onSwitchAccount(option.id)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))
        VisibilityChip(selected = visibility, onSelected = onVisibilitySelected)
    }
}

/**
 * Visibility of the post as a chip with icon and a menu.
 */
@Composable
private fun VisibilityChip(
    selected: StatusVisibility,
    onSelected: (StatusVisibility) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(stringResource(selected.labelRes)) },
            leadingIcon = {
                Icon(
                    imageVector = selected.icon,
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize)
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize)
                )
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            StatusVisibility.entries.forEach { visibility ->
                DropdownMenuItem(
                    text = { Text(stringResource(visibility.labelRes)) },
                    leadingIcon = { Icon(visibility.icon, contentDescription = null) },
                    onClick = {
                        onSelected(visibility)
                        expanded = false
                    }
                )
            }
        }
    }
}

private val StatusVisibility.icon: ImageVector
    get() = when (this) {
        StatusVisibility.PUBLIC -> Icons.Default.Public
        StatusVisibility.UNLISTED -> Icons.Default.LockOpen
        StatusVisibility.PRIVATE -> Icons.Default.Lock
        StatusVisibility.DIRECT -> Icons.Default.AlternateEmail
    }

/**
 * Bottom toolbar: attachments, content warning, emoji and language on the left,
 * the character counter on the right.
 */
@Composable
private fun ComposeToolbar(
    uiState: ComposeUiState,
    onAttachMedia: () -> Unit,
    onAddPoll: () -> Unit,
    onToggleContentWarning: () -> Unit,
    onEmoji: () -> Unit,
    onLanguage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onAttachMedia, enabled = uiState.canAddMedia) {
            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = stringResource(R.string.cd_attach_media))
        }
        IconButton(onClick = onAddPoll, enabled = uiState.canAddPoll) {
            Icon(Icons.Outlined.BarChart, contentDescription = stringResource(R.string.poll))
        }
        IconToggleButton(checked = uiState.showContentWarning, onCheckedChange = { onToggleContentWarning() }) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = stringResource(R.string.cd_content_warning))
        }
        IconButton(onClick = onEmoji) {
            Icon(Icons.Outlined.EmojiEmotions, contentDescription = stringResource(R.string.cd_custom_emoji))
        }
        val languageName = uiState.language?.let { languageDisplayName(it) }.orEmpty()
        TextButton(onClick = onLanguage) {
            Icon(
                imageVector = Icons.Outlined.Translate,
                contentDescription = stringResource(R.string.cd_post_language, languageName),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(uiState.language?.uppercase().orEmpty())
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = stringResource(R.string.compose_character_count, uiState.characterCount, uiState.maxCharacters),
            style = MaterialTheme.typography.labelMedium,
            color = if (uiState.isOverCharacterLimit) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(end = 8.dp)
        )
    }
}

/**
 * Suggestions for the mention, hashtag or emoji being typed.
 */
@Composable
private fun SuggestionList(
    suggestions: List<ComposeSuggestion>,
    onSelect: (ComposeSuggestion) -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            suggestions.forEach { suggestion ->
                when (suggestion) {
                    is ComposeSuggestion.Mention -> ListItem(
                        headlineContent = {
                            Text(
                                suggestion.account.displayName.ifBlank { suggestion.account.username },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        supportingContent = {
                            Text("@${suggestion.account.acct}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        leadingContent = {
                            AvatarImage(
                                url = suggestion.account.avatar,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onSelect(suggestion) }
                    )
                    is ComposeSuggestion.Hashtag -> ListItem(
                        headlineContent = { Text("#${suggestion.name}") },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onSelect(suggestion) }
                    )
                    is ComposeSuggestion.Emoji -> ListItem(
                        headlineContent = { Text(":${suggestion.emoji.shortcode}:") },
                        leadingContent = {
                            AsyncImage(
                                model = suggestion.emoji.staticUrl,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onSelect(suggestion) }
                    )
                }
            }
        }
    }
}

/**
 * Dialog for choosing the post language, with search.
 */
@Composable
private fun LanguagePickerDialog(
    selected: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val languages = remember {
        Locale.getISOLanguages()
            .map { code -> PostLanguage(code, languageDisplayName(code), languageNativeName(code)) }
            .filter { it.name.isNotBlank() && it.name != it.code }
            .sortedBy { it.name.lowercase() }
    }
    // Matches the name in the device language, the language's own name and the code
    val filtered = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) languages
        else languages.filter {
            it.name.lowercase().contains(q) || it.nativeName.lowercase().contains(q) || it.code == q
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.compose_post_language)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.compose_language_search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(filtered, key = { it.code }) { language ->
                        ListItem(
                            headlineContent = { Text(language.name) },
                            supportingContent = {
                                Text(
                                    if (language.nativeName != language.name) language.nativeName
                                    else language.code
                                )
                            },
                            trailingContent = {
                                if (language.code == selected) RadioButton(selected = true, onClick = null)
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { onSelect(language.code) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private data class PostLanguage(val code: String, val name: String, val nativeName: String)

/** Name of a language in the device language, e.g. "de" -> "Deutsch"/"German". */
private fun languageDisplayName(code: String): String =
    Locale.forLanguageTag(code).getDisplayLanguage(Locale.getDefault())
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }

/** Name of a language in that language itself, e.g. "de" -> "Deutsch". */
private fun languageNativeName(code: String): String {
    val locale = Locale.forLanguageTag(code)
    return locale.getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) }
}
