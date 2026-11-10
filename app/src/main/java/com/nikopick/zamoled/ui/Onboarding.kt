package com.nikopick.zamoled.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nikopick.zamoled.gen.Styles

/** First launch: pick the worlds you like, and the feed leans toward them. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(onDone: (Set<String>) -> Unit) {
    var picked by remember { mutableStateOf(emptySet<String>()) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(48.dp))
        Text("Welcome to", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        GradientText("Zamoled", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(12.dp))
        Text(
            "Pitch-black wallpapers, drawn on your phone. Pick the worlds you like and your feed will lean toward them. You can still browse everything.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Styles.categories.forEach { category ->
                    val selected = category.name in picked
                    FilterChip(
                        selected = selected,
                        onClick = { picked = if (selected) picked - category.name else picked + category.name },
                        label = { Text(category.label, style = MaterialTheme.typography.titleMedium) },
                        leadingIcon = { Icon(category.icon, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) },
                        shape = CircleShape,
                        modifier = Modifier.height(48.dp),
                    )
                }
            }
        }
        Button(onClick = { onDone(picked) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(if (picked.isEmpty()) "Show me everything" else "Continue with ${picked.size}")
        }
        TextButton(onClick = { onDone(emptySet()) }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Skip") }
        Spacer(Modifier.height(8.dp))
    }
}
