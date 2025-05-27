package com.luminoverse.animevibe.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ImageCardWithContent(
    imageUrl: String?,
    contentBackgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentDescription: String?,
    onItemClick: () -> Unit,
    leftContent: @Composable () -> Unit,
    rightContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clickable { onItemClick() }
    ) {
        AsyncImageWithPlaceholder(
            model = imageUrl ?: "",
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.75f)
                .align(Alignment.CenterEnd),
            roundedCorners = ImageRoundedCorner.NONE,
            isClickable = false
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.75f)
                .align(Alignment.CenterEnd)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            contentBackgroundColor,
                            Color.Transparent
                        ),
                        startX = if (isRtl) Float.POSITIVE_INFINITY else 0f,
                        endX = if (isRtl) 0f else Float.POSITIVE_INFINITY
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.6f)
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = if (isRtl) Alignment.End else Alignment.Start
            ) {
                leftContent()
            }

            rightContent?.let {
                Row(
                    horizontalArrangement = if (isRtl) Arrangement.Start else Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(
                        modifier = Modifier.wrapContentWidth(),
                        horizontalAlignment = if (isRtl) Alignment.Start else Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        it()
                    }
                }
            }
        }
    }
}