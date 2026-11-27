package com.voidglass.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val backdrop = rememberLayerBackdrop()
            Box(Modifier.fillMaxSize().background(Color(0xFF0A0A0F))) {
                Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                    Box(Modifier.offset(40.dp, 120.dp).size(180.dp).background(Color(0xFF6C4DFF), CircleShape))
                    Box(Modifier.offset(200.dp, 380.dp).size(220.dp).background(Color(0xFF00C2FF), CircleShape))
                }
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedCornerShape(32.dp) },
                            effects = {
                                vibrancy()
                                blur(6.dp.toPx())
                                lens(16.dp.toPx(), 32.dp.toPx())
                            }
                        )
                        .padding(horizontal = 44.dp, vertical = 28.dp)
                ) {
                    Text("VoidGlass", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light)
                }
            }
        }
    }
}
