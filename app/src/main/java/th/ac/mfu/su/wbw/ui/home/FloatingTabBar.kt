package th.ac.mfu.su.wbw.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import th.ac.mfu.su.wbw.ui.theme.glass
import th.ac.mfu.su.wbw.ui.theme.wbwColors

data class TabItem(
    val route: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
    val contentDescription: String,
)

/** Floating frosted-glass navigation bar (design 1b): 5 icon tabs, active one gold. */
@Composable
fun FloatingTabBar(
    items: List<TabItem>,
    currentRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = wbwColors
    Row(
        modifier
            .fillMaxWidth()
            .height(62.dp)
            .glass(RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(62.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(item.route) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (selected) item.selectedIcon else item.icon,
                    contentDescription = item.contentDescription,
                    tint = if (selected) colors.gold else colors.textMuted,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
