package com.momobridge.ui.navigation

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.momobridge.ui.theme.MomoColors
import com.momobridge.ui.theme.MomoSpacing
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
        contentColor = MomoColors.TextPrimary,
        tonalElevation = MomoSpacing.Xs
    ) {
        bottomNavTabs.forEach { tab ->
            val selected = currentRoute == tab.route
            val isKeysTab = tab.route == "api_keys"

            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab.route) },
                icon = {
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
                },
                label = {
                    Text(
                        text = tab.label,
                        style = MomoTypography.LabelSmall
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
