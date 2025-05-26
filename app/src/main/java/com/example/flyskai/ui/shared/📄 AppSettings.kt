package com.example.flyskai.ui.shared

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.MutableState

enum class UnitSystem {
    METRIC, IMPERIAL;
    fun toggle(): UnitSystem = if (this == METRIC) IMPERIAL else METRIC
}
object AppSettings {
    val unitSystem: MutableState<UnitSystem> = mutableStateOf(UnitSystem.IMPERIAL)

    val speedUnit: SpeedUnit
        get() = if (unitSystem.value == UnitSystem.METRIC) SpeedUnit.KMH else SpeedUnit.KNOTS

    val altitudeUnit: AltitudeUnit
        get() = if (unitSystem.value == UnitSystem.METRIC) AltitudeUnit.METERS else AltitudeUnit.FEET

    val vsiUnit: VSIUnit
        get() = if (unitSystem.value == UnitSystem.METRIC) VSIUnit.MPM else VSIUnit.FPM

    val distanceUnit: DistanceUnit
        get() = if (unitSystem.value == UnitSystem.METRIC) DistanceUnit.KM else DistanceUnit.NM


    fun toggleUnitSystem() {
        unitSystem.value = unitSystem.value.toggle()
    }
}

enum class DistanceUnit {
    KM, NM;
    override fun toString() = if (this == KM) "km" else "nm"
}

enum class VSIUnit {
    MPM, FPM;
    override fun toString() = if (this == MPM) "m/min" else "ft/min"
}

enum class SpeedUnit {
    KMH, KNOTS;
    override fun toString() = if (this == KMH) "km/h" else "kt"
}

enum class AltitudeUnit {
    METERS, FEET;
    override fun toString() = if (this == METERS) "m" else "ft"
}
