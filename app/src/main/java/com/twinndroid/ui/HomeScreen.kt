package com.twinndroid.ui

import android.content.Intent
import androidx.compose.ui.res.stringResource
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.twinndroid.R
import com.twinndroid.data.CloneFolder
import com.twinndroid.install.ShizukuInstaller
import com.twinndroid.ui.theme.LocalTwinnExtras

/** What the folder-name dialog is for: a new folder (optionally filing [moveItem] into it) or a rename. */
private data class FolderNameRequest(val rename: CloneFolder? = null, val moveItem: CloneItem? = null)

private fun LazyItemScope.motion(): Modifier = Modifier.animateItem(
    fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
    placementSpec = spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold),
)

/** First tab: the user's clones, grouped into the user's folders. */
@Composable
fun ClonesPage(vm: MainViewModel, onNewClone: () -> Unit) {
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf<CloneItem?>(null) }
    var accessFor by remember { mutableStateOf<String?>(null) }
    var moveFor by remember { mutableStateOf<String?>(null) }
    var nameRequest by remember { mutableStateOf<FolderNameRequest?>(null) }
    var confirmDeleteFolder by remember { mutableStateOf<CloneFolder?>(null) }
    val canInstall = context.packageManager.canRequestPackageInstalls() ||
        (vm.settings.useShizuku && ShizukuInstaller.hasPermission())

    // A clone whose folder no longer exists is shown at the top level instead of disappearing.
    val folderIds = vm.folders.map { it.id }.toSet()
    val loose = vm.clones.filter { val fid = it.config.folderId; fid == null || fid !in folderIds }

    val onDeleteClone: (CloneItem) -> Unit = { confirmDelete = it }
    val onAccessClone: (CloneItem) -> Unit = { accessFor = it.config.id }
    val onMoveClone: (CloneItem) -> Unit = { moveFor = it.config.id }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "header") {
            HomeHeader(
                vm.clones.size,
                vm.clones.count { it.updateAvailable },
                onAppearance = { vm.screen = Screen.Appearance },
                onSettings = { vm.screen = Screen.Settings },
            )
        }

        if (!canInstall) item(key = "warn") {
            WarningCard(
                stringResource(R.string.install_perm_title),
                stringResource(R.string.install_perm_desc),
                stringResource(R.string.allow),
                Modifier.animateItem(),
            ) {
                context.startActivity(
                    Intent(AndroidSettings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")),
                )
            }
        }

        if (vm.clones.isEmpty()) {
            item(key = "empty") { EmptyState(onNewClone) }
        } else {
            item(key = "folders-bar") { FoldersBar(onNewFolder = { nameRequest = FolderNameRequest() }) }
        }

        vm.folders.forEach { folder ->
            val inside = vm.clones.filter { it.config.folderId == folder.id }
            item(key = "folder-${folder.id}") {
                FolderHeader(
                    folder = folder,
                    count = inside.size,
                    updates = inside.count { it.updateAvailable },
                    modifier = motion(),
                    onToggle = { vm.toggleFolder(folder) },
                    onRename = { nameRequest = FolderNameRequest(rename = folder) },
                    onDelete = { confirmDeleteFolder = folder },
                )
            }
            if (!folder.collapsed) {
                if (inside.isEmpty()) {
                    item(key = "folder-empty-${folder.id}") {
                        Text(
                            stringResource(R.string.folder_empty),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = motion().padding(start = 48.dp, end = 16.dp, bottom = 2.dp),
                        )
                    }
                } else {
                    items(inside, key = { it.config.id }) { item ->
                        CloneEntry(vm, item, nested = true, modifier = motion(), onDeleteClone, onAccessClone, onMoveClone)
                    }
                }
            }
        }

        items(loose, key = { it.config.id }) { item ->
            CloneEntry(vm, item, nested = false, modifier = motion(), onDeleteClone, onAccessClone, onMoveClone)
        }
    }

    accessFor?.let { id ->
        vm.clones.firstOrNull { it.config.id == id }?.let { item -> SpecialAccessSheet(vm, item) { accessFor = null } }
    }

    moveFor?.let { id ->
        vm.clones.firstOrNull { it.config.id == id }?.let { item ->
            MoveToFolderDialog(
                item = item,
                folders = vm.folders,
                onDismiss = { moveFor = null },
                onMove = { folderId -> vm.moveToFolder(item.config, folderId); moveFor = null },
                onNewFolder = { nameRequest = FolderNameRequest(moveItem = item); moveFor = null },
            )
        }
    }

    nameRequest?.let { req ->
        val renaming = req.rename
        FolderNameDialog(
            title = stringResource(if (renaming != null) R.string.rename_folder else R.string.new_folder),
            initial = renaming?.name.orEmpty(),
            confirmLabel = stringResource(if (renaming != null) R.string.folder_save else R.string.folder_create),
            onDismiss = { nameRequest = null },
            onConfirm = { name ->
                if (renaming != null) vm.renameFolder(renaming, name) else vm.createFolder(name, req.moveItem?.config)
                nameRequest = null
            },
        )
    }

    confirmDeleteFolder?.let { folder ->
        AlertDialog(
            onDismissRequest = { confirmDeleteFolder = null },
            title = { Text(stringResource(R.string.delete_folder_title, folder.name)) },
            text = { Text(stringResource(R.string.delete_folder_desc)) },
            confirmButton = { TextButton(onClick = { vm.deleteFolder(folder); confirmDeleteFolder = null }) { Text(stringResource(R.string.folder_delete)) } },
            dismissButton = { TextButton(onClick = { confirmDeleteFolder = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    confirmDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(stringResource(R.string.remove_clone_title, item.config.label)) },
            text = {
                Text(
                    if (item.installedVersion != null) stringResource(R.string.remove_clone_installed)
                    else stringResource(R.string.remove_clone_desc),
                )
            },
            confirmButton = { TextButton(onClick = { vm.delete(item.config); confirmDelete = null }) { Text(stringResource(R.string.remove)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun EmptyState(onNewClone: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(120.dp).clip(CircleShape).background(LocalTwinnExtras.current.headerBrush), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(120.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.empty_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onNewClone) { Text(stringResource(R.string.empty_button)) }
    }
}

@Composable
fun WarningCard(title: String, body: String, action: String, modifier: Modifier = Modifier, onAction: () -> Unit) {
    TwinnCard(
        modifier.fillMaxWidth().padding(horizontal = 16.dp),
        container = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            }
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
private fun CloneEntry(
    vm: MainViewModel,
    item: CloneItem,
    nested: Boolean,
    modifier: Modifier,
    onDeleteClone: (CloneItem) -> Unit,
    onAccessClone: (CloneItem) -> Unit,
    onMoveClone: (CloneItem) -> Unit,
) {
    val context = LocalContext.current
    CloneCard(
        item = item,
        nested = nested,
        modifier = modifier,
        onOpen = { vm.launchIntent(item.config.clonePackage)?.let(context::startActivity) },
        onBuild = { vm.rebuild(item.config) },
        onEdit = { vm.edit(item.config) },
        onUninstall = {
            context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${item.config.clonePackage}")))
        },
        onDelete = { onDeleteClone(item) },
        onAccess = { onAccessClone(item) },
        onMove = { onMoveClone(item) },
    )
}

/**
 * One clone as a single slim row: icon, name + status, an inline Build/Update button only when one is
 * needed, and a ⋮ menu holding every other action (Open, Edit, Rebuild, Move to folder, …).
 */
@Composable
private fun CloneCard(
    item: CloneItem,
    nested: Boolean,
    modifier: Modifier,
    onOpen: () -> Unit,
    onBuild: () -> Unit,
    onEdit: () -> Unit,
    onUninstall: () -> Unit,
    onDelete: () -> Unit,
    onAccess: () -> Unit,
    onMove: () -> Unit,
) {
    val c = item.config
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    TwinnCard(
        modifier.fillMaxWidth().padding(start = if (nested) 32.dp else 16.dp, end = 16.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (item.icon != null) {
                Image(item.icon, null, Modifier.size(40.dp))
            } else {
                Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                StatusChip(item)
                if (item.missingAccess.isNotEmpty()) {
                    Text(
                        stringResource(R.string.needs_access, item.missingAccess.joinToString { ctx.getString(it.label).lowercase() }),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAccess).padding(vertical = 2.dp),
                    )
                }
            }
            if (item.updateAvailable || item.installedVersion == null) {
                FilledTonalIconButton(onClick = onBuild) {
                    Icon(
                        if (item.updateAvailable) Icons.Rounded.SystemUpdate else Icons.Rounded.Build,
                        stringResource(if (item.updateAvailable) R.string.update else R.string.build_install),
                        Modifier.size(20.dp),
                    )
                }
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.more)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (item.installedVersion != null) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.open)) }, leadingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null) }, onClick = { menu = false; onOpen() })
                    }
                    DropdownMenuItem(text = { Text(stringResource(R.string.edit)) }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.rebuild)) }, leadingIcon = { Icon(Icons.Rounded.Build, null) }, onClick = { menu = false; onBuild() })
                    if (item.access.isNotEmpty()) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.special_access)) }, leadingIcon = { Icon(Icons.Rounded.AdminPanelSettings, null) }, onClick = { menu = false; onAccess() })
                    }
                    DropdownMenuItem(text = { Text(stringResource(R.string.move_to_folder)) }, leadingIcon = { Icon(Icons.Rounded.Folder, null) }, onClick = { menu = false; onMove() })
                    if (item.installedVersion != null) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.uninstall_clone)) }, leadingIcon = { Icon(Icons.Rounded.Delete, null) }, onClick = { menu = false; onUninstall() })
                    }
                    DropdownMenuItem(text = { Text(stringResource(R.string.remove_from_list)) }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}

/** Slim row above the list with the "New folder" action. */
@Composable
private fun FoldersBar(onNewFolder: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
        TextButton(
            onClick = onNewFolder,
            modifier = Modifier.height(32.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        ) {
            Icon(Icons.Rounded.CreateNewFolder, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.new_folder))
        }
    }
}

/** Tappable folder row: tap to fold/unfold, ⋮ for rename and delete. */
@Composable
private fun FolderHeader(
    folder: CloneFolder,
    count: Int,
    updates: Int,
    modifier: Modifier,
    onToggle: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val res = LocalContext.current.resources
    var menu by remember { mutableStateOf(false) }
    TwinnCard(
        modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            Modifier.clickable(onClick = onToggle).padding(start = 12.dp, top = 2.dp, bottom = 2.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (folder.collapsed) Icons.Rounded.Folder else Icons.Rounded.FolderOpen,
                null,
                Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(folder.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        res.getQuantityString(R.plurals.clone_count, count, count),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (updates > 0) {
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            res.getQuantityString(R.plurals.update_count, updates, updates),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
            Icon(
                if (folder.collapsed) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.more)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.rename_folder)) }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = { menu = false; onRename() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.delete_folder)) }, leadingIcon = { Icon(Icons.Rounded.Delete, null) }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun FolderNameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                singleLine = true,
                label = { Text(stringResource(R.string.folder_name)) },
            )
        },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name) }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Pick the folder a clone lives in ("No folder" = top level), or make a new one for it. */
@Composable
private fun MoveToFolderDialog(
    item: CloneItem,
    folders: List<CloneFolder>,
    onDismiss: () -> Unit,
    onMove: (String?) -> Unit,
    onNewFolder: () -> Unit,
) {
    val current = item.config.folderId?.takeIf { id -> folders.any { it.id == id } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.move_to_folder)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                FolderChoice(stringResource(R.string.no_folder), selected = current == null) { onMove(null) }
                folders.forEach { f -> FolderChoice(f.name, selected = current == f.id) { onMove(f.id) } }
            }
        },
        confirmButton = {
            TextButton(onClick = onNewFolder) {
                Icon(Icons.Rounded.CreateNewFolder, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.new_folder))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun FolderChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StatusChip(item: CloneItem) {
    val (text, color) = when {
        item.updateAvailable -> stringResource(R.string.status_update) to MaterialTheme.colorScheme.tertiary
        item.installedVersion != null -> stringResource(R.string.status_installed, item.config.builtVersionName ?: item.installedVersion ?: "") to MaterialTheme.colorScheme.secondary
        else -> stringResource(R.string.status_not_installed) to MaterialTheme.colorScheme.outline
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
        Text(
            "  ·  " + stringResource(R.string.status_from, item.config.sourceLabel),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
