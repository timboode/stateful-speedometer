package com.github.timboode.statefulspeedometer.viewers

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.timboode.statefulspeedometer.MainActivity
import com.github.timboode.statefulspeedometer.MainActivity.Companion.KEY_CURRENT_GAS_DISTANCE
import com.github.timboode.statefulspeedometer.MainActivity.Companion.KEY_GAS_WARNING_DISTANCE
import com.github.timboode.statefulspeedometer.MainActivity.Companion.KEY_TOTAL_DISTANCE
import com.github.timboode.statefulspeedometer.MainActivity.Companion.KEY_CURRENT_OIL_DISTANCE
import com.github.timboode.statefulspeedometer.MainActivity.Companion.KEY_OIL_WARNING_DISTANCE
import com.github.timboode.statefulspeedometer.R
import com.github.timboode.statefulspeedometer.services.SpeedColorService

class DigitalSpeedometer1Activity : AppCompatActivity() {
    var _locationManager: LocationManager? = null
    var _locationListener: LocationListener? = null

    var _speedColorService: SpeedColorService = SpeedColorService()

    var totalDistanceInMetres: Double = 0.0
        private set

    var gasWarningDistanceInMetres: Double = 0.0
        private set

    var currentGasDistanceInMetres: Double = 0.0
        private set

    var oilWarningDistanceInMetres: Double = 0.0
        private set

    var currentOilDistanceInMetres: Double = 0.0
        private set

    var lastLocation: Location? = null
        private set

    var updateCount: Int = 0

    // Drag-to-unlock variables
    private var sliderButton: ImageView? = null
    private var sliderContainer: RelativeLayout? = null
    private var initialX: Float = 0f
    private var sliderStartX: Float = 0f
    private val unlockThreshold: Float = 0.8f // 80% of the width

    class MyLocationListener : LocationListener {
        val digitalSpeedometer1Activity:DigitalSpeedometer1Activity

        constructor(digitalSpeedometer1Activity: DigitalSpeedometer1Activity) {
            this.digitalSpeedometer1Activity = digitalSpeedometer1Activity
        }

        override fun onLocationChanged(location: Location) {
            digitalSpeedometer1Activity.updateLocation(location)
        }

        // called in some Android version and fails
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make activity show over lock screen and disable system UI
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContentView(R.layout.activity_viewers_digital_meter1)

        // Disable automatic brightness adjustment - use current user brightness setting
        val layoutParams = window.attributes
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = layoutParams

        this._locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        this._locationListener = MyLocationListener(this)

        // Setup drag-to-unlock slider
        setupUnlockSlider()

        totalDistanceInMetres = PreferenceManager.getDefaultSharedPreferences(this).getFloat(KEY_TOTAL_DISTANCE, 0f).toDouble()

        gasWarningDistanceInMetres = PreferenceManager.getDefaultSharedPreferences(this).getFloat(
            KEY_GAS_WARNING_DISTANCE, 50f).toDouble()
        currentGasDistanceInMetres = PreferenceManager.getDefaultSharedPreferences(this).getFloat(
            KEY_CURRENT_GAS_DISTANCE, 0f).toDouble()

        oilWarningDistanceInMetres = PreferenceManager.getDefaultSharedPreferences(this).getFloat(
            KEY_OIL_WARNING_DISTANCE, 5000f).toDouble()
        currentOilDistanceInMetres = PreferenceManager.getDefaultSharedPreferences(this).getFloat(
            KEY_CURRENT_OIL_DISTANCE, 0f).toDouble()

        if (currentGasDistanceInMetres >= gasWarningDistanceInMetres) {
            findViewById<View>(R.id.digital_meter1_gas_icon).visibility = View.VISIBLE
        }
        else
            findViewById<View>(R.id.digital_meter1_gas_icon).visibility = View.INVISIBLE

        if (currentOilDistanceInMetres >= oilWarningDistanceInMetres) {
            findViewById<View>(R.id.digital_meter1_oil_icon).visibility = View.VISIBLE
        }
        else
            findViewById<View>(R.id.digital_meter1_oil_icon).visibility = View.INVISIBLE
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupUnlockSlider() {
        sliderButton = findViewById(R.id.unlock_slider_button)
        sliderContainer = findViewById(R.id.unlock_slider_container)

        sliderButton?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = event.rawX
                    sliderStartX = view.x
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialX
                    val newX = sliderStartX + deltaX
                    val containerWidth = sliderContainer?.width?.toFloat() ?: 0f
                    val buttonWidth = view.width.toFloat()

                    // Constrain movement within container
                    if (newX >= 0 && newX <= containerWidth - buttonWidth) {
                        view.x = newX
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val containerWidth = sliderContainer?.width?.toFloat() ?: 0f
                    val buttonWidth = view.width.toFloat()
                    val currentX = view.x

                    // Check if slider reached unlock threshold
                    if (currentX >= (containerWidth - buttonWidth) * unlockThreshold) {
                        // Unlock - exit the activity
                        finish()
                    } else {
                        // Reset slider to start position
                        view.animate().x(0f).setDuration(200).start()
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onBackPressed() {
        // Disable back button - user must use drag-to-unlock slider
        // Do nothing
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Intercept all key events to prevent navigation
        // Note: Android system prevents intercepting KEYCODE_HOME for security reasons,
        // but we can block other navigation keys
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_APP_SWITCH,
            KeyEvent.KEYCODE_MENU -> {
                // Block these keys - user must use drag-to-unlock slider
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onUserLeaveHint() {
        // This is called when the user presses home button or switches apps
        // We don't call super to prevent the default behavior
        // Note: This won't completely prevent leaving the activity, but makes it harder
    }

    override fun onStop() {
        this._locationManager!!.removeUpdates(this._locationListener!!)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        findViewById<TextView>(R.id.digital_meter1_textview).setText("...")
        updateLocationProvider()
        totalDistanceInMetres = PreferenceManager.getDefaultSharedPreferences(this).getFloat(KEY_TOTAL_DISTANCE, 0f).toDouble()
        gasWarningDistanceInMetres = PreferenceManager.getDefaultSharedPreferences(this).getFloat(KEY_GAS_WARNING_DISTANCE, 50f).toDouble()
        currentGasDistanceInMetres = PreferenceManager.getDefaultSharedPreferences(this).getFloat(KEY_CURRENT_GAS_DISTANCE, 0f).toDouble()
        oilWarningDistanceInMetres = PreferenceManager.getDefaultSharedPreferences(this).getFloat(KEY_OIL_WARNING_DISTANCE, 5000f).toDouble()
        currentOilDistanceInMetres = PreferenceManager.getDefaultSharedPreferences(this).getFloat(KEY_CURRENT_OIL_DISTANCE, 0f).toDouble()
        updateView()

        if (currentGasDistanceInMetres >= gasWarningDistanceInMetres) {
            findViewById<View>(R.id.digital_meter1_gas_icon).visibility = View.VISIBLE
        }
        else
            findViewById<View>(R.id.digital_meter1_gas_icon).visibility = View.INVISIBLE

        if (currentOilDistanceInMetres >= oilWarningDistanceInMetres) {
            findViewById<View>(R.id.digital_meter1_oil_icon).visibility = View.VISIBLE
        }
        else
            findViewById<View>(R.id.digital_meter1_oil_icon).visibility = View.INVISIBLE
    }

    @SuppressLint("MissingPermission")
    private fun updateLocationProvider() {
        this._locationManager!!.removeUpdates(this._locationListener!!)

        val locationProviders:List<String> = _locationManager!!.getProviders(true)

        val currentPreferenceLocationProvider:String = PreferenceManager.getDefaultSharedPreferences(this).getString(MainActivity.PREFERENCE_KEY_LOCATION_PROVIDER, "")!!

        var providerAvailable:Boolean = false

        for (p:String in locationProviders) {
            if (p.equals(currentPreferenceLocationProvider)) {
                providerAvailable = true
            }
        }

        if (providerAvailable) {
            this._locationManager!!.requestLocationUpdates(currentPreferenceLocationProvider, 0, 0.0f, this._locationListener!!)
        }
    }

    // Confidence for each unit is on comment on MainActivity
    private fun updateView() {
        val speedUnit:Int = PreferenceManager.getDefaultSharedPreferences(this).getInt(MainActivity.PREFERENCE_KEY_SPEED_UNIT, MainActivity.PREFERENCE_VAL_SPEED_UNIT_DEFAULT )!!

        when(speedUnit) {
            MainActivity.PREFERENCE_VAL_SPEED_UNIT_KM_H -> {
                findViewById<TextView>(R.id.digital_meter1_unit_textview).setText(getText(R.string.unit_km_per_hour))
            }
            MainActivity.PREFERENCE_VAL_SPEED_UNIT_KNOT -> {
                findViewById<TextView>(R.id.digital_meter1_unit_textview).setText(getText(R.string.unit_knot))
            }
            MainActivity.PREFERENCE_VAL_SPEED_UNIT_M_S -> {
                findViewById<TextView>(R.id.digital_meter1_unit_textview).setText(getText(R.string.unit_meter_per_second))
            }
            MainActivity.PREFERENCE_VAL_SPEED_UNIT_MPH -> {
                findViewById<TextView>(R.id.digital_meter1_unit_textview).setText(getText(R.string.unit_mile_per_hour))
            }
        }
    }

    fun setLocation(location: Location) {
        val gasLightWasOff:Boolean = currentGasDistanceInMetres < gasWarningDistanceInMetres
        val oilLightWasOff:Boolean = currentOilDistanceInMetres < oilWarningDistanceInMetres

        if (lastLocation != null) {
            totalDistanceInMetres += location.distanceTo(lastLocation!!)
            currentGasDistanceInMetres += location.distanceTo(lastLocation!!)
            currentOilDistanceInMetres += location.distanceTo(lastLocation!!)

            if (currentGasDistanceInMetres >= gasWarningDistanceInMetres && gasLightWasOff) {
                findViewById<View>(R.id.digital_meter1_gas_icon).visibility = View.VISIBLE
            }

            if (currentOilDistanceInMetres >= oilWarningDistanceInMetres && oilLightWasOff) {
                findViewById<View>(R.id.digital_meter1_oil_icon).visibility = View.VISIBLE
            }
        }
        lastLocation = location

        if(updateCount++ % 25 == 0) {
            updateCount = 0
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putFloat(KEY_TOTAL_DISTANCE, totalDistanceInMetres.toFloat())
                .apply()
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putFloat(KEY_CURRENT_GAS_DISTANCE, currentGasDistanceInMetres.toFloat())
                .apply()
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putFloat(KEY_CURRENT_OIL_DISTANCE, currentOilDistanceInMetres.toFloat())
                .apply()
        }
    }

    private fun formatDistance(distance: Double): String {
        return when {
            distance >= 1000 -> String.format("%.1f", distance)
            distance >= 100 -> String.format("%.2f", distance)
            distance >= 10 -> String.format("%.3f", distance)
            else -> String.format("%.4f", distance)
        }
    }

    // Confidence for each unit is on comment on MainActivity
    private fun updateLocation(location: Location) {
        val speedUnit:Int = PreferenceManager.getDefaultSharedPreferences(this).getInt(MainActivity.PREFERENCE_KEY_SPEED_UNIT, MainActivity.PREFERENCE_VAL_SPEED_UNIT_DEFAULT )!!

        var color:Int = _speedColorService.getSpeedColor(location.speed * 3.6f)
        setLocation(location)

        var speedDigitsView = findViewById<TextView>(R.id.digital_meter1_textview)
        var distanceDigitsView = findViewById<TextView>(R.id.digital_meter1_distance_textview)
        var distanceUnitsView = findViewById<TextView>(R.id.digital_meter1_distance_unit_textview)

        when(speedUnit) {
            MainActivity.PREFERENCE_VAL_SPEED_UNIT_KM_H -> {
                speedDigitsView.setText((location.speed * 3.6).toInt().toString())
                speedDigitsView.setTextColor(color)

                distanceDigitsView.setText(formatDistance(this.totalDistanceInMetres / 1000.0))
                distanceUnitsView.setText(R.string.unit_kilometres)
            }
            MainActivity.PREFERENCE_VAL_SPEED_UNIT_KNOT -> {
                speedDigitsView.setText((location.speed * 3.6 / 1.852).toInt().toString())
                speedDigitsView.setTextColor(color)

                distanceDigitsView.setText(formatDistance(this.totalDistanceInMetres / 1000.0))
                distanceUnitsView.setText(R.string.unit_kilometres)
            }
            MainActivity.PREFERENCE_VAL_SPEED_UNIT_M_S -> {
                speedDigitsView.setText((location.speed).toInt().toString())
                speedDigitsView.setTextColor(color)

                distanceDigitsView.setText(formatDistance(this.totalDistanceInMetres / 1000.0))
                distanceUnitsView.setText(R.string.unit_kilometres)
            }
            MainActivity.PREFERENCE_VAL_SPEED_UNIT_MPH -> {
                speedDigitsView.setText((location.speed * 3.6 / 1.609344).toInt().toString())
                speedDigitsView.setTextColor(color)

                distanceDigitsView.setText(formatDistance(this.totalDistanceInMetres / 1609.344))
                distanceUnitsView.setText(R.string.unit_kilometres)
            }
        }
        updateView()
    }
}