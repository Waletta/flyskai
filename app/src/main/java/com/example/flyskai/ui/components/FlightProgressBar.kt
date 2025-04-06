package com.example.flyskai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import model.FlightPhase

@Composable
fun FlightProgressStepper(currentPhase: FlightPhase) {
    val phases = FlightPhase.entries.toTypedArray()
    Column(modifier = Modifier.fillMaxWidth()) {
        phases.forEach { phase ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (phase == currentPhase) Color.Green else Color.DarkGray)
                    .border(1.dp, Color.Black)
                    .padding(8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = phase.name.replace("_", " "),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = if (phase == currentPhase) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
