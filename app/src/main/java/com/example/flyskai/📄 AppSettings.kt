package com.example.flyskai

enum class UnitSystem {
    METRIC, IMPERIAL;

    fun toggle(): UnitSystem = if (this == METRIC) IMPERIAL else METRIC
}

object AppSettings {
    var unitSystem: UnitSystem = UnitSystem.METRIC

    val speedUnit: SpeedUnit
        get() = if (unitSystem == UnitSystem.METRIC) SpeedUnit.KMH else SpeedUnit.KNOTS

    val altitudeUnit: AltitudeUnit
        get() = if (unitSystem == UnitSystem.METRIC) AltitudeUnit.METERS else AltitudeUnit.FEET

    fun toggleUnitSystem() {
        unitSystem = unitSystem.toggle()
    }
}

enum class SpeedUnit {
    KMH, KNOTS;
    override fun toString() = if (this == KMH) "k/h" else "kt"
}

enum class AltitudeUnit {
    METERS, FEET;
    override fun toString() = if (this == METERS) "m" else "ft"
}
