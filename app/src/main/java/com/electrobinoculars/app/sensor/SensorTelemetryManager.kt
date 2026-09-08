package com.electrobinoculars.app.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import com.electrobinoculars.app.data.SensorTelemetry

private const val TAG = "SensorTelemetryManager"

/**
 * Manages device attitude, orientation, and motion sensors for the Electrobinoculars HUD.
 * Remaps sensor coordinates for fixed landscape viewing and provides real-time compass,
 * pitch/roll horizon telemetry, and stadiametric range estimation.
 *
 * Applies EMA (Exponential Moving Average) filters and a dead-zone filter to produce
 * smooth, stable telemetry readings suitable for rangefinder distance computation.
 */
class SensorTelemetryManager(
    private val context: Context,
    private val onTelemetryUpdated: (SensorTelemetry) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager

    private var rotationVectorSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var magnetometerSensor: Sensor? = null

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private var hasAccelerometer = false
    private var hasMagnetometer = false

    private val rotationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var isListening = false

    private var lastAzimuthDeg: Float = 0.0f
    private var lastPitchDeg: Float = -5.0f // default slight tilt to produce realistic initial distance
    private var lastRollDeg: Float = 0.0f

    private var simulatedAzimuth: Float? = null
    private var simulatedPitch: Float? = null

    // Signal processing filters for smooth, stable readings
    // Pitch: α=0.12 for extra smoothing since it drives rangefinder math
    private val pitchFilter = ExponentialMovingAverage(alpha = 0.12f)
    // Roll: α=0.15 standard smoothing for horizon ladder display
    private val rollFilter = ExponentialMovingAverage(alpha = 0.15f)
    // Azimuth: circular EMA to handle 0°/360° wrap correctly
    private val azimuthFilter = CircularExponentialMovingAverage(alpha = 0.15f)
    // Dead-zone filter for pitch: suppresses jitter when near-horizontal
    private val pitchDeadZone = DeadZoneFilter(deadZoneThreshold = 1.5f, hysteresis = 0.5f)

    init {
        rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    /**
     * Sets a manual simulated heading in degrees [0, 359].
     */
    fun setSimulatedHeading(headingDeg: Float) {
        simulatedAzimuth = ((headingDeg % 360f) + 360f) % 360f
        emitCurrentState()
    }

    /**
     * Adjusts the simulated heading by a delta angle in degrees.
     */
    fun adjustSimulatedHeading(deltaDeg: Float) {
        val current = simulatedAzimuth ?: lastAzimuthDeg
        setSimulatedHeading(current + deltaDeg)
    }

    /**
     * Sets a manual simulated pitch in degrees [-85, 85].
     */
    fun setSimulatedPitch(pitchDeg: Float) {
        simulatedPitch = pitchDeg.coerceIn(-85f, 85f)
        emitCurrentState()
    }

    /**
     * Adjusts the simulated pitch by a delta angle in degrees.
     */
    fun adjustSimulatedPitch(deltaDeg: Float) {
        val current = simulatedPitch ?: lastPitchDeg
        setSimulatedPitch(current + deltaDeg)
    }

    /**
     * Resets simulated overrides back to hardware sensor readings.
     */
    fun resetSimulation() {
        simulatedAzimuth = null
        simulatedPitch = null
        emitCurrentState()
    }

    /**
     * Starts listening to motion and orientation sensors.
     */
    fun startListening() {
        if (isListening) return
        if (sensorManager == null) {
            onTelemetryUpdated(SensorTelemetry(isAvailable = false))
            return
        }

        var registered = false
        rotationVectorSensor?.let { sensor ->
            registered = sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_GAME
            )
        }

        if (!registered) {
            // Fallback to accelerometer + magnetometer combo
            val accOk = accelerometerSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            } ?: false
            val magOk = magnetometerSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            } ?: false
            registered = accOk && magOk
        }

        if (!registered) {
            onTelemetryUpdated(SensorTelemetry(isAvailable = false))
        }

        isListening = true
        emitCurrentState()
    }

    /**
     * Stops sensor listening to conserve battery and avoid leaks.
     */
    fun stopListening() {
        if (!isListening || sensorManager == null) return
        sensorManager.unregisterListener(this)
        isListening = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                computeAndEmitOrientation()
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerReading, 0, 3)
                hasAccelerometer = true
                if (hasMagnetometer) {
                    if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)) {
                        computeAndEmitOrientation()
                    }
                }
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerReading, 0, 3)
                hasMagnetometer = true
                if (hasAccelerometer) {
                    if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)) {
                        computeAndEmitOrientation()
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op for attitude telemetry
    }

    private fun computeAndEmitOrientation() {
        // Remap coordinates for Landscape orientation
        // In landscape (standard tablet or phone turned 90 deg counterclockwise),
        // device X is pointing towards user bottom, device Y is pointing right.
        val displayRotation = windowManager?.defaultDisplay?.rotation ?: Surface.ROTATION_90
        when (displayRotation) {
            Surface.ROTATION_0 -> {
                // Portrait
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_X,
                    SensorManager.AXIS_Y,
                    remappedMatrix
                )
            }
            Surface.ROTATION_90 -> {
                // Landscape default (rotated counter-clockwise)
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_Y,
                    SensorManager.AXIS_MINUS_X,
                    remappedMatrix
                )
            }
            Surface.ROTATION_180 -> {
                // Reverse portrait
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_MINUS_X,
                    SensorManager.AXIS_MINUS_Y,
                    remappedMatrix
                )
            }
            Surface.ROTATION_270 -> {
                // Reverse landscape (rotated clockwise)
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_MINUS_Y,
                    SensorManager.AXIS_X,
                    remappedMatrix
                )
            }
        }

        SensorManager.getOrientation(remappedMatrix, orientationAngles)

        // Azimuth (radians -> degrees 0..359), filtered with circular EMA
        var rawAzimuthDeg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        if (rawAzimuthDeg.isNaN() || rawAzimuthDeg.isInfinite()) {
            rawAzimuthDeg = 0.0f
        } else if (rawAzimuthDeg < 0) {
            rawAzimuthDeg += 360f
        }
        val filteredAzimuth = azimuthFilter.update(rawAzimuthDeg)

        // Pitch (degrees), filtered with EMA then dead-zone
        var rawPitchDeg = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
        if (rawPitchDeg.isNaN() || rawPitchDeg.isInfinite()) {
            rawPitchDeg = 0.0f
        }
        val smoothedPitch = pitchFilter.update(rawPitchDeg)
        val filteredPitch = pitchDeadZone.update(smoothedPitch)

        // Roll (degrees), filtered with EMA
        var rawRollDeg = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
        if (rawRollDeg.isNaN() || rawRollDeg.isInfinite()) {
            rawRollDeg = 0.0f
        }
        val filteredRoll = rollFilter.update(rawRollDeg)

        lastAzimuthDeg = filteredAzimuth
        lastPitchDeg = filteredPitch
        lastRollDeg = filteredRoll

        emitCurrentState()
    }

    /**
     * Emits current state accounting for active simulation overrides or hardware values.
     */
    fun emitCurrentState() {
        val effectiveAzimuth = simulatedAzimuth ?: lastAzimuthDeg
        val effectivePitch = simulatedPitch ?: lastPitchDeg
        val cardinal = RangefinderEngine.getCardinalDirection(effectiveAzimuth)

        // Distance is computed from pitch only — zoom does NOT affect physical distance
        val rangeResult = RangefinderEngine.calculateRange(effectivePitch)

        onTelemetryUpdated(
            SensorTelemetry(
                headingDegrees = effectiveAzimuth,
                cardinalDirection = cardinal,
                pitch = effectivePitch,
                roll = lastRollDeg,
                estimatedDistanceMeters = rangeResult.distanceMeters,
                rangeConfidence = rangeResult.confidence,
                isRangeStable = !pitchDeadZone.isInDeadZone,
                isAvailable = true
            )
        )
    }
}
