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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CockPit(
    track: Float,
    heading: Float,
    speed: Float,
    altitude: Float
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
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

            Canvas(
                modifier = Modifier
                    .fillMaxWidth(0.35f)
                    .aspectRatio(1f)
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f - 12f

                // Draw compass rose rotating with heading
                rotate(-heading, pivot = center) {
                    drawCircle(color = Color.DarkGray, center = center, radius = radius)

                    // Cardinal labels (rotating)
                    val directions = listOf(
                        "N" to 0f,
                        "NE" to 45f,
                        "E" to 90f,
                        "SE" to 135f,
                        "S" to 180f,
                        "SW" to 225f,
                        "W" to 270f,
                        "NW" to 315f
                    )

                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 28f
                        isFakeBoldText = true
                    }

                    drawContext.canvas.nativeCanvas.apply {
                        directions.forEach { (label, angleDeg) ->
                            val angleRad = Math.toRadians(angleDeg.toDouble())
                            val labelRadius = radius - 10f
                            val x = center.x + (labelRadius * Math.cos(angleRad)).toFloat()
                            val y = center.y + (labelRadius * Math.sin(angleRad)).toFloat() + 10f
                            drawText(label, x, y, textPaint)
                        }
                    }
                }

                // Draw TRACK arrow (independent of heading)
                rotate(track - heading, pivot = center) {
                    drawLine(
                        color = Color.Green,
                        start = center,
                        end = Offset(center.x, center.y - radius),
                        strokeWidth = 6f
                    )
                }

                // Aircraft symbol (fixed)
                val triangleSize = 20f
                drawLine(
                    color = Color.White,
                    start = Offset(center.x - triangleSize, center.y + triangleSize),
                    end = center,
                    strokeWidth = 4f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(center.x + triangleSize, center.y + triangleSize),
                    end = center,
                    strokeWidth = 4f
                )

                // Labels: HDG and TRK
                val labelPaint = android.graphics.Paint().apply {
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                    textSize = 36f
                }

                drawContext.canvas.nativeCanvas.apply {
                    labelPaint.color = android.graphics.Color.CYAN
                    drawText("HDG ${heading.toInt()}°", center.x, center.y + radius + 40f, labelPaint)

                    labelPaint.color = android.graphics.Color.GREEN
                    drawText("TRK ${track.toInt()}°", center.x, center.y + radius + 80f, labelPaint)
                }
            }


        }
    }
}