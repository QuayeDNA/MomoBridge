package com.momobridge.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.momobridge.ui.theme.MomoColors
import com.momobridge.ui.theme.MomoType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AmountText(
    amount: Double,
    modifier: Modifier = Modifier
) {
    Text(
        text = "GH₵${"%.2f".format(amount)}",
        style = MomoType.AmountMono,
        fontWeight = FontWeight.Bold,
        color = MomoColors.TextPrimary,
        modifier = modifier
    )
}

@Composable
fun TimestampText(
    timestamp: Long,
    verbose: Boolean = false,
    modifier: Modifier = Modifier
) {
    val now = Calendar.getInstance()
    val msgTime = Calendar.getInstance().apply { timeInMillis = timestamp }

    val formatStr = when {
        verbose -> "dd MMM yyyy HH:mm"
        isSameDay(now, msgTime) -> "HH:mm"
        isSameWeek(now, msgTime) -> "EEE HH:mm"
        else -> "dd MMM"
    }

    val fmt = SimpleDateFormat(formatStr, Locale.US)
    Text(
        text = fmt.format(Date(timestamp)),
        style = MomoType.LabelSmall,
        color = MomoColors.TextTertiary,
        modifier = modifier
    )
}

private fun isSameDay(a: Calendar, b: Calendar): Boolean {
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}

private fun isSameWeek(a: Calendar, b: Calendar): Boolean {
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.WEEK_OF_YEAR) == b.get(Calendar.WEEK_OF_YEAR)
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MomoType.TitleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MomoColors.TextPrimary,
        modifier = modifier
    )
}

@Composable
fun SectionSubtext(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MomoType.BodySmall,
        color = MomoColors.TextSecondary,
        modifier = modifier
    )
}
