package com.example.flyskai.ui.components

import com.example.flyskai.R
import com.example.flyskai.AppSettings

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
import com.example.flyskai.AltitudeUnit
import model.TakeoffPhase


@Composable
fun CockPit(
    track: Float,
    heading: Float,
    speed: Float,
    altitude: Float,
    verticalSpeed: Float,
    takeoffPhase: TakeoffPhase,
    pitch: Float = 0f,
    bank: Float = 0f,
) {
    val drift = ((track - heading + 360) % 360).let {
        if (it > 180) it - 360 else it
    }

    val currentUnitSystem = AppSettings.unitSystem.value
    val speedUnit = maxOf(0, formatSpeed(speed))
    val altitudeUnit = maxOf(0, formatAltitude(altitude))

    BankPitchWarningOverlay(bankAngle = bank, pitchAngle = pitch)
    Box(
        modifier = Modifier
            .background(Color.Black)
            .height(240.dp)
            .fillMaxWidth()
    ) {
            Text(
                text = "Phase: ${takeoffPhase.name}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(10.dp)
            )

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

            // Compass + Track
            Box(modifier = Modifier
                .fillMaxWidth(0.5f)
                .aspectRatio(1f)
                .padding(bottom = 16.dp)) {
                HeadingTrackCompass(track, heading, pitch, bank, drift, verticalSpeed)
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
fun SpeedAltitudeTape(
    value: Int,
    label: String,
    tapeRange: Int,
    highlightColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
    ) {
        for (i in tapeRange downTo 1) {
            val v = value - i * 10
            Text(
                text = "$v",
                color = Color.LightGray,
                fontSize = 10.sp
            )
        }

        Box(
            modifier = Modifier
                .background(highlightColor)
                .padding(4.dp)
        ) {
            Text(
                text = "$value $label",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }

        for (i in 1..tapeRange) {
            Text(
                text = "${value + i * 10}",
                color = Color.LightGray,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun HeadingTrackCompass(track: Float, heading: Float, pitch: Float, bank: Float, drift: Float, verticalSpeed: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 12f

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
        rotate(-heading, pivot = center) {
            drawCircle(color = Color.DarkGray, center = center, radius = radius)

            val directions = listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f)
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
                    val x = center.x + (labelRadius * cos(angleRad)).toFloat()
                    val y = center.y + (labelRadius * sin(angleRad)).toFloat() + 10f
                    drawText(label, x, y, textPaint)
                }
            }
        }

     //    TRACK ARROW
        rotate(track - heading, pivot = center) {
            drawLine(
                color = Color.Green,
                start = center,
                end = Offset(center.x, center.y - radius),
                strokeWidth = 6f
            )
        }

        // HDG ARROW
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

        // PITCH + BANK LABELS
        val labelPaint = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            textSize = 32f
        }

        drawContext.canvas.nativeCanvas.apply {
//            labelPaint.color = android.graphics.Color.CYAN
//            drawText("HDG ${heading.toInt()}°", center.x, center.y + radius + 40f, labelPaint)
//
//            labelPaint.color = android.graphics.Color.GREEN
//            drawText("TRK [${track.toInt()}] MAG", center.x, center.y + radius + 80f, labelPaint)

            labelPaint.color = android.graphics.Color.MAGENTA
            drawText(
                "DRIFT ${if (drift >= 0) "+%.0f° R" else "%.0f° L".format(-drift)}".format(drift),
                center.x,
                center.y + radius + 50f,
                labelPaint
            )

            // VSI Calculation
            val vsi = formatVerticalSpeed(verticalSpeed)
            val vsiLabel = if (AppSettings.altitudeUnit == AltitudeUnit.FEET) "ft/min" else "m/min"

            labelPaint.color = android.graphics.Color.YELLOW
            drawText(
                "VSI ${if (vsi >= 0) "+" else ""}${vsi} $vsiLabel",
                center.x,
                center.y + radius + 90f,
                labelPaint
            )

//            labelPaint.color = android.graphics.Color.YELLOW
//            drawText("PITCH ${pitch.toInt()}°", center.x, center.y - radius - 40f, labelPaint)
//
//            labelPaint.color = android.graphics.Color.LTGRAY
//            drawText("BANK ${bank.toInt()}°", center.x, center.y - radius - 80f, labelPaint)
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


