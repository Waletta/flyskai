package com.example.flyskai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CockPit(
    track: Float,   // GPS bearing
    heading: Float,  // Compass heading
    speed: Float,      // Speed in km/h
    altitude: Float    // Altitude in meters
) {
    val drift = ((track - heading + 360) % 360).let {
        if (it > 180) it - 360 else it
    }
    Box(
        modifier = Modifier
            .background(Color.Black)
            .padding(12.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Track: ${track.toInt()}°",
                color = Color.Green,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Heading: ${heading.toInt()}°",
                color = Color.Cyan,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "WCA: ${drift.toInt()}°",
                color = Color.Yellow,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${speed.toInt()} km/h",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${altitude.toInt()} m",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        }

        // Compass (top right)
        Canvas(
            modifier = Modifier.padding(22.dp)
                .fillMaxWidth(0.22f)
                .aspectRatio(1f)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f - 12f

            drawCircle(color = Color.DarkGray, center = center, radius = radius)

            // Track arrow (Green)
            rotate(track, pivot = center) {
                drawLine(
                    color = Color.Green,
                    start = center,
                    end = Offset(center.x, center.y - radius),
                    strokeWidth = 6f
                )
            }

            // Heading arrow (Cyan)
            rotate(heading, pivot = center) {
                drawLine(
                    color = Color.Cyan,
                    start = center,
                    end = Offset(center.x, center.y - radius * 0.8f),
                    strokeWidth = 4f
                )
            }
        }
    }