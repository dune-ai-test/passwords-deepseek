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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegis.vault.AppViewModel
import com.aegis.vault.R
import com.aegis.vault.data.ItemType
import com.aegis.vault.ui.CategoryMode
import com.aegis.vault.ui.Navigator
import com.aegis.vault.ui.Screen
import com.aegis.vault.ui.design.AegisCard
import com.aegis.vault.ui.design.AegisColors
import com.aegis.vault.ui.design.AegisType
import com.aegis.vault.ui.design.AText
import com.aegis.vault.ui.design.IconBadge
import com.aegis.vault.ui.design.InfoNote
import com.aegis.vault.ui.design.ItemRow
import com.aegis.vault.ui.design.ProgressRing
import com.aegis.vault.ui.design.SectionHeader
import com.aegis.vault.ui.design.SmallButton

@Composable
fun SecurityScreen(vm: AppViewModel, nav: Navigator) {
    val audit by vm.audit.collectAsStateWithLifecycle()
    val a = audit ?: return

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
            AText("Security", style = AegisType.title, modifier = Modifier.weight(1f))
            IconBadge(
                R.drawable.ic_shield_check,
                AegisColors.greenSoft,
                AegisColors.greenDeep,
                size = 40.dp,
                corner = 20.dp,
                iconSize = 20.dp,
            )
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 148.dp)
        ) {
            AegisCard(modifier = Modifier.padding(horizontal = 20.dp), padding = 18.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        AText("Security score", style = AegisType.label)
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            AText("${a.score}", style = AegisType.score)
                            Spacer(Modifier.width(10.dp))
                            ScoreChip(a.scoreLabel, a.score)
                        }
                        Spacer(Modifier.height(8.dp))
                        AText(
                            if (a.fixesNeeded == 0) {
                                "No issues found — your vault looks great."
                            } else {
                                "${a.fixesNeeded} ${if (a.fixesNeeded == 1) "issue" else "issues"} could be improved."
                            },
                            style = AegisType.small,
                        )
                        Spacer(Modifier.height(2.dp))
                        AText("Checked locally just now", style = AegisType.caption)
                    }
                    Spacer(Modifier.width(14.dp))
                    ProgressRing(
                        progress = a.score / 100f,
                        color = scoreColor(a.score),
                        size = 74.dp,
                        strokeWidth = 8.dp,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatTile(
                    count = a.weak.size,
                    label = "Weak",
                    icon = R.drawable.ic_triangle_alert,
                    tint = AegisColors.orange,
                    bg = AegisColors.orangeSoft,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    count = a.reusedAccountCount,
                    label = "Reused",
                    icon = R.drawable.ic_copy,
                    tint = AegisColors.red,
                    bg = AegisColors.redSoft,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatTile(
                    count = a.old.size,
                    label = "Old",
                    icon = R.drawable.ic_clock,
                    tint = AegisColors.violetDeep,
                    bg = AegisColors.violetSoft,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    count = a.strongCount,
                    label = "Strong",
                    icon = R.drawable.ic_shield_check,
                    tint = AegisColors.greenDeep,
                    bg = AegisColors.greenSoft,
                    modifier = Modifier.weight(1f),
                )
            }

            if (a.reuseGroups.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                val group = a.reuseGroups.first()
                AegisCard(modifier = Modifier.padding(horizontal = 20.dp), padding = 16.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(
                            R.drawable.ic_copy,
                            AegisColors.redSoft,
                            AegisColors.red,
                            size = 40.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            AText("Reuse detected", style = AegisType.rowTitle)
                            AText(
                                "\"${group.password}\" is shared by ${group.items.size} accounts",
                                style = AegisType.small,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        SmallButton("Fix") {
                            nav.push(Screen.Category(CategoryMode.REUSED, ItemType.LOGIN))
                        }
                    }
                }
            }

            if (a.weak.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                SectionHeader(
                    title = "Weak passwords",
                    action = "Review",
                    onAction = { nav.push(Screen.Category(CategoryMode.WEAK, ItemType.LOGIN)) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    a.weak.take(5).forEach { item ->
                        ItemRow(
                            item = item,
                            subtitle = "Password is too simple",
                            onClick = { nav.push(Screen.Detail(item.id)) },
                        )
                    }
                }
            }

            if (a.old.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                SectionHeader(
                    title = "Old passwords",
                    action = "Review",
                    onAction = { nav.push(Screen.Category(CategoryMode.OLD, ItemType.LOGIN)) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    a.old.take(5).forEach { item ->
                        ItemRow(
                            item = item,
                            subtitle = oldSubtitle(item.updatedAt),
                            onClick = { nav.push(Screen.Detail(item.id)) },
                        )
                    }
                }
            }

            if (a.fixesNeeded == 0) {
                Spacer(Modifier.height(24.dp))
                InfoNote(
                    text = "All clear — no weak, reused or old passwords were found.",
                    icon = R.drawable.ic_check,
                    tint = AegisColors.greenDeep,
                    bg = AegisColors.greenSoft,
                    textColor = AegisColors.greenDeep,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            Spacer(Modifier.height(14.dp))
            InfoNote(
                text = "The audit runs entirely on this device — your passwords are never sent anywhere.",
                icon = R.drawable.ic_lock,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

private fun oldSubtitle(updatedAt: Long): String {
    val years = ((System.currentTimeMillis() - updatedAt) / (365L * 24 * 60 * 60 * 1000)).coerceAtLeast(1L)
    return "Unchanged for $years ${if (years == 1L) "year" else "years"}"
}

private fun scoreColor(score: Int): Color = when {
    score >= 90 -> AegisColors.green
    score >= 75 -> AegisColors.greenDeep
    score >= 50 -> AegisColors.orange
    else -> AegisColors.red
}

@Composable
private fun ScoreChip(label: String, score: Int) {
    val color = scoreColor(score)
    Box(
        Modifier
            .padding(bottom = 7.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        AText(label, style = AegisType.smallSemibold.copy(color = color))
    }
}

@Composable
private fun StatTile(
    count: Int,
    label: String,
    icon: Int,
    tint: Color,
    bg: Color,
    modifier: Modifier = Modifier,
) {
    AegisCard(modifier = modifier, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, bg, tint, size = 36.dp, corner = 11.dp, iconSize = 18.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                AText("$count", style = AegisType.big.copy(fontSize = 20.sp))
                AText(label, style = AegisType.small)
            }
        }
    }
}
