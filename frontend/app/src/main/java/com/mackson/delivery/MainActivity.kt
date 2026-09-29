package com.mackson.delivery

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mackson.delivery.ui.navigation.MacksonNavHost
import com.mackson.delivery.ui.theme.MacksonDeliveryTheme

class MainActivity : ComponentActivity() {
    @androidx.camera.core.ExperimentalGetImage
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // A gentle zoom-and-fade exit on top of the platform's built-in splash entrance —
        // makes the crest feel like it's "landing" rather than the screen just abruptly
        // switching over.
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val zoomX = ObjectAnimator.ofFloat(splashScreenView.iconView, "scaleX", 1f, 1.15f, 0.85f)
            val zoomY = ObjectAnimator.ofFloat(splashScreenView.iconView, "scaleY", 1f, 1.15f, 0.85f)
            val fade = ObjectAnimator.ofFloat(splashScreenView.view, "alpha", 1f, 0f).apply {
                startDelay = 250L
                duration = 250L
            }
            AnimatorSet().apply {
                playTogether(zoomX.apply { duration = 400L }, zoomY.apply { duration = 400L }, fade)
                interpolator = AnticipateInterpolator()
                doOnEnd { splashScreenView.remove() }
                start()
            }
        }

        enableEdgeToEdge()
        setContent {
            MacksonDeliveryTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MacksonNavHost()
                }
            }
        }
    }
}
