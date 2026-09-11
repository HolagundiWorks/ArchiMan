package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BreadcrumbItem(
    val title: String,
    val onClick: (() -> Unit)? = null
)

@Composable
fun BreadcrumbBar(
    items: List<BreadcrumbItem>,
    modifier: Modifier = Modifier,
    onHomeClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Home icon
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            modifier = Modifier.clickable { onHomeClick() }
        ) {
            Icon(
                Icons.Default.Home,
                contentDescription = "Home",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(6.dp).size(16.dp)
            )
        }

        items.forEachIndexed { index, item ->
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "separator",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 6.dp).size(10.dp)
            )

            val isLast = index == items.size - 1
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (isLast) MaterialTheme.colorScheme.primary else Color.Transparent,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .then(
                        if (!isLast && item.onClick != null) Modifier.clickable { item.onClick.invoke() }
                        else Modifier
                    )
            ) {
                Text(
                    text = item.title,
                    fontSize = 12.sp,
                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                    color = if (isLast) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    maxLines = 1
                )
            }
        }
    }
}
