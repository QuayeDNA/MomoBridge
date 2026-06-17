package com.momobridge.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import kotlin.time.Duration.Companion.milliseconds

object MomoMotion {
    val Instant = 100.milliseconds
    val Fast = 200.milliseconds
    val Normal = 350.milliseconds
    val Slow = 500.milliseconds

    val EaseOutExpo: Easing = FastOutLinearInEasing
    val EaseInOutQuart: Easing = FastOutSlowInEasing
    val EaseLinear: Easing = LinearEasing

    val CardEntrance = tween<Offset>(
        durationMillis = 350, easing = FastOutLinearInEasing
    )
    val StatusTransition = tween<Float>(
        durationMillis = 500, easing = FastOutSlowInEasing
    )
    val FadeIn = tween<Float>(
        durationMillis = 200, easing = LinearEasing
    )
}
