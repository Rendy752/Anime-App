package com.luminoverse.animevibe.ui.main.navigation

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun getNavigationBarPadding(): androidx.compose.ui.unit.Dp {
    val bottomPadding = WindowInsets.navigationBars.getBottom(LocalDensity.current)
    return with(LocalDensity.current) { bottomPadding.toDp() }
}

@Composable
fun BottomNavigationBar(modifier: Modifier, navController: NavHostController) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        var selectedItemWidth by remember { mutableStateOf(0.dp) }
        var selectedItemXOffset by remember { mutableStateOf(0.dp) }

        val animatedSelectedItemXOffset by animateDpAsState(
            targetValue = selectedItemXOffset,
            animationSpec = tween(durationMillis = 300), label = "animatedXOffset"
        )

        val density = LocalDensity.current

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 8.dp,
            modifier = modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp + getNavigationBarPadding())
                    .padding(bottom = getNavigationBarPadding())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavRoute.bottomRoutes.forEach { screen ->
                        val icon = screen.icon ?: Icons.Filled.Home
                        val isSelected = currentRoute == screen.route
                        CustomBottomNavigationItem(
                            icon = icon,
                            label = screen.route.replaceFirstChar { it.uppercase() },
                            selected = isSelected,
                            onClick = { navController.navigateTo(screen) },
                            onGloballyPositioned = { layoutCoordinates ->
                                if (isSelected) {
                                    selectedItemWidth =
                                        with(density) { layoutCoordinates.size.width.toDp() }
                                    val itemCenter =
                                        with(density) { layoutCoordinates.positionInParent().x.toDp() + layoutCoordinates.size.width.toDp() / 2 }
                                    selectedItemXOffset = itemCenter - (selectedItemWidth / 2)
                                }
                            }
                        )
                    }
                }

                if (selectedItemWidth > 0.dp) {
                    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = animatedSelectedItemXOffset, y = 0.dp)
                            .height(4.dp)
                            .width(selectedItemWidth)
                            .background(
                                color = primaryContainerColor,
                                shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.CustomBottomNavigationItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onGloballyPositioned: (androidx.compose.ui.layout.LayoutCoordinates) -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.1f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 200f
        ),
        label = "itemScaleAnimation"
    )

    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    val startColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    else MaterialTheme.colorScheme.surfaceContainer
    val endColor = MaterialTheme.colorScheme.surfaceContainer

    val density = LocalDensity.current
    val backgroundBrush = remember(startColor, endColor) {
        if (selected) {
            Brush.verticalGradient(
                colors = listOf(startColor, endColor),
                startY = with(density) { 0.dp.toPx() },
                endY = with(density) { 56.dp.toPx() }
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(startColor, endColor)
            )
        }
    }

    Column(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .onGloballyPositioned { onGloballyPositioned(it) }
            .animateContentSize(animationSpec = tween(durationMillis = 300))
            .background(brush = backgroundBrush)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val iconColor by animateColorAsState(
            targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            animationSpec = tween(durationMillis = 300), label = "iconColor"
        )

        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            modifier = Modifier.padding(horizontal = 4.dp),
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 10.sp
            ),
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewBottomNavigationBar() {
    MaterialTheme {
        BottomNavigationBar(modifier = Modifier, navController = rememberNavController())
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCustomBottomNavigationItem() {
    MaterialTheme {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            CustomBottomNavigationItem(
                icon = Icons.Filled.Home,
                label = "Home",
                selected = true,
                onClick = { }
            )
            CustomBottomNavigationItem(
                icon = Icons.Filled.Search,
                label = "Search",
                selected = false,
                onClick = { }
            )
        }
    }
}