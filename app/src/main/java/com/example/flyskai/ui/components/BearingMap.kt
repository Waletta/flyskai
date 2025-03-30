import com.example.flyskai.R
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.style.BooleanValue
import com.mapbox.maps.extension.compose.style.ColorValue
import com.mapbox.maps.extension.compose.style.DoubleListValue
import com.mapbox.maps.extension.compose.style.DoubleValue
import com.mapbox.maps.extension.compose.style.MapboxStyleComposable
import com.mapbox.maps.extension.compose.style.layers.generated.LineCapValue
import com.mapbox.maps.extension.compose.style.layers.generated.LineJoinValue
import com.mapbox.maps.extension.compose.style.layers.generated.LineLayer
import com.mapbox.maps.extension.compose.style.sources.GeoJSONData
import com.mapbox.maps.extension.compose.style.sources.generated.rememberGeoJsonSourceState
import com.mapbox.maps.extension.compose.style.standard.LightPresetValue
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.interpolate
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.turf.TurfMeasurement
import kotlinx.coroutines.delay

@Composable
fun BearingMap() {
    val context = LocalContext.current
    var progress by remember {
        mutableDoubleStateOf(0.0)
    }
    var routeLine by remember {
        mutableStateOf<LineString?>(null)
    }
    val start = Point.fromLngLat(12.4924, 41.8902) // Rome
    val end = Point.fromLngLat(13.3615, 38.1157)   // Palermo

    var currentPoint by remember { mutableStateOf(start) }
    var currentBearing by remember { mutableDoubleStateOf(0.0) }

    var pointAnnotationManager: PointAnnotationManager? by remember { mutableStateOf(null) }
    var aircraftAnnotation: PointAnnotation? by remember { mutableStateOf(null) }

}


@MapboxStyleComposable
@Composable
public fun NavigationStyle(
    routeLine: LineString?,
    progress: Double,
    lightPreset: LightPresetValue
) {
    val geoJsonSource = rememberGeoJsonSourceState {
        lineMetrics = BooleanValue(true)
    }
    LaunchedEffect(routeLine) {
        routeLine?.let {
            geoJsonSource.data = GeoJSONData(it)
        }
    }
    MapboxStandardStyle(
        topSlot = {
            if (routeLine != null) {
                LineLayer(
                    sourceState = geoJsonSource
                ) {
                    lineTrimOffset = DoubleListValue(listOf(0.0, progress))
                    lineWidth = DoubleValue(
                        interpolate {
                            exponential {
                                literal(1.5)
                            }
                            zoom()
                            stop {
                                literal(10)
                                product(7.0, 1.0)
                            }
                            stop {
                                literal(14.0)
                                product(10.5, 1.0)
                            }
                            stop {
                                literal(16.5)
                                product(15.5, 1.0)
                            }
                            stop {
                                literal(19.0)
                                product(24.0, 1.0)
                            }
                            stop {
                                literal(22.0)
                                product(29.0, 1.0)
                            }
                        }
                    )
                    lineCap = LineCapValue.ROUND
                    lineJoin = LineJoinValue.ROUND
                    lineGradient = ColorValue(
                        interpolate {
                            linear()
                            lineProgress()
                            stop {
                                literal(0)
                                rgba(47.0, 122.0, 198.0, 1.0)
                            }
                            stop {
                                literal(1.0)
                                rgba(47.0, 122.0, 198.0, 1.0)
                            }
                        }
                    )
                }
                LineLayer(
                    sourceState = geoJsonSource
                ) {
                    lineTrimOffset = DoubleListValue(listOf(0.0, progress))
                    lineWidth = DoubleValue(
                        interpolate {
                            exponential {
                                literal(1.5)
                            }
                            zoom()
                            stop {
                                literal(4.0)
                                product(3.0, 1.0)
                            }
                            stop {
                                literal(10.0)
                                product(4.0, 1.0)
                            }
                            stop {
                                literal(13.0)
                                product(6.0, 1.0)
                            }
                            stop {
                                literal(16.0)
                                product(10.0, 1.0)
                            }
                            stop {
                                literal(19.0)
                                product(14.0, 1.0)
                            }
                            stop {
                                literal(22.0)
                                product(18.0, 1.0)
                            }
                        }
                    )
                    lineCap = LineCapValue.ROUND
                    lineJoin = LineJoinValue.ROUND
                    lineGradient = ColorValue(
                        interpolate {
                            linear()
                            lineProgress()
                            // blue
                            stop { literal(0.0); rgb { literal(6); literal(1); literal(255) } }
                            // royal blue
                            stop { literal(0.1); rgb { literal(59); literal(118); literal(227) } }
                            // cyan
                            stop { literal(0.3); rgb { literal(7); literal(238); literal(251) } }
                            // lime
                            stop { literal(0.5); rgb { literal(0); literal(255); literal(42) } }
                            // yellow
                            stop { literal(0.7); rgb { literal(255); literal(252); literal(0) } }
                            // red
                            stop { literal(1.0); rgb { literal(255); literal(30); literal(0) } }
                        }
                    )
                }
            }
        }
    ) {
        this.lightPreset = lightPreset
    }
}