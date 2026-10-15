package com.aegis.vault.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegis.vault.AppViewModel
import com.aegis.vault.R
import com.aegis.vault.data.CardItem
import com.aegis.vault.data.IdentityItem
import com.aegis.vault.data.ItemType
import com.aegis.vault.data.LoginItem
import com.aegis.vault.data.NoteItem
import com.aegis.vault.ui.Navigator
import com.aegis.vault.ui.Screen
import com.aegis.vault.ui.design.AegisFilterChip
import com.aegis.vault.ui.design.AegisLinkText
import com.aegis.vault.ui.design.AegisType
import com.aegis.vault.ui.design.AText
import com.aegis.vault.ui.design.EmptyState
import com.aegis.vault.ui.design.ItemRow
import com.aegis.vault.ui.design.SearchInput
import com.aegis.vault.ui.design.TagChip

@Composable
fun SearchScreen(vm: AppViewModel, nav: Navigator) {
    val vaultState by vm.vault.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableIntStateOf(0) }

    val items = vaultState?.activeItems ?: emptyList()
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
            .imePadding()
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
                            vm.markUsed(item.id)
                            nav.push(Screen.Detail(item.id))
                        },
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
