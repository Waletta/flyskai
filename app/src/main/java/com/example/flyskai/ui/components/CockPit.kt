package com.example.flyskai.ui.components

import android.graphics.Typeface
import com.example.flyskai.R
import com.example.flyskai.AppSettings
import java.util.Locale

import android.media.MediaPlayer
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontFamily
import com.example.flyskai.AltitudeUnit
import model.FlightPhase
import kotlin.math.abs
import androidx.core.graphics.toColorInt

@Composable
fun CockPit(
    track: Float,
    trueHeading: Float,
    magneticHeading: Float,
    speed: Float,
    altitude: Float,
    verticalSpeed: Float,
    flightPhase: FlightPhase,
    pitch: Float = 0f,
    bank: Float = 0f,
    northUpMode: Boolean,
    course: Float = 0f,
) {

    val drift = ((track - magneticHeading + 360) % 360).let {
        if (it > 180) it - 360 else it
    }

    val speedUnit = maxOf(0, formatSpeed(speed))
    val altitudeUnit = maxOf(0, formatAltitude(altitude))

    BankPitchWarningOverlay(bankAngle = bank, pitchAngle = pitch)
    Box(
        modifier = Modifier
            .background(Color.Black)
            .height(240.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Speed Tape
            SpeedAltitudeTape(
                value = speedUnit,
                label = AppSettings.speedUnit.toString(),
                tapeRange = 5,
                highlightColor = Color.Magenta
            )

            // Compass Rose
            Box(modifier = Modifier
                .fillMaxWidth(0.6f)
                .aspectRatio(1f)
                .padding(bottom = 16.dp)) {
                HeadingTrackCompass(track, trueHeading, magneticHeading, pitch, bank, drift, verticalSpeed, false, course)
            }

            // Altitude Tape
            SpeedAltitudeTape(
                value = altitudeUnit,
                label = AppSettings.altitudeUnit.toString(),
                tapeRange = 5,
                highlightColor = Color.Magenta
            )
        }

    }
}

@Composable
fun HeadingTrackCompass(
    track: Float,
    trueHeading: Float,
    magneticHeading: Float,
    pitch: Float,
    bank: Float,
    drift: Float,
    verticalSpeed: Float,
    northUp: Boolean,
    course: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 12f

        val labelPaint = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.LEFT
            isFakeBoldText = true
            textSize = 34f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0f
        }

        // ARTIFICIAL HORIZON BACKGROUND
        rotate(-bank, pivot = center) {
            val pitchOffset = pitch * 3  // scale pitch visually
            drawRect(
                color = Color(0xFF4FC3F7), // Sky blue
                topLeft = Offset(0f, center.y - radius + pitchOffset),
                size = size.copy(height = radius + pitchOffset)
            )
            drawRect(
                color = Color(0xFF795548), // Brown for ground
                topLeft = Offset(0f, center.y + pitchOffset),
                size = size.copy(height = radius - pitchOffset)
            )
            drawLine(
                color = Color.White,
                start = Offset(0f, center.y + pitchOffset),
                end = Offset(size.width, center.y + pitchOffset),
                strokeWidth = 4f
            )
        }

        // COMPASS ROSE
        rotate(-magneticHeading, pivot = center) {
            drawCircle(color = Color.DarkGray, center = center, radius = radius)
            val directions = listOf(
                "N" to 0f,
                "E" to 90f,
                "S" to 180f,
                "W" to 270f,
            )
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 32f
                isFakeBoldText = true
            }

            drawContext.canvas.nativeCanvas.apply {
                directions.forEach { (label, angleDeg) ->
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val labelRadius = radius - 10f
                    val x = center.x + (labelRadius * cos(angleRad)).toFloat()
                    val y = center.y + (labelRadius * sin(angleRad)).toFloat() + 10f
                    drawText(label, x, y, textPaint)
                }
            }
        }

        // TRACK ARROW
        rotate(track - magneticHeading, pivot = center) {
            drawLine(
                color = Color.Green,
                start = center,
                end = Offset(center.x, center.y - radius),
                strokeWidth = 6f
            )
        }

        // COURSE TO DESTINATION ARROW
        rotate(course - magneticHeading, pivot = center) {
            drawLine(
                color = Color(0xFFFFA500), // Orange
                start = center,
                end = Offset(center.x, center.y - radius),
                strokeWidth = 6f
            )
        }

        // HDG ARROW (centered)
        drawLine(
            color = Color.Cyan,
            start = Offset(center.x, center.y),
            end = Offset(center.x, center.y - radius),
            strokeWidth = 6f
        )

        // FIXED AIRCRAFT SYMBOL
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

        drawContext.canvas.nativeCanvas.apply {
            val labelSpacing = 36f
            val verticalStart = center.y + radius + 60f
            val rightX = center.x + radius - 60f


            // DRIFT (Left Top)
            labelPaint.color = android.graphics.Color.MAGENTA
            val driftLabel = if (drift >= 0f)
                String.format(Locale.US,"DRIFT %03.0f° R", drift)
            else
                String.format(Locale.US,"DRIFT %03.0f° L", -drift)
            drawText(
                driftLabel,
                8f,
                center.y + radius + 60f,
                labelPaint
            )

            // VSI (Left Bottom)
            val vsi = formatVerticalSpeed(verticalSpeed)
            val vsiLabel = if (AppSettings.altitudeUnit == AltitudeUnit.FEET) "ft/min" else "m/min"
            val paddedVsi = String.format(Locale.US,"%04d", abs(vsi))
            val signedVsi = if (vsi >= 0) "+$paddedVsi" else "-$paddedVsi"
            val formattedVsi = "VSI   $signedVsi $vsiLabel"
            labelPaint.color = android.graphics.Color.YELLOW
            drawText(
                formattedVsi,
                8f,
                center.y + radius + 96f,
                labelPaint
            )


            // BANK ANGLE (Right Top — replaces TH)
            labelPaint.color = android.graphics.Color.WHITE
            drawText(
                String.format(Locale.US, "BA %03.0f°", bank),
                rightX,
                verticalStart,
                labelPaint
            )

            // MAGNETIC HEADING
            labelPaint.color = android.graphics.Color.GREEN
            drawText(
                "MH %.0f°".format(magneticHeading),
                rightX,
                verticalStart + labelSpacing,
                labelPaint
            )

            labelPaint.color = "#FFA500".toColorInt()
            drawText(
                "CRS %.0f°".format(course),
                rightX,
                verticalStart + labelSpacing * 2,
                labelPaint
            )

            // BANK ANGLE
//            labelPaint.color = android.graphics.Color.LTGRAY
//            val bankText = "${bank.toInt()}°"
//            val textYOffset = center.y - radius - 80f
//            val textXOffset = 80f
//            when {
//                bank > 5f -> drawText(bankText, center.x + textXOffset, textYOffset, labelPaint)
//                bank < -5f -> drawText(bankText, center.x - textXOffset, textYOffset, labelPaint)
//            }
        }
    }
}

@Composable
fun FlashingWarningText(text: String) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500), // 0.5s per cycle
            repeatMode = RepeatMode.Reverse
        )
    )

    Text(
        text = text,
        color = Color.Yellow,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(Color.Red.copy(alpha = alpha))
            .padding(8.dp)
    )
}


@Composable
fun BankPitchWarningOverlay(bankAngle: Float, pitchAngle: Float) {
    val context = LocalContext.current
    val soundPlayed = remember { mutableStateOf(false) }

    val bankExceeded = bankAngle > 30f || bankAngle < -30f
    val pitchExceeded = pitchAngle > 20f || pitchAngle < -10f

    // Play sound on first entry
    LaunchedEffect(bankExceeded, pitchExceeded) {
        if ((bankExceeded || pitchExceeded) && !soundPlayed.value) {
            val mediaPlayer = MediaPlayer.create(context, R.raw.alert_beep)
            mediaPlayer.start()
            soundPlayed.value = true
        } else if (!bankExceeded && !pitchExceeded) {
            soundPlayed.value = false // Reset when safe
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (bankExceeded) {
            FlashingWarningText("⚠ Bank Angle Exceeded!")
        }

        if (pitchExceeded) {
            FlashingWarningText("⚠ Pitch Angle Exceeded!")
        }
    }
}

fun formatVerticalSpeed(vs: Float): Int {
    return when (AppSettings.altitudeUnit) {
        AltitudeUnit.METERS -> (vs * 60).toInt() // m/s to m/min
        AltitudeUnit.FEET -> (vs * 196.8504).toInt() // m/s to ft/min
    }
}

fun formatSpeed(mps: Float): Int {
    return when (AppSettings.speedUnit) {
        com.example.flyskai.SpeedUnit.KMH -> (mps * 3.6).toInt()
        com.example.flyskai.SpeedUnit.KNOTS -> (mps * 1.94384).toInt()
    }
}

fun formatAltitude(meters: Float): Int {
    return when (AppSettings.altitudeUnit) {
        AltitudeUnit.METERS -> meters.toInt()
        AltitudeUnit.FEET -> (meters * 3.28084).toInt()
    }
}


@Composable
fun SpeedAltitudeTape(
    value: Int,
    label: String,
    tapeRange: Int,
    highlightColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        for (i in tapeRange downTo 1) {
            val v = value - i * 10
            val text = if (v >= 0) "%04d".format(v) else "    "
            Text(
                text = text,
                color = Color.LightGray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Box(
            modifier = Modifier
                .background(highlightColor)
                .padding(4.dp)
        ) {
            Text(
                text = "%04d %s".format(value, label),
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        for (i in 1..tapeRange) {
            val v = value + i * 10
            val text = if (v >= 0) "%04d".format(v) else "    "
            Text(
                text = text,
                color = Color.LightGray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

