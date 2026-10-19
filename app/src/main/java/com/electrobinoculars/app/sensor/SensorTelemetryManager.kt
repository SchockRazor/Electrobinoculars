package com.electrobinoculars.app.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.view.Surface
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

    private var rotationVectorSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var magnetometerSensor: Sensor? = null

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private var hasAccelerometer = false
    private var hasMagnetometer = false

    private val rotationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)

    private var isListening = false

    private var lastAzimuthDeg: Float = 0.0f
    private var lastPitchDeg: Float = -5.0f // default slight tilt to produce realistic initial distance
    private var lastRollDeg: Float = 0.0f

    private var simulatedAzimuth: Float? = null
    private var simulatedPitch: Float? = null
    private var simulatedRoll: Float? = null

    // Signal processing filters for smooth, stable readings
    // Pitch: α=0.12 for extra smoothing since it drives rangefinder math
    private val pitchFilter = ExponentialMovingAverage(alpha = 0.12f)
    // Roll: circular EMA for the horizon-ladder roll, which can wrap at ±180°
    private val rollFilter = CircularExponentialMovingAverage(alpha = 0.15f)
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
     * Sets a manual simulated roll in degrees [-180, 180].
     */
    fun setSimulatedRoll(rollDeg: Float) {
        simulatedRoll = rollDeg.coerceIn(-180f, 180f)
        emitCurrentState()
    }

    /**
     * Adjusts the simulated roll by a delta angle in degrees.
     */
    fun adjustSimulatedRoll(deltaDeg: Float) {
        val current = simulatedRoll ?: lastRollDeg
        setSimulatedRoll(current + deltaDeg)
    }

    /**
     * Resets simulated overrides back to hardware sensor readings.
     */
    fun resetSimulation() {
        simulatedAzimuth = null
        simulatedPitch = null
        simulatedRoll = null
        pitchFilter.reset()
        rollFilter.reset()
        azimuthFilter.reset()
        pitchDeadZone.reset()
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

    private fun getDisplayRotation(): Int {
        return try {
            context.display?.rotation
        } catch (_: Exception) {
            null
        } ?: Surface.ROTATION_90
    }

    private fun computeAndEmitOrientation() {
        // Remap coordinates for Landscape orientation
        // In landscape (standard tablet or phone turned 90 deg counterclockwise),
        // device X is pointing towards user bottom, device Y is pointing right.
        val displayRotation = getDisplayRotation()
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

        // Camera-frame attitude derived directly from rotation matrix columns
        val cameraAttitude = CameraAttitude.fromRotationMatrix(remappedMatrix)

        // Azimuth derived from camera optical axis, filtered with circular EMA
        val filteredAzimuth = azimuthFilter.update(cameraAttitude.azimuthDegrees)

        // Pitch (camera line of sight: 0° level, negative = down), EMA then dead-zone
        val smoothedPitch = pitchFilter.update(cameraAttitude.pitchDegrees)
        val filteredPitch = pitchDeadZone.update(smoothedPitch)

        // Roll (about the optical axis), circular EMA normalized back to [-180°, 180°)
        val filteredRoll = rollFilter.update(cameraAttitude.rollDegrees)
            .let { if (it > 180f) it - 360f else it }

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
        val effectiveRoll = simulatedRoll ?: lastRollDeg
        val cardinal = RangefinderEngine.getCardinalDirection(effectiveAzimuth)

        // Distance is computed from pitch only — zoom does NOT affect physical distance
        val rangeResult = RangefinderEngine.calculateRange(effectivePitch)

        onTelemetryUpdated(
            SensorTelemetry(
                headingDegrees = effectiveAzimuth,
                cardinalDirection = cardinal,
                pitch = effectivePitch,
                roll = effectiveRoll,
                estimatedDistanceMeters = rangeResult.distanceMeters,
                rangeConfidence = rangeResult.confidence,
                isRangeStable = !pitchDeadZone.isInDeadZone,
                isAvailable = true
            )
        )
    }
}
