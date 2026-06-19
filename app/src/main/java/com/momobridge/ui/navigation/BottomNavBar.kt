package com.momobridge.ui.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.momobridge.ui.theme.MomoColors
import com.momobridge.ui.theme.MomoTypography

data class BottomNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val bottomNavTabs = listOf(
    BottomNavTab("dashboard", "Dashboard", Icons.Default.Home),
    BottomNavTab("transactions", "Transactions", Icons.Default.Receipt),
    BottomNavTab("api_keys", "Keys", Icons.Default.VpnKey),
    BottomNavTab("settings", "Settings", Icons.Default.Settings)
)

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    apiKeyCount: Int = 0
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MomoColors.GroundMedium,
        tonalElevation = 0.dp
    ) {
        bottomNavTabs.forEach { tab ->
            val selected = currentRoute == tab.route
            val isKeysTab = tab.route == "api_keys"

            val animatedScale by animateFloatAsState(
                targetValue = if (selected) 1.05f else 1f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
                label = "nav_scale"
            )

            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab.route) },
                icon = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(MomoColors.Gold)
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Box(
                            modifier = Modifier.graphicsLayer {
                                scaleX = animatedScale
                                scaleY = animatedScale
                            }
                        ) {
                            if (isKeysTab && apiKeyCount > 0) {
                                BadgedBox(badge = {
                                    Badge(
                                        containerColor = MomoColors.Gold,
                                        contentColor = MomoColors.OnGold
                                    ) {
                                        Text(
                                            text = apiKeyCount.toString(),
                                            style = MomoTypography.LabelSmall
                                        )
                                    }
                                }) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label
                                )
                            }
                        }
                    }
                },
                label = {
                    Text(
                        text = tab.label.uppercase(),
                        style = MomoTypography.LabelSmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MomoColors.Gold,
                    selectedTextColor = MomoColors.Gold,
                    unselectedIconColor = MomoColors.TextTertiary,
                    unselectedTextColor = MomoColors.TextTertiary,
                    indicatorColor = MomoColors.Highlight
                )
            )
        }
    }
}
