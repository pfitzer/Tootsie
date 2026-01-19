package com.pfitzer.tootsie.presentation.screens.trending.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pfitzer.tootsie.domain.model.TrendingTag
import com.pfitzer.tootsie.presentation.icons.CustomIcons
import com.pfitzer.tootsie.presentation.icons.Tags

@Composable
fun TrendingHashtagCard(
    trendingTag: TrendingTag,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CustomIcons.Tags,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "#${trendingTag.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TrendStat(
                        label = "${trendingTag.totalUses}",
                        description = "posts"
                    )
                    TrendStat(
                        label = "${trendingTag.totalAccounts}",
                        description = "people"
                    )
                }
            }
            
            // Sparkline-like representation could go here if we had more space/data
            TrendMiniChart(
                history = trendingTag.history.takeLast(7).map { it.uses },
                modifier = Modifier.size(width = 60.dp, height = 32.dp)
            )
        }
    }
}

@Composable
private fun TrendStat(
    label: String,
    description: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TrendMiniChart(
    history: List<Int>,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) return
    
    val max = history.maxOrNull() ?: 1
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        history.forEach { value ->
            val heightFactor = if (max > 0) value.toFloat() / max else 0f
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(fraction = heightFactor.coerceAtLeast(0.1f))
                    .padding(horizontal = 1.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    shape = MaterialTheme.shapes.extraSmall
                ) {}
            }
        }
    }
}
