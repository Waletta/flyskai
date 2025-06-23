package com.example.flyskai.ui.components

import java.util.Locale
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.graphics.toColorInt
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import com.example.flyskai.ui.shared.AltitudeUnit
import java.lang.Math.toRadians
import kotlin.math.abs
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import com.example.flyskai.ui.shared.SpeedUnit
import model.Airport
import com.example.flyskai.ui.shared.AppSettings
import com.example.flyskai.ui.shared.CockPitViewModel
import com.example.flyskai.ui.shared.DistanceUnit
import com.example.flyskai.ui.shared.VSIUnit
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CockPit(cpVm: CockPitViewModel,
    ) {
    val acState by cpVm.acState.collectAsStateWithLifecycle()
    val displaySpeed = formatSpeed(acState.speed)
    val displayAltitude = formatAltitude(acState.altitude.toFloat())
    val destAirport by cpVm.destinationAirport.collectAsState()
    val distanceToDestination = acState.destinationDistance
    val course = acState.courseToDest
    val pitch = acState.pitch
    val bank = acState.bank
    val speed = acState.speed
    val track = acState.courseOverGround
    val trueHeading = acState.trueHeading
    val magneticHeading = acState.magneticHeading
    val verticalSpeed = acState.verticalSpeed
    val eta = acState.eta
    val magneticFieldStrength = acState.magneticFieldMagnitude

    val drift: Float? = if (speed > 60f) {
        ((track - trueHeading + 360) % 360).let {
            if (it > 180) it - 360 else it
        }
    } else {
        null
    }

    val isCalibrating by cpVm.isCalibrating.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .background(Color(0xFF303030))
            .height(325.dp)
            .fillMaxWidth()
    ) {

            Column(modifier = Modifier.fillMaxSize()) { // Use Column to stack elements vertically

                if (destAirport != null) {
                    DestinationInfoBar(
                        destinationAirport = destAirport,
                        distanceToDestination = distanceToDestination,
                        eta = eta,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                            .background(Color.Black)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), // This makes the row take up remaining space
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SpeedTapeAirbus(
                        modifier = Modifier.fillMaxHeight(),
                        value = displaySpeed,
                        speedUnit = AppSettings.speedUnit,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .padding(bottom = 12.dp)
                    ) {
                        HeadingTrackCompass(
                            track = track,
                            magneticHeading = magneticHeading,
                            pitch = pitch,
                            bank = bank,
                            drift = drift,
                            verticalSpeed = verticalSpeed,
                            magneticFieldStrength = magneticFieldStrength,
                            course = course,
                            destinationAirport = destAirport,
                            isCalibrating = isCalibrating,
                        )
                    }

                    AltitudeTapeAirbus(
                        modifier = Modifier.fillMaxHeight(),
                        altitude = displayAltitude,
                        altitudeUnit = AppSettings.altitudeUnit,
                    )
                }
            }
        }
    }


@Composable
fun DestinationInfoBar(
    destinationAirport: Airport?,
    distanceToDestination: Double,
    eta: Long?,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val textPxSize = remember(density) { with(density) { 10.sp.toPx() } }

    val labelPaintOrange = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.LEFT
            isFakeBoldText = true
            textSize = textPxSize
            typeface = Typeface.MONOSPACE
            letterSpacing = 0f
            color = "#FFA500".toColorInt() // Orange color
        }
    }

    val labelPaintWhiteRight = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.RIGHT
            textSize = textPxSize
            isFakeBoldText = true // Ensure consistency if other paints have it
            typeface = Typeface.MONOSPACE // Ensure consistency
            letterSpacing = 0f // Ensure consistency
        }
    }


    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {

        val airportName = destinationAirport?.name
        val distance = formatDistance(distanceToDestination)
        val distanceStr = String.format(Locale.US, "%.0f %s", distance, AppSettings.distanceUnit.toString())
        val destinationLabel = if (airportName != null) {
            "DST  ${airportName.uppercase()}  $distanceStr"
        } else {
            "DST  ---"
        }

        Canvas(modifier = Modifier.weight(1f).height(IntrinsicSize.Min)) {
            drawContext.canvas.nativeCanvas.drawText(destinationLabel, 0f, textPxSize, labelPaintOrange)
        }

        val etaStr = eta?.let {
            Instant.ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        } ?: "N/A"
        val etaLabel = "ETA  $etaStr"

        Canvas(modifier = Modifier.weight(1f).height(IntrinsicSize.Min)) {
            drawContext.canvas.nativeCanvas.drawText(etaLabel, size.width, textPxSize, labelPaintWhiteRight)
        }
    }
}

@Composable
fun HeadingTrackCompass(
    track: Float,
    magneticHeading: Float,
    pitch: Float,
    bank: Float,
    drift: Float?,
    verticalSpeed: Float,
    magneticFieldStrength: Float, // in µT
    course: Float?,
    destinationAirport: Airport?,
    isCalibrating: Boolean,
) {

    val tapeColor = Color(0xFF303030)
    val driftAlpha = if (drift != null) 1f else 0.3f

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = tapeColor)
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

        val pitchOffset = pitch * -7
        // ARTIFICIAL HORIZON BACKGROUND
        drawRect(
            color = Color(0xFF87CEEB), // Sky
            topLeft = Offset.Zero,
            size = Size(width = size.width, height = center.y + pitchOffset)
        )
        drawRect(
            color = Color(0xFF8B4513), // Ground
            topLeft = Offset(x = 0f, y = center.y + pitchOffset),
            size = Size(width = size.width, height = size.height - (center.y + pitchOffset))
        )
        drawLine(
            color = Color.White,
            start = Offset(x = 0f, y = center.y + pitchOffset),
            end = Offset(x = size.width, y = center.y + pitchOffset),
            strokeWidth = 4f
        )

        // Pitch ladders
        val pixelsPerDegree = 7f

        for (pitchAngle in -30..30 step 5) {
            if (pitchAngle == 0) continue
            val yOffset = -pitchAngle * pixelsPerDegree

            val dynamicLineWidth = when (abs(pitchAngle)) {
                30 -> 100f
                20 -> 80f
                10 -> 60f
                else -> 40f
            }
            drawLine(
                color = Color.White,
                start = Offset(center.x - dynamicLineWidth / 2, center.y + yOffset + pitchOffset),
                end = Offset(center.x + dynamicLineWidth / 2, center.y + yOffset + pitchOffset),
                strokeWidth = 2f
            )
            if (pitchAngle % 10 == 0) {
                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        pitchAngle.toString(),
                        center.x - dynamicLineWidth / 2 - 20f,
                        center.y + yOffset + pitchOffset + 5f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 8.sp.toPx()
                        }
                    )
                    drawText(
                        pitchAngle.toString(),
                        center.x + dynamicLineWidth / 2 + 20f,
                        center.y + yOffset + pitchOffset + 5f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 8.sp.toPx()
                        }
                    )
                }
            }
        }

            // COMPASS ROSE
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

                if (angle % 30 == 0 && angle != 0 && angle != 360) {
                    val (label, fontSize) = when (angle) {
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


        // TRACK ARROW
        rotate(track, pivot = center) {
            drawLine(color = Color.Green, start = center, end = Offset(center.x, center.y - radius), strokeWidth = 6f)
        }

        // COURSE TO DESTINATION
        if (destinationAirport != null) {
            rotate(course ?: 0f, pivot = center) {
                drawLine(
                    color = Color(0xFFFFA500),
                    start = center,
                    end = Offset(center.x, center.y - radius),
                    strokeWidth = 6f
                )
            }
        }

        // AIRCRAFT SYMBOL
        rotate(magneticHeading, pivot = center) {
            val triangleSize = 30f
            drawLine(
                color = Color.Yellow,
                start = Offset(center.x - triangleSize, center.y + triangleSize),
                end = center,
                strokeWidth = 6f
            )
            drawLine(
                color = Color.Yellow,
                start = Offset(center.x + triangleSize, center.y + triangleSize),
                end = center,
                strokeWidth = 6f
            )
            drawLine(
                color = Color.Yellow,
                start = Offset(center.x, center.y + triangleSize),
                end = Offset(center.x, center.y + triangleSize + 15f),
                strokeWidth = 4f
            )
        }

        // NORTH LABEL
        val rad = toRadians(-90.0) // North at 0/360 degrees
        val textOffset = Offset(
            x = center.x + (radius - 25f) * cos(rad).toFloat(),
            y = center.y + (radius - 25f) * sin(rad).toFloat()
        )
        drawContext.canvas.nativeCanvas.apply {
            drawText(
                "N",
                textOffset.x,
                textOffset.y,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = cardinalFontSize.toPx()
                    isFakeBoldText = true
                }
            )
        }

        drawContext.canvas.nativeCanvas.apply {
            val verticalStart = center.y + radius + 60f
            val rightX = center.x + radius - 200f
            labelPaint.alpha = (driftAlpha * 255).toInt()

            val driftLabel = if (drift != null) {
                if (drift >= 0f)
                    String.format(Locale.US, "DRIFT %03.0f° R", drift)
                else
                    String.format(Locale.US, "DRIFT %03.0f° L", -drift)
            } else {
                "DRIFT ---"
            }

            labelPaint.color = android.graphics.Color.WHITE
            drawText(driftLabel, 8f, verticalStart, labelPaint)
            labelPaint.alpha = 255
            val vsi = formatVSI(verticalSpeed)
            val vsiLabel = if (AppSettings.altitudeUnit == AltitudeUnit.FEET) "fpm" else "mpm"
            val paddedVsi = String.format(Locale.US,"%04d", abs(vsi))
            val signedVsi = if (vsi >= 0) "+$paddedVsi" else "-$paddedVsi"
            val formattedVsi = "VSI   $signedVsi $vsiLabel"
            labelPaint.color = android.graphics.Color.WHITE
            drawText(
                formattedVsi,
                8f,
                center.y + radius + 96f,
                labelPaint
            )

            labelPaint.color = when {
                magneticFieldStrength > 500f -> android.graphics.Color.RED
                magneticFieldStrength >= 200f -> android.graphics.Color.rgb(255, 165, 0) // Orange
                else -> android.graphics.Color.WHITE
            }
            val magneticFieldText = String.format(Locale.US, "MAG %.1f µT", magneticFieldStrength)
                drawText(magneticFieldText, 8f, center.y + radius + 132f, labelPaint)

            labelPaint.color = android.graphics.Color.WHITE
            val bankText = if (isCalibrating) "Calibrating..." else String.format(Locale.US, "BA %02.0f°", bank)
            drawText(
                bankText,
                rightX,
                verticalStart,
                labelPaint
            )
            val pitchDisplayColor: Int
            val pitchText: String
            if (isCalibrating) {
                pitchText = "Calibrating..."
                pitchDisplayColor = android.graphics.Color.WHITE
            } else {
                pitchText = String.format(Locale.US, "Pitch %02.0f°", -pitch)
                pitchDisplayColor = when {
                    -pitch >=  20f || -pitch <= -10f -> android.graphics.Color.RED
                    -pitch >=  10f || -pitch <= -6f -> "#FFA500".toColorInt()
                    else -> android.graphics.Color.WHITE
                }
            }
            labelPaint.color = pitchDisplayColor
            drawText(
                pitchText,
                rightX,
                verticalStart + 36f,
                labelPaint
            )

         }
    }
}


fun formatSpeed(mps: Float): Int {
    return when (AppSettings.speedUnit) {
        SpeedUnit.KMH -> (mps * 3.6).toInt()
        SpeedUnit.KNOTS -> (mps * 1.94384).toInt()
    }
}

fun formatDistance(km: Double): Double {
    return when (AppSettings.distanceUnit) {
        DistanceUnit.KM -> km
        DistanceUnit.NM -> km * 0.539957
    }
}

fun formatVSI(mps: Float): Int {
    return when (AppSettings.vsiUnit) {
        VSIUnit.MPM -> (mps * 60).toInt()         // meters per second → meters per minute
        VSIUnit.FPM -> (mps * 196.8504).toInt()    // meters per second → feet per minute
    }
}

fun formatAltitude(meters: Float): Int {
    return when (AppSettings.altitudeUnit) {
        AltitudeUnit.METERS -> meters.toInt()
        AltitudeUnit.FEET -> (meters * 3.28084).toInt()
    }
}

@Composable
fun SpeedTapeAirbus(
    modifier: Modifier = Modifier,
    value: Int,
    speedUnit: SpeedUnit
) {
    val tapeColor = Color(0xFF303030)
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textAlign = android.graphics.Paint.Align.CENTER
        textSize = with(LocalDensity.current) { 10.sp.toPx() }
        isAntiAlias = true
    }

    Canvas(modifier = modifier
        .width(80.dp)
        .fillMaxHeight()
        .background(tapeColor)
    ) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2f, height / 2f)

        val indicatorBoxWidth = width * 0.8f
        val indicatorBoxHeight = 20.dp.toPx()
        val pixelsPerUnit = 4.dp.toPx()

        // Define the static indicator's vertical bounds
        val indicatorTop = center.y - indicatorBoxHeight / 2
        val indicatorBottom = center.y + indicatorBoxHeight / 2


        // 1. Draw the Moving Tape FIRST
        clipRect(
            top = 0f,
            bottom = height,
            left = center.x - indicatorBoxWidth / 2,
            right = center.x + indicatorBoxWidth / 2
        ) {
            val majorTickInterval: Int
            val minorTickInterval: Int
            when (speedUnit) {
                SpeedUnit.KNOTS -> { majorTickInterval = 20; minorTickInterval = 10 }
                SpeedUnit.KMH -> { majorTickInterval = 20; minorTickInterval = 10 }
            }
            val majorTickLength = 20.dp.toPx()
            val minorTickLength = 10.dp.toPx()
            val verticalRange = (height / pixelsPerUnit).toInt()
            val startValue = value - verticalRange / 2
            val endValue = value + verticalRange / 2

            for (currentValue in startValue..endValue) {
                if (currentValue < 0) continue
                val yPos = center.y - (currentValue - value) * pixelsPerUnit

                if (currentValue % majorTickInterval == 0) {
                    // FIX: Only draw the tape's number if it's OUTSIDE the static indicator area
                    if (yPos < indicatorTop || yPos > indicatorBottom) {
                        drawContext.canvas.nativeCanvas.drawText(
                            currentValue.toString(),
                            center.x,
                            yPos + textPaint.textSize / 3,
                            textPaint
                        )
                    }
                    // Always draw the tick mark
                    drawLine(
                        color = Color.White,
                        start = Offset(center.x - indicatorBoxWidth / 2, yPos),
                        end = Offset(center.x - indicatorBoxWidth / 2 + majorTickLength, yPos),
                        strokeWidth = 2.dp.toPx()
                    )
                } else if (currentValue % minorTickInterval == 0) {
                    // Always draw the minor tick mark
                    drawLine(
                        color = Color.White,
                        start = Offset(center.x - indicatorBoxWidth / 2, yPos),
                        end = Offset(center.x - indicatorBoxWidth / 2 + minorTickLength, yPos),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }

        // Overspeed/Underspeed indicators can be drawn here, behind the static box
        // ...

        // 2. Draw the Static Indicator Box and Pointer
        drawRect(
            color = Color.Black,
            topLeft = Offset(center.x - indicatorBoxWidth / 2, indicatorTop),
            size = Size(indicatorBoxWidth, indicatorBoxHeight)
        )
        val indicatorPointerWidth = 15.dp.toPx()
        val pointerPath = Path().apply {
            moveTo(center.x + indicatorBoxWidth / 2, indicatorTop)
            lineTo(center.x + indicatorBoxWidth / 2 - indicatorPointerWidth, center.y)
            lineTo(center.x + indicatorBoxWidth / 2, indicatorBottom)
            close()
        }
        drawPath(pointerPath, color = Color.Yellow)

        // 3. Draw the actual current speed text LAST to ensure it's on top
        drawContext.canvas.nativeCanvas.drawText(
            value.toString(),
            center.x,
            center.y + textPaint.textSize / 3,
            textPaint
        )
    }
}

@Composable
fun AltitudeTapeAirbus(
    modifier: Modifier = Modifier,
    altitude: Int,
    altitudeUnit: AltitudeUnit
) {
    val tapeColor = Color(0xFF303030)
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textAlign = android.graphics.Paint.Align.CENTER
        textSize = with(LocalDensity.current) { 10.sp.toPx() }
        isAntiAlias = true
    }

    Canvas(modifier = modifier
        .width(80.dp)
        .fillMaxHeight()
        .background(tapeColor)
    ) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2f, height / 2f)

        val indicatorBoxWidth = width * 0.8f
        val indicatorBoxHeight = 20.dp.toPx()
        val pixelsPerUnit = 0.4.dp.toPx()

        val indicatorTop = center.y - indicatorBoxHeight / 2
        val indicatorBottom = center.y + indicatorBoxHeight / 2

        // 1. Draw the Moving Tape FIRST
        clipRect(
            top = 0f,
            bottom = height,
            left = center.x - indicatorBoxWidth / 2,
            right = center.x + indicatorBoxWidth / 2
        ) {
            val majorTickInterval: Int
            val minorTickInterval: Int
            when (altitudeUnit) {
                AltitudeUnit.FEET -> { majorTickInterval = 500; minorTickInterval = 100 }
                AltitudeUnit.METERS -> { majorTickInterval = 100; minorTickInterval = 50 }
            }
            val majorTickLength = 20.dp.toPx()
            val minorTickLength = 10.dp.toPx()
            val verticalRange = (height / pixelsPerUnit).toInt()
            val startValue = altitude - verticalRange / 2
            val endValue = altitude + verticalRange / 2

            for (currentValue in startValue..endValue) {
                if (currentValue < 0) continue
                if (currentValue % minorTickInterval != 0) continue
                val yPos = center.y - (currentValue - altitude) * pixelsPerUnit

                if (currentValue % majorTickInterval == 0) {
                    if (yPos < indicatorTop || yPos > indicatorBottom) {
                        drawContext.canvas.nativeCanvas.drawText(
                            currentValue.toString(),
                            center.x,
                            yPos + textPaint.textSize / 3,
                            textPaint
                        )
                    }
                    drawLine(
                        color = Color.White,
                        start = Offset(center.x + indicatorBoxWidth / 2, yPos),
                        end = Offset(center.x + indicatorBoxWidth / 2 - majorTickLength, yPos),
                        strokeWidth = 2.dp.toPx()
                    )
                } else {
                    drawLine(
                        color = Color.White,
                        start = Offset(center.x + indicatorBoxWidth / 2, yPos),
                        end = Offset(center.x + indicatorBoxWidth / 2 - minorTickLength, yPos),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }

        // 2. Draw the Static Indicator Box and Pointer
        drawRect(
            color = Color.Black,
            topLeft = Offset(center.x - indicatorBoxWidth / 2, indicatorTop),
            size = Size(indicatorBoxWidth, indicatorBoxHeight)
        )
        val indicatorPointerWidth = 15.dp.toPx()
        drawLine(
            color = Color.Yellow,
            start = Offset(x = center.x - indicatorBoxWidth / 2 - indicatorPointerWidth, y = center.y),
            end = Offset(x = center.x - indicatorBoxWidth / 2, y = center.y),
            strokeWidth = 6f
        )

        // 3. Draw the actual current altitude text LAST
        drawContext.canvas.nativeCanvas.drawText(
            altitude.toString(),
            center.x,
            center.y + textPaint.textSize / 3,
            textPaint
        )
    }
}