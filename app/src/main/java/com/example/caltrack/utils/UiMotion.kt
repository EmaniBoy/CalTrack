package com.example.caltrack.utils

import android.view.View
import android.view.animation.OvershootInterpolator

fun View.playEntrance(delayMillis: Long = 0L) {
    alpha = 0f
    translationY = 28f
    animate()
        .alpha(1f)
        .translationY(0f)
        .setStartDelay(delayMillis)
        .setDuration(420)
        .setInterpolator(OvershootInterpolator(0.75f))
        .start()
}
