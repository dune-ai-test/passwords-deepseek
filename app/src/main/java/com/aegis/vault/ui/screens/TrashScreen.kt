package com.aegis.vault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegis.vault.AppViewModel
import com.aegis.vault.R
import com.aegis.vault.data.Fmt
import com.aegis.vault.data.VaultItem
import com.aegis.vault.ui.Navigator
import com.aegis.vault.ui.design.AegisColors
import com.aegis.vault.ui.design.AegisIcon
import com.aegis.vault.ui.design.AegisLinkText
import com.aegis.vault.ui.design.AegisType
import com.aegis.vault.ui.design.AText
import com.aegis.vault.ui.design.ConfirmDialog
import com.aegis.vault.ui.design.EmptyState
import com.aegis.vault.ui.design.ItemBadge
import com.aegis.vault.ui.design.NavHeader
import com.aegis.vault.ui.design.pressable

@Composable
fun TrashScreen(vm: AppViewModel, nav: Navigator) {
    val vaultState by vm.vault.collectAsStateWithLifecycle()
    val items = vaultState?.trashedItems ?: emptyList()
    var confirmEmpty by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(bottom = 96.dp)
    ) {
        NavHeader(
            title = "Trash",
            subtitle = if (items.isEmpty()) {
                "Empty"
            } else {
                "${items.size} ${if (items.size == 1) "item" else "items"}"
            },
            onBack = { nav.pop() },
            actions = {
                if (items.isNotEmpty()) {
                    AegisLinkText(
                        "Empty",
                        onClick = { confirmEmpty = true },
                        color = AegisColors.red,
                    )
                }
            },
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (items.isEmpty()) {
                EmptyState(
                    icon = R.drawable.ic_trash_2,
                    title = "Trash is empty",
                    message = "Deleted items appear here before they're gone for good.",
                )
            } else {
                items.forEach { item ->
                    TrashRow(
                        item = item,
                        onRestore = { vm.restore(item.id) },
                        onDelete = { vm.deleteForever(item.id) },
                    )
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (confirmEmpty) {
        ConfirmDialog(
            title = "Empty the trash?",
            message = "${items.size} ${if (items.size == 1) "item" else "items"} will be permanently deleted.",
            confirmLabel = "Delete forever",
            destructive = true,
            onConfirm = {
                confirmEmpty = false
                vm.emptyTrash()
            },
            onDismiss = { confirmEmpty = false },
        )
    }
}

@Composable
private fun TrashRow(
    item: VaultItem,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AegisColors.card)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemBadge(item)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            AText(item.title.ifBlank { "Untitled" }, style = AegisType.rowTitle, maxLines = 1)
            AText(
                "Deleted ${Fmt.relativeTime(item.trashedAt ?: item.updatedAt)}",
                style = AegisType.small,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        RoundAction(R.drawable.ic_rotate_ccw, AegisColors.blue, onRestore)
        Spacer(Modifier.width(6.dp))
        RoundAction(R.drawable.ic_trash_2, AegisColors.red, onDelete)
    }
}

@Composable
private fun RoundAction(icon: Int, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f))
            .pressable(scaleDown = 0.88f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AegisIcon(icon, size = 16.dp, tint = tint)
    }
}
