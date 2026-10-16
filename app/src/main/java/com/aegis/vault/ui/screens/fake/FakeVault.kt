package com.aegis.vault.ui.screens.fake

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.aegis.vault.AppViewModel
import com.aegis.vault.R
import com.aegis.vault.data.CardItem
import com.aegis.vault.data.Fmt
import com.aegis.vault.data.IdentityItem
import com.aegis.vault.data.ItemType
import com.aegis.vault.data.LoginItem
import com.aegis.vault.data.NoteItem
import com.aegis.vault.data.PasswordTools
import com.aegis.vault.data.VaultItem
import com.aegis.vault.ui.CategoryMode
import com.aegis.vault.ui.Tab
import com.aegis.vault.ui.design.AegisCard
import com.aegis.vault.ui.design.AegisColors
import com.aegis.vault.ui.design.AegisFilterChip
import com.aegis.vault.ui.design.AegisIcon
import com.aegis.vault.ui.design.AegisLinkText
import com.aegis.vault.ui.design.AegisType
import com.aegis.vault.ui.design.AText
import com.aegis.vault.ui.design.AvatarBadge
import com.aegis.vault.ui.design.EmptyState
import com.aegis.vault.ui.design.IconBadge
import com.aegis.vault.ui.design.ItemRow
import com.aegis.vault.ui.design.ModalSheet
import com.aegis.vault.ui.design.NavHeader
import com.aegis.vault.ui.design.SearchBarButton
import com.aegis.vault.ui.design.SearchInput
import com.aegis.vault.ui.design.SectionHeader
import com.aegis.vault.ui.design.SheetTitle
import com.aegis.vault.ui.design.TagChip
import com.aegis.vault.ui.design.defaultSubtitle
import com.aegis.vault.ui.design.pressable
import com.aegis.vault.ui.screens.SortMode

/* ────────────────────────────────────────────────────────────────────────────
 * Decoy vault — a self-contained twin of the unlocked app.
 *
 * Opened whenever an unlock attempt fails. It renders the same UI with
 * in-memory demo content (see FakeVaultData) and has no connection to the
 * real vault: nothing here reads, writes or decrypts real data.
 * ──────────────────────────────────────────────────────────────────────────── */

sealed interface FakeScreen {
    data object Vault : FakeScreen
    data class Category(val mode: CategoryMode, val type: ItemType) : FakeScreen
    data class Detail(val id: String) : FakeScreen
    data class Edit(val id: String? = null, val presetType: ItemType? = null) : FakeScreen
    data object Generator : FakeScreen
    data object Security : FakeScreen
    data object Settings : FakeScreen
    data object Search : FakeScreen
    data object Trash : FakeScreen
}

/** Tiny back-stack, mirrors [com.aegis.vault.ui.Navigator]. */
class FakeNavigator {
    val stack = mutableStateListOf<FakeScreen>(FakeScreen.Vault)

    var lastDirection by mutableIntStateOf(0)
        private set

    val current: FakeScreen get() = stack.last()
    val depth: Int get() = stack.size

    fun push(screen: FakeScreen) {
        lastDirection = 1
        stack.add(screen)
    }

    fun pop() {
        if (stack.size > 1) {
            lastDirection = -1
            stack.removeAt(stack.lastIndex)
        }
    }

    fun switchTab(screen: FakeScreen) {
        lastDirection = 0
        stack.clear()
        stack.add(screen)
    }
}

private fun fakeTabOf(screen: FakeScreen): Tab? = when (screen) {
    FakeScreen.Vault, is FakeScreen.Category, is FakeScreen.Detail -> Tab.PASSWORDS
    FakeScreen.Generator -> Tab.GENERATOR
    FakeScreen.Security -> Tab.SECURITY
    FakeScreen.Settings, FakeScreen.Trash -> Tab.SETTINGS
    else -> null
}

private fun fakeRootFor(tab: Tab): FakeScreen = when (tab) {
    Tab.PASSWORDS -> FakeScreen.Vault
    Tab.GENERATOR -> FakeScreen.Generator
    Tab.SECURITY -> FakeScreen.Security
    Tab.SETTINGS -> FakeScreen.Settings
}

@Composable
fun FakeVaultShell(vm: AppViewModel) {
    val nav = remember { FakeNavigator() }
    val state = remember { FakeVaultState() }
    val current = nav.current

    BackHandler(enabled = nav.depth > 1) { nav.pop() }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                        vm.noteInteraction()
                    }
                }
            }
    ) {
        AnimatedContent(
            targetState = current,
            transitionSpec = {
                when (nav.lastDirection) {
                    1 -> (slideInHorizontally(animationSpec = tween(240)) { it / 3 } + fadeIn(tween(240))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(240)) { -it / 8 } + fadeOut(tween(180)))
                    -1 -> (slideInHorizontally(animationSpec = tween(240)) { -it / 8 } + fadeIn(tween(240))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(240)) { it } + fadeOut(tween(180)))
                    else -> fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                }
            },
            label = "fakeScreen",
            modifier = Modifier.fillMaxSize(),
        ) { screen ->
            when (screen) {
                FakeScreen.Vault -> FakeVaultRoot(nav, state)
                is FakeScreen.Category -> FakeCategoryScreen(nav, state, screen)
                is FakeScreen.Detail -> FakeDetailScreen(vm, nav, state, screen.id)
                is FakeScreen.Edit -> FakeEditScreen(vm, nav, state, screen)
                FakeScreen.Generator -> FakeGeneratorScreen(vm)
                FakeScreen.Security -> FakeSecurityScreen(nav, state)
                FakeScreen.Settings -> FakeSettingsScreen(vm, nav, state)
                FakeScreen.Search -> FakeSearchScreen(nav, state)
                FakeScreen.Trash -> FakeTrashScreen(vm, nav, state)
            }
        }

        val tab = fakeTabOf(current)
        if (tab != null) {
            FakeDock(
                current = tab,
                onTab = { target -> nav.switchTab(fakeRootFor(target)) },
                onAdd = { nav.push(FakeScreen.Edit()) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * Root: the main "Passwords" screen
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
private fun FakeVaultRoot(nav: FakeNavigator, state: FakeVaultState) {
    val items = state.activeItems
    val logins = items.count { it.type == ItemType.LOGIN }
    val cards = items.count { it.type == ItemType.CARD }
    val notes = items.count { it.type == ItemType.NOTE }
    val identities = items.count { it.type == ItemType.IDENTITY }
    val recent = items
        .filter { it.lastUsedAt != null }
        .sortedByDescending { it.lastUsedAt }
        .take(3)
    val a = state.audit

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AText("Passwords", style = AegisType.title, modifier = Modifier.weight(1f))
            Box(
                Modifier.pressable(scaleDown = 0.92f) { nav.switchTab(FakeScreen.Settings) }
            ) {
                AvatarBadge("A", AegisColors.violet, size = 40.dp, corner = 20.dp)
            }
        }

        SearchBarButton(
            text = "Search passwords, cards, notes",
            onClick = { nav.push(FakeScreen.Search) },
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(14.dp))

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 148.dp)
        ) {
            AegisCard(modifier = Modifier.padding(horizontal = 20.dp), padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        R.drawable.ic_hard_drive,
                        AegisColors.blueSoft,
                        AegisColors.blue,
                        size = 40.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        AText("Local-only vault", style = AegisType.rowTitle)
                        AText(
                            "${Fmt.formatBytes(FakeVaultData.APPROX_BYTES)} on this device",
                            style = AegisType.small,
                            modifier = Modifier.padding(top = 1.dp),
                        )
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AegisColors.greenSoft)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        AText("Offline", style = AegisType.smallSemibold.copy(color = AegisColors.greenDeep))
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CategoryTile(
                    R.drawable.ic_key_round, AegisColors.blue, AegisColors.blueSoft,
                    "Logins", logins, Modifier.weight(1f),
                ) { nav.push(FakeScreen.Category(CategoryMode.ALL, ItemType.LOGIN)) }
                CategoryTile(
                    R.drawable.ic_credit_card, AegisColors.orange, AegisColors.orangeSoft,
                    "Cards", cards, Modifier.weight(1f),
                ) { nav.push(FakeScreen.Category(CategoryMode.ALL, ItemType.CARD)) }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CategoryTile(
                    R.drawable.ic_file_text, AegisColors.violetDeep, AegisColors.violetSoft,
                    "Notes", notes, Modifier.weight(1f),
                ) { nav.push(FakeScreen.Category(CategoryMode.ALL, ItemType.NOTE)) }
                CategoryTile(
                    R.drawable.ic_user, AegisColors.greenDeep, AegisColors.greenSoft,
                    "Identities", identities, Modifier.weight(1f),
                ) { nav.push(FakeScreen.Category(CategoryMode.ALL, ItemType.IDENTITY)) }
            }

            if (recent.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                SectionHeader(
                    title = "Recently used",
                    action = "See all",
                    onAction = { nav.push(FakeScreen.Category(CategoryMode.RECENT, ItemType.LOGIN)) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    recent.forEach { item ->
                        ItemRow(
                            item = item,
                            onClick = {
                                state.markUsed(item.id)
                                nav.push(FakeScreen.Detail(item.id))
                            },
                        )
                    }
                }
            }

            if (a.reuseGroups.isNotEmpty() || a.old.isNotEmpty() || a.weak.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                SectionHeader(
                    title = "Needs attention",
                    action = "Fix all",
                    onAction = { nav.switchTab(FakeScreen.Security) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (a.reuseGroups.isNotEmpty()) {
                        val group = a.reuseGroups.first()
                        AttentionRow(
                            icon = R.drawable.ic_copy,
                            tint = AegisColors.orange,
                            bg = AegisColors.orangeSoft,
                            title = "\"${group.password}\" reused",
                            subtitle = "${group.items.size} accounts share it",
                        ) { nav.push(FakeScreen.Category(CategoryMode.REUSED, ItemType.LOGIN)) }
                    }
                    if (a.old.isNotEmpty()) {
                        val oldest = a.old.minBy { it.updatedAt }
                        val years = yearsSince(oldest.updatedAt)
                        AttentionRow(
                            icon = R.drawable.ic_clock,
                            tint = AegisColors.violetDeep,
                            bg = AegisColors.violetSoft,
                            title = "Unchanged for $years ${if (years == 1L) "year" else "years"}",
                            subtitle = listOfNotNull(
                                oldest.title.takeIf { it.isNotBlank() },
                                (oldest as? LoginItem)?.username?.takeIf { it.isNotBlank() },
                            ).joinToString(" · "),
                        ) { nav.push(FakeScreen.Category(CategoryMode.OLD, ItemType.LOGIN)) }
                    }
                    if (a.weak.isNotEmpty()) {
                        val count = a.weak.size
                        AttentionRow(
                            icon = R.drawable.ic_triangle_alert,
                            tint = AegisColors.red,
                            bg = AegisColors.redSoft,
                            title = "$count weak ${if (count == 1) "password" else "passwords"}",
                            subtitle = "Easily guessed — update them soon",
                        ) { nav.push(FakeScreen.Category(CategoryMode.WEAK, ItemType.LOGIN)) }
                    }
                }
            }

            if (items.isEmpty()) {
                Spacer(Modifier.height(28.dp))
                EmptyState(
                    icon = R.drawable.ic_key_round,
                    title = "Your vault is empty",
                    message = "Tap the blue + button to add your first login, card or secure note.",
                    cta = "Add a login",
                    onCta = { nav.push(FakeScreen.Edit(presetType = ItemType.LOGIN)) },
                    footnote = "Everything stays encrypted on this device.",
                )
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

private fun yearsSince(millis: Long): Long =
    ((System.currentTimeMillis() - millis) / (365L * 24 * 60 * 60 * 1000)).coerceAtLeast(1L)

@Composable
private fun CategoryTile(
    icon: Int,
    tint: Color,
    bg: Color,
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    AegisCard(modifier = modifier, padding = 14.dp, onClick = onClick) {
        IconBadge(icon, bg, tint, size = 38.dp, corner = 12.dp, iconSize = 19.dp)
        Spacer(Modifier.height(10.dp))
        AText(title, style = AegisType.rowTitle)
        AText(
            if (count == 1) "1 item" else "$count items",
            style = AegisType.small,
            modifier = Modifier.padding(top = 1.dp),
        )
    }
}

@Composable
private fun AttentionRow(
    icon: Int,
    tint: Color,
    bg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AegisColors.card)
            .pressable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, bg, tint, size = 38.dp, corner = 12.dp, iconSize = 19.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            AText(title, style = AegisType.rowTitle, maxLines = 1)
            if (subtitle.isNotBlank()) {
                AText(subtitle, style = AegisType.small, maxLines = 1, modifier = Modifier.padding(top = 1.dp))
            }
        }
        AegisIcon(R.drawable.ic_chevron_right, size = 16.dp, tint = AegisColors.inkTertiary)
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * Category list (with filter chips + sort sheet)
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
private fun FakeCategoryScreen(nav: FakeNavigator, state: FakeVaultState, screen: FakeScreen.Category) {
    var mode by remember(screen) { mutableStateOf(screen.mode) }
    var sort by remember {
        mutableStateOf(
            if (screen.mode == CategoryMode.RECENT) SortMode.RECENT else SortMode.AZ
        )
    }
    var showSort by remember { mutableStateOf(false) }
    val a = state.audit

    val baseList: List<VaultItem> = when (mode) {
        CategoryMode.ALL -> state.activeItems.filter { it.type == screen.type }
        CategoryMode.RECENT -> state.activeItems.filter { it.lastUsedAt != null }
        CategoryMode.WEAK -> a.weak
        CategoryMode.REUSED -> a.reuseGroups.flatMap { it.items }.distinctBy { it.id }
        CategoryMode.OLD -> a.old
    }
    val list = when (sort) {
        SortMode.AZ -> baseList.sortedBy { it.title.lowercase() }
        SortMode.ZA -> baseList.sortedByDescending { it.title.lowercase() }
        SortMode.RECENT -> baseList.sortedByDescending { it.lastUsedAt ?: 0L }
        SortMode.UPDATED -> baseList.sortedByDescending { it.updatedAt }
    }

    val title = when (mode) {
        CategoryMode.ALL -> when (screen.type) {
            ItemType.LOGIN -> "Logins"
            ItemType.CARD -> "Cards"
            ItemType.NOTE -> "Secure Notes"
            ItemType.IDENTITY -> "Identities"
        }
        CategoryMode.RECENT -> "Recently used"
        CategoryMode.WEAK -> "Weak passwords"
        CategoryMode.REUSED -> "Reused passwords"
        CategoryMode.OLD -> "Old passwords"
    }
    val noun = when (mode) {
        CategoryMode.ALL -> when (screen.type) {
            ItemType.LOGIN -> if (list.size == 1) "login" else "logins"
            ItemType.CARD -> if (list.size == 1) "card" else "cards"
            ItemType.NOTE -> if (list.size == 1) "note" else "notes"
            ItemType.IDENTITY -> if (list.size == 1) "identity" else "identities"
        }
        else -> if (list.size == 1) "account" else "accounts"
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(bottom = 148.dp)
    ) {
        NavHeader(
            title = title,
            subtitle = "${list.size} $noun · ${sort.subtitle}",
            onBack = { nav.pop() },
            actions = {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AegisColors.blueSoft)
                        .pressable(scaleDown = 0.9f) { showSort = true },
                    contentAlignment = Alignment.Center,
                ) {
                    AegisIcon(R.drawable.ic_sliders_horizontal, size = 18.dp, tint = AegisColors.blue)
                }
            },
        )

        if (mode != CategoryMode.RECENT && screen.type == ItemType.LOGIN) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AegisFilterChip(label = "All", selected = mode == CategoryMode.ALL, onClick = { mode = CategoryMode.ALL })
                AegisFilterChip(label = "Weak", selected = mode == CategoryMode.WEAK, onClick = { mode = CategoryMode.WEAK })
                AegisFilterChip(label = "Reused", selected = mode == CategoryMode.REUSED, onClick = { mode = CategoryMode.REUSED })
                AegisFilterChip(label = "Old", selected = mode == CategoryMode.OLD, onClick = { mode = CategoryMode.OLD })
            }
        }

        Spacer(Modifier.height(10.dp))

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (list.isEmpty()) {
                when (mode) {
                    CategoryMode.ALL -> when (screen.type) {
                        ItemType.LOGIN -> EmptyState(
                            icon = R.drawable.ic_key_round,
                            title = "No logins yet",
                            message = "Save the passwords you use every day — they'll be one tap away.",
                            cta = "Add a login",
                            onCta = { nav.push(FakeScreen.Edit(presetType = ItemType.LOGIN)) },
                        )
                        ItemType.CARD -> EmptyState(
                            icon = R.drawable.ic_credit_card,
                            title = "No cards yet",
                            message = "Keep card details handy for checkout without reaching for your wallet.",
                            cta = "Add a card",
                            onCta = { nav.push(FakeScreen.Edit(presetType = ItemType.CARD)) },
                        )
                        ItemType.NOTE -> EmptyState(
                            icon = R.drawable.ic_file_text,
                            title = "No secure notes yet",
                            message = "Store passport details, software keys and Wi-Fi passwords — encrypted on this device.",
                            cta = "Create a note",
                            onCta = { nav.push(FakeScreen.Edit(presetType = ItemType.NOTE)) },
                        )
                        ItemType.IDENTITY -> EmptyState(
                            icon = R.drawable.ic_user,
                            title = "No identities yet",
                            message = "Keep names, emails and addresses ready to paste into forms.",
                            cta = "Add an identity",
                            onCta = { nav.push(FakeScreen.Edit(presetType = ItemType.IDENTITY)) },
                        )
                    }
                    CategoryMode.RECENT -> EmptyState(
                        icon = R.drawable.ic_clock,
                        title = "Nothing used recently",
                        message = "Items you open will show up here for quick access.",
                    )
                    CategoryMode.WEAK -> EmptyState(
                        icon = R.drawable.ic_shield_check,
                        title = "No weak passwords",
                        message = "Nice — every password passes the strength check.",
                    )
                    CategoryMode.REUSED -> EmptyState(
                        icon = R.drawable.ic_copy,
                        title = "No reused passwords",
                        message = "Every login has a password of its own. Well done.",
                    )
                    CategoryMode.OLD -> EmptyState(
                        icon = R.drawable.ic_clock,
                        title = "No old passwords",
                        message = "Everything has been updated within the last year.",
                    )
                }
            } else {
                list.forEach { item ->
                    ItemRow(
                        item = item,
                        onClick = {
                            state.markUsed(item.id)
                            nav.push(FakeScreen.Detail(item.id))
                        },
                        subtitle = subtitleFor(item, mode, a),
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showSort) {
        ModalSheet(onDismiss = { showSort = false }) {
            SheetTitle("Sort")
            Spacer(Modifier.height(8.dp))
            SortMode.entries.forEach { option ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .pressable(scaleDown = 0.98f) {
                            sort = option
                            showSort = false
                        }
                        .padding(vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AText(option.menu, style = AegisType.body, modifier = Modifier.weight(1f))
                    if (sort == option) {
                        AegisIcon(R.drawable.ic_check, size = 18.dp, tint = AegisColors.blue)
                    }
                }
            }
        }
    }
}

private fun subtitleFor(item: VaultItem, mode: CategoryMode, audit: PasswordTools.Audit?): String =
    when (mode) {
        CategoryMode.WEAK -> "Password is too simple"
        CategoryMode.REUSED -> {
            val count = (item as? LoginItem)?.let { login ->
                audit?.reuseGroups?.firstOrNull { it.password == login.password }?.items?.size
            } ?: 0
            if (count > 1) "Reused by $count accounts" else "Reused password"
        }
        CategoryMode.OLD -> {
            val years = ((System.currentTimeMillis() - item.updatedAt) / (365L * 24 * 60 * 60 * 1000))
                .coerceAtLeast(1L)
            "Unchanged for $years ${if (years == 1L) "year" else "years"}"
        }
        else -> defaultSubtitle(item)
    }

/* ────────────────────────────────────────────────────────────────────────────
 * Search
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
private fun FakeSearchScreen(nav: FakeNavigator, state: FakeVaultState) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableIntStateOf(0) }

    val items = state.activeItems
    val needle = query.trim().lowercase()
    val results = if (needle.isEmpty()) {
        emptyList()
    } else {
        items
            .filter { item ->
                val haystack = buildString {
                    append(item.title).append(' ')
                    append(item.tags.joinToString(" ")).append(' ')
                    when (item) {
                        is LoginItem -> {
                            append(item.username).append(' ')
                            append(item.website).append(' ')
                            append(item.notes)
                        }
                        is CardItem -> {
                            append(item.cardholder).append(' ')
                            append(item.number.filter { it.isDigit() }.takeLast(4)).append(' ')
                            append(item.billingAddress).append(' ')
                            append(item.notes)
                        }
                        is NoteItem -> append(item.body)
                        is IdentityItem -> {
                            append(item.fullName).append(' ')
                            append(item.email).append(' ')
                            append(item.phone).append(' ')
                            append(item.address).append(' ')
                            append(item.notes)
                        }
                    }
                }.lowercase()
                haystack.contains(needle)
            }
            .filter { item ->
                when (filter) {
                    1 -> item.type == ItemType.LOGIN
                    2 -> item.type == ItemType.CARD
                    3 -> item.type == ItemType.NOTE
                    4 -> item.type == ItemType.IDENTITY
                    else -> true
                }
            }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchInput(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search your vault",
                onClear = { query = "" },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(14.dp))
            AegisLinkText("Cancel", onClick = { nav.pop() })
        }

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val labels = listOf("All", "Logins", "Cards", "Notes", "Identities")
            labels.forEachIndexed { index, label ->
                AegisFilterChip(
                    label = label,
                    selected = filter == index,
                    onClick = { filter = index },
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (needle.isEmpty()) {
                AText(
                    "Suggested searches",
                    style = AegisType.label,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("work", "email", "passport", "bank", "wifi").forEach { word ->
                        TagChip(text = word, onClick = { query = word })
                    }
                }
            } else if (results.isEmpty()) {
                EmptyState(
                    icon = R.drawable.ic_search,
                    title = "No results",
                    message = "Nothing in your vault matched \"$query\".",
                    footnote = "Search works offline on titles, usernames, tags and notes.",
                )
            } else {
                results.forEach { item ->
                    ItemRow(
                        item = item,
                        onClick = {
                            state.markUsed(item.id)
                            nav.push(FakeScreen.Detail(item.id))
                        },
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * Floating dock: 4 tabs + blue circular add button
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
private fun FakeDock(
    current: Tab,
    onTab: (Tab) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(29.dp),
                    clip = false,
                    ambientColor = Color(0x1F0B1B3A),
                    spotColor = Color(0x1F0B1B3A),
                )
                .clip(RoundedCornerShape(29.dp))
                .background(Color(0xFFFFFFFF))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FakeDockTab(Tab.PASSWORDS, R.drawable.ic_key_round, "Passwords", current, onTab, Modifier.weight(1f))
            FakeDockTab(Tab.GENERATOR, R.drawable.ic_refresh_cw, "Generator", current, onTab, Modifier.weight(1f))
            FakeDockTab(Tab.SECURITY, R.drawable.ic_shield_check, "Security", current, onTab, Modifier.weight(1f))
            FakeDockTab(Tab.SETTINGS, R.drawable.ic_settings, "Settings", current, onTab, Modifier.weight(1f))
            Spacer(Modifier.width(4.dp))
            Box(
                Modifier
                    .size(44.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        clip = false,
                        ambientColor = Color(0x59007AFF),
                        spotColor = Color(0x59007AFF),
                    )
                    .clip(CircleShape)
                    .background(AegisColors.blue)
                    .pressable(scaleDown = 0.92f) { onAdd() },
                contentAlignment = Alignment.Center,
            ) {
                AegisIcon(R.drawable.ic_plus, size = 20.dp, tint = Color.White)
            }
        }
    }
}

@Composable
private fun FakeDockTab(
    tab: Tab,
    icon: Int,
    label: String,
    current: Tab,
    onTab: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = tab == current
    Column(
        modifier = modifier
            .pressable(scaleDown = 0.95f) { onTab(tab) }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .width(46.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (selected) AegisColors.blueSoft else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            AegisIcon(
                icon,
                size = 18.dp,
                tint = if (selected) AegisColors.blue else AegisColors.dockInactive,
            )
        }
        Spacer(Modifier.height(2.dp))
        AText(
            label,
            style = AegisType.tinyBold,
            color = if (selected) AegisColors.blue else AegisColors.dockInactive,
            maxLines = 1,
        )
    }
}
