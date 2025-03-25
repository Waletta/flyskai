package com.example.flyskai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*

@Composable
fun ScrollingTape(value: Int, label: String, unit: String) {
    val visibleRange = (-2..2)
    Column(
        modifier = Modifier
            .width(80.dp)
            .background(Color.Black, shape = RoundedCornerShape(8.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, color = Color.Gray, fontSize = 12.sp)

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.height(120.dp),
        ) {
            visibleRange.forEach { offset ->
                val displayValue = value + (offset * 10)
                val isCurrent = offset == 0
                Text(
                    text = "$displayValue",
                    fontSize = if (isCurrent) 20.sp else 14.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) Color.Cyan else Color.LightGray,
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .background(if (isCurrent) Color.DarkGray else Color.Transparent)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        Text(text = unit, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun CockpitPFD(speed: Int, altitude: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ScrollingTape(value = speed, label = "SPD", unit = "KT")
        Spacer(Modifier.width(16.dp))
        ScrollingTape(value = altitude, label = "ALT", unit = "FT")
    }
}
