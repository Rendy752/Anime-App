package com.luminoverse.animevibe.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.android.R
import java.util.Locale

private val errorImages = listOf(
    R.drawable.boy_1_sad,
    R.drawable.boy_2_sad,
    R.drawable.boy_3_mad,
    R.drawable.boy_4_confuse,
    R.drawable.girl_1_sad,
    R.drawable.girl_2_sad,
    R.drawable.girl_3_mad,
    R.drawable.girl_4_confuse,
)

/**
 * A composable that displays a random image, a main message, and an optional suggestion text.
 * Ideal for empty states or error messages.
 *
 * @param modifier The modifier to be applied to the Column.
 * @param message The main message to be displayed as a header.
 * @param suggestion An optional, smaller message to be displayed below the main message.
 */
@Composable
fun SomethingWentWrongDisplay(
    modifier: Modifier = Modifier,
    message: String,
    suggestion: String? = null
) {
    val formattedMessage = remember(message) {
        message.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }
    }

    val randomImageResId = remember { errorImages.random() }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = painterResource(id = randomImageResId),
            contentDescription = message,
            modifier = Modifier.size(200.dp)
        )

        Text(
            text = formattedMessage,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        suggestion?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}