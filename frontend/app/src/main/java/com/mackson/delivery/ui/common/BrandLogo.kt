package com.mackson.delivery.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mackson.delivery.R
import com.mackson.delivery.ui.theme.MacksonNavy

/** Brand mark used on auth screens and the role hub — the client's real crest (crossed
 * utensils + basket, navy/gold) and tagline "People caring for people", cropped straight from
 * the Part 1 prototype's own logo slide rather than a recreated placeholder. */
@Composable
fun BrandLogo(modifier: Modifier = Modifier, showWordmark: Boolean = true) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(R.drawable.logo_macksons),
            contentDescription = "Mackson's",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(140.dp).clip(RoundedCornerShape(24.dp))
        )
        if (showWordmark) {
            Text(
                text = "People caring for people",
                color = MacksonNavy,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
