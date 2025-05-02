package com.example.gooter_proyecto

import android.app.UiModeManager
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.os.StrictMode
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.gooter_proyecto.databinding.ActivityMapsBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapsActivity : AppCompatActivity() {

    val RADIUS_OF_EARTH_KM = 6378
    private lateinit var binding: ActivityMapsBinding
    private lateinit var map: MapView
    private val bogota = GeoPoint(4.62, -74.07)
    private lateinit var geocoder: Geocoder
    private var routeOverlay: Polyline? = null
    lateinit var locationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var currentLocation: GeoPoint? = null
    private var currentLocationMarker: Marker? = null
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private lateinit var sensorEventListener: SensorEventListener
    private var destinationLocation: GeoPoint? = null
    private var destinationMarker: Marker? = null

    val locationSettings = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
        ActivityResultCallback {
            if (it.resultCode == RESULT_OK) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, "El GPS está apagado", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ActivityResultCallback {
            if (it) {
                locationSettings()
            } else {
                Toast.makeText(this, "No hay permiso para acceder al GPS", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar sensor de luz
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        sensorEventListener = createSensorEventListener()

        // Configurar mapa
        Configuration.getInstance().load(
            this,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        )
        map = binding.osmMap
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        // Configurar utilidades
        geocoder = Geocoder(this)
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        // Configurar ubicación
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = createLocationRequest()
        locationCallback = createLocationCallback()

        // Configurar el botón para volver a mi ubicación
        binding.btnMyLocation.setOnClickListener {
            goToMyLocation()
        }

        // Configurar búsqueda por texto
        binding.editUbicacion.setOnEditorActionListener { _, i, _ ->
            if (i == EditorInfo.IME_ACTION_SEARCH) {
                searchLocation()
                // Ocultar teclado después de buscar
                hideKeyboard()
            }
            true
        }

        // Botón para dibujar ruta
        binding.botonGo.setOnClickListener {
            if (currentLocation != null && destinationLocation != null) {
                // Mostrar mensaje de carga
                Toast.makeText(
                    this,
                    "Calculando ruta...",
                    Toast.LENGTH_SHORT
                ).show()

                // Usar coroutine para no bloquear UI durante el cálculo
                CoroutineScope(Dispatchers.Main).launch {
                    drawDirectLine(currentLocation!!, destinationLocation!!)
                }
            } else {
                // Si no hay destino seleccionado pero hay texto en la búsqueda, intentar buscar primero
                val searchText = binding.editUbicacion.text.toString().trim()
                if (searchText.isNotEmpty() && destinationLocation == null) {
                    Toast.makeText(
                        this,
                        "Buscando ubicación primero...",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Buscar la ubicación y luego trazar la ruta
                    if (searchLocation()) {
                        // La función searchLocation ya establece destinationLocation si encuentra algo
                        // Esperamos un momento y trazamos la ruta
                        CoroutineScope(Dispatchers.Main).launch {
                            withContext(Dispatchers.IO) {
                                Thread.sleep(1000) // Pequeña espera para que se complete la búsqueda
                            }
                            if (currentLocation != null && destinationLocation != null) {
                                drawDirectLine(currentLocation!!, destinationLocation!!)
                            }
                        }
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Ingresa una dirección para crear la ruta",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Método para ir a mi ubicación actual
    private fun goToMyLocation() {
        currentLocation?.let { current ->
            map.controller.animateTo(current)
            map.controller.setZoom(18.0)
        }
    }

    // Función para buscar ubicación y poner marcador
    private fun searchLocation(): Boolean {
        val text = binding.editUbicacion.text.toString().trim()

        if (text.isEmpty()) {
            Toast.makeText(this, "Ingresa una dirección para buscar", Toast.LENGTH_SHORT).show()
            return false
        }

        val latlong = findLocation(text)

        return if (this@MapsActivity::map.isInitialized && latlong != null) {
            val loc = GeoPoint(latlong.latitude, latlong.longitude)
            val address = findAddress(latlong)

            // Guardar la ubicación del destino
            destinationLocation = loc

            // Remover el marcador anterior si existe
            destinationMarker?.let {
                map.overlays.remove(it)
            }

            if (!address.isNullOrEmpty()) {
                map.controller.animateTo(loc)
                map.controller.setZoom(18.0)

                // Agregar nuevo marcador
                destinationMarker = createMarker(
                    loc,
                    "Destino",
                    address,
                    R.drawable.baseline_location_alt_24
                )
                destinationMarker?.let {
                    map.overlays.add(it)
                }

                // Calcular distancia si tenemos ubicación actual
                currentLocation?.let {
                    val distancia = distance(
                        it.latitude,
                        it.longitude,
                        loc.latitude,
                        loc.longitude
                    )
                    Toast.makeText(
                        baseContext,
                        "Distancia: $distancia km",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // Refrescar mapa
                map.invalidate()
                true
            } else {
                Toast.makeText(
                    baseContext,
                    "No se encontró información para esta ubicación",
                    Toast.LENGTH_SHORT
                ).show()
                false
            }
        } else {
            Toast.makeText(
                baseContext,
                "No se encontró la ubicación",
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }

    // Función para ocultar el teclado
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.editUbicacion.windowToken, 0)
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            sensorEventListener,
            lightSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        map.onResume()
        map.controller.setZoom(18.0)
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationSettings()
        } else {
            locationPermission.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Modo nocturno basado en configuración del sistema
        val uims = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        if (uims.nightMode == UiModeManager.MODE_NIGHT_YES) {
            map.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
        }

        // Centrar mapa en ubicación actual o en Bogotá
        currentLocation?.let {
            map.controller.animateTo(it)
        } ?: run {
            map.controller.animateTo(bogota)
        }
    }

    private fun createSensorEventListener(): SensorEventListener {
        val sel = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (this@MapsActivity::map.isInitialized) {
                    if (event != null) {
                        // El valor de 5000 es mejor calibrarlo con un dispositivo real
                        if (event.values[0] > 5000) {
                            // Modo claro - quitar filtro de color
                            map.overlayManager.tilesOverlay.setColorFilter(null)
                        } else {
                            // Modo oscuro - aplicar filtro de inversión de colores
                            map.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // No se requiere implementación
            }
        }
        return sel
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        stopLocationUpdates()
        sensorManager.unregisterListener(sensorEventListener)
    }

    fun createMarker(p: GeoPoint, title: String, desc: String, iconID: Int): Marker? {
        var marker: Marker? = null;
        if (map != null) {
            marker = Marker(map);
            marker.title = title
            marker.subDescription = desc
            if (iconID != 0) {
                val myIcon = resources.getDrawable(iconID, this.theme)
                marker.icon = myIcon
            }
            marker.position = p
            marker.setAnchor(
                Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM
            );
        }
        return marker
    }

    // Función para dibujar una línea recta entre dos puntos como alternativa a una ruta
    fun drawDirectLine(start: GeoPoint, end: GeoPoint) {
        // Eliminar ruta anterior si existe
        routeOverlay?.let {
            map.overlays.remove(it)
        }

        // Crear nueva polyline
        val polyline = Polyline()
        polyline.outlinePaint.color = Color.BLUE
        polyline.outlinePaint.strokeWidth = 10F

        // Agregar puntos a la polyline
        val points = ArrayList<GeoPoint>()
        points.add(start)
        points.add(end)
        polyline.setPoints(points)

        // Agregar polyline al mapa
        map.overlays.add(polyline)
        routeOverlay = polyline

        // Refrescar el mapa
        map.invalidate()

        // Calcular la distancia para mostrarla en el Toast
        val distancia = distance(
            start.latitude,
            start.longitude,
            end.latitude,
            end.longitude
        )

        Toast.makeText(
            this,
            "Ruta trazada - Distancia: $distancia km",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun locationSettings() {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener { locationSettingsResponse ->
            startLocationUpdates()
        }
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    val isr: IntentSenderRequest =
                        IntentSenderRequest.Builder(exception.resolution).build()
                    locationSettings.launch(isr)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Toast.makeText(this, "El dispositivo no tiene GPS", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    fun stopLocationUpdates() {
        locationClient.removeLocationUpdates(locationCallback)
    }

    private fun createLocationRequest(): LocationRequest {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(5000)
            .build()
        return request
    }

    private fun createLocationCallback(): LocationCallback {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                val loc = result.lastLocation
                if (loc != null) {
                    updateUI(loc)
                }
            }
        }
        return callback
    }

    fun updateUI(location: Location) {
        val newLocation = GeoPoint(location.latitude, location.longitude)
        val address = findAddress(LatLng(location.latitude, location.longitude))

        val shouldMoveCamera = currentLocation == null ||
                currentLocation!!.distanceToAsDouble(newLocation) > 30.0

        currentLocation = newLocation

        currentLocationMarker?.let {
            map.overlays.remove(it)
        }

        val newMarker = createMarker(
            newLocation,
            "Tu ubicación",
            address ?: "Estás aquí",
            R.drawable.baseline_location_alt_24
        )

        map.overlays.add(newMarker)
        currentLocationMarker = newMarker

        if (shouldMoveCamera) {
            map.controller.animateTo(newLocation)
        }

        // Actualizar distancia al destino si existe
        destinationLocation?.let {
            val distancia = distance(
                newLocation.latitude,
                newLocation.longitude,
                it.latitude,
                it.longitude
            )
            Log.i("DISTANCE", "Distancia al destino: $distancia km")

            // Si existe una ruta trazada, actualizarla con la nueva posición
            if (routeOverlay != null) {
                drawDirectLine(newLocation, it)
            }
        }

        // Refrescar mapa
        map.invalidate()
    }

    fun findAddress(location: LatLng): String? {
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 2)
        if (addresses != null && !addresses.isEmpty()) {
            val addr = addresses.get(0)
            val locname = addr.getAddressLine(0)
            return locname
        }
        return null
    }

    fun findLocation(address: String): LatLng? {
        val addresses = geocoder.getFromLocationName(address, 2)
        if (addresses != null && !addresses.isEmpty()) {
            val addr = addresses.get(0)
            val location = LatLng(
                addr.latitude, addr.longitude
            )
            return location
        }
        return null
    }

    fun distance(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
        val latDistance = Math.toRadians(lat1 - lat2)
        val lngDistance = Math.toRadians(long1 - long2)
        val a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val result = RADIUS_OF_EARTH_KM * c
        return Math.round(result * 100.0) / 100.0
    }
}