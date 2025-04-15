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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import com.example.flyskai.AltitudeUnit
import model.FlightPhase
import kotlin.math.abs
import androidx.core.graphics.toColorInt
import model.knownAirports
import java.lang.Math.toRadians
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

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
    destination: String = "",
    currentLat: Double = 0.0,
    currentLon: Double = 0.0,
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
                HeadingTrackCompass(track, trueHeading, magneticHeading, pitch, bank, drift, verticalSpeed, northUpMode, course, destination, currentLat, currentLon)
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
    course: Float,
    destination: String = "",
    currentLat: Double = 0.0,
    currentLon: Double = 0.0,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 12f
        val cardinalFontSize = 12.sp
        val numericFontSize = 7.sp
        val labelPaint = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.LEFT
            isFakeBoldText = true
            textSize = 34f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0f
        }

        // ARTIFICIAL HORIZON BACKGROUND
        // In north-up mode, we rotate the horizon instead of the compass
        val rotationAngle = if (northUp) -bank else -bank
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

        // COMPASS ROSE - Fixed in north-up mode
        val compassRotation = if (northUp) 0f else -magneticHeading
        rotate(compassRotation, pivot = center) {
            for (angle in 0 until 360 step 3) {
                val rad = toRadians((angle - 90).toDouble())
                val start = Offset(
                    center.x + (radius - 10f) * cos(rad).toFloat(),
                    center.y + (radius - 10f) * sin(rad).toFloat()
                )
                val end = Offset(
                    center.x + radius * cos(rad).toFloat(),
                    center.y + radius * sin(rad).toFloat()
                )
                drawLine(
                    color = Color.White,
                    start = start,
                    end = end,
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )

                if (angle % 30 == 0) {
                    val (label, fontSize) = when (angle) {
                        0, 360 -> "N" to cardinalFontSize
                        90 -> "E" to cardinalFontSize
                        180 -> "S" to cardinalFontSize
                        270 -> "W" to cardinalFontSize
                        else -> angle.toString() to numericFontSize
                    }
                    val textOffset = Offset(
                        x = center.x + (radius - 25f) * cos(rad).toFloat(),
                        y = center.y + (radius - 25f) * sin(rad).toFloat()
                    )
                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            label,
                            textOffset.x,
                            textOffset.y,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = fontSize.toPx()
                                isFakeBoldText = true
                            }
                        )
                    }
                }
            }
        }

        // TRACK ARROW - Adjust rotation for north-up mode
        val trackRotation = if (northUp) track else track - magneticHeading
        rotate(trackRotation, pivot = center) {
            drawLine(
                color = Color.Green,
                start = center,
                end = Offset(center.x, center.y - radius),
                strokeWidth = 6f
            )
        }

        // COURSE TO DESTINATION ARROW
        if(destination.isNotBlank()){
            val courseRotation = if (northUp) course else course - magneticHeading
            rotate(courseRotation, pivot = center) {
                drawLine(
                    color = Color(0xFFFFA500),
                    start = center,
                    end = Offset(center.x, center.y - radius),
                    strokeWidth = 6f
                )
            }
        }

        // HEADING ARROW (blue) - Only shown in heading-up mode
        if (!northUp) {
            rotate(magneticHeading, pivot = center) {
                drawLine(
                    color = Color.Cyan,
                    start = center,
                    end = Offset(center.x, center.y - radius),
                    strokeWidth = 6f
                )
            }
        }

        // Drift ARROW
//        rotate(magneticHeading + drift, pivot = center) {
//            drawLine(
//                color = Color.Magenta,
//                start = center,
//                end = Offset(center.x, center.y - radius * 0.7f),
//                strokeWidth = 4f,
//                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
//            )
//        }

        // HDG ARROW (centered)
//        drawLine(
//            color = Color.Cyan,
//            start = Offset(center.x, center.y),
//            end = Offset(center.x, center.y - radius),
//            strokeWidth = 6f
//        )

        // AIRCRAFT SYMBOL - Rotate for north-up mode
        val aircraftRotation = if (northUp) magneticHeading else 0f
        rotate(aircraftRotation, pivot = center) {
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

            // Add small heading indicator at bottom of aircraft symbol
            if (northUp) {
                drawLine(
                    color = Color.Cyan,
                    start = Offset(center.x, center.y + triangleSize),
                    end = Offset(center.x, center.y + triangleSize + 15f),
                    strokeWidth = 3f
                )
            }
        }


        drawContext.canvas.nativeCanvas.apply {
            val verticalStart = center.y + radius + 60f
            val rightX = center.x + radius - 60f

            // DRIFT (Left Top)
            labelPaint.color = android.graphics.Color.RED
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

            // DESTINATION
            if(destination.isNotBlank()){
                labelPaint.color = "#FFA500".toColorInt()
                val destinationAirport = knownAirports.find { it.icao.equals(destination, ignoreCase = true) }
                val airportName = destinationAirport?.name ?: destination.uppercase()

                val distanceKm = destinationAirport?.let {
                    haversine(currentLat, currentLon, it.lat, it.lon)
                } ?: 0.0

                val distanceStr = String.format(Locale.US, "%.0f km", distanceKm)
                val destinationLabel = "DST  ${airportName.uppercase()}  $distanceStr"
                drawText(
                    destinationLabel,
                    8f,
                    center.y + radius + 132f,
                    labelPaint
                )
            }


            // BANK ANGLE (Right Top — replaces TH)
            labelPaint.color = android.graphics.Color.WHITE
            drawText(
                String.format(Locale.US, "BA %03.0f°", bank),
                rightX,
                verticalStart,
                labelPaint
            )

            // MAGNETIC HEADING
//            labelPaint.color = android.graphics.Color.GREEN
//            drawText(
//                "* Heading",
//                rightX,
//                verticalStart + labelSpacing,
//                labelPaint
//            )

//            labelPaint.color = "#FFA500".toColorInt()
//            drawText(
//                "* Course",
//                rightX,
//                verticalStart + labelSpacing * 2,
//                labelPaint
//            )

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

fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0 // Earth radius in km
    val dLat = toRadians(lat2 - lat1)
    val dLon = toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) + cos(toRadians(lat1)) * cos(toRadians(lat2)) * sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c // km to NM
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

