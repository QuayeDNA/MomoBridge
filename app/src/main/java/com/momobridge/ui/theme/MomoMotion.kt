package com.momobridge.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing

object MomoMotion {
    const val InstantMs = 100
    const val FastMs = 200
    const val NormalMs = 350
    const val SlowMs = 500

    val EaseOutExpo: Easing = FastOutLinearInEasing
    val EaseInOutQuart: Easing = FastOutSlowInEasing
    val EaseLinear: Easing = LinearEasing
}
