package com.example.gooter_proyecto

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
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
import com.google.firebase.auth.FirebaseAuth
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.os.Handler

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
    private lateinit var auth: FirebaseAuth
    private lateinit var locationUpdateHandler: Handler
    private lateinit var locationUpdateRunnable: Runnable

    // Lista para almacenar los marcadores de estacionamiento
    private val estacionamientoMarkers: MutableList<Marker> = mutableListOf()

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
        binding.botonBack.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
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

        // Configurar botón de ruta
        setupRouteButton()

        // Cargar los puntos de estacionamiento desde Firebase
        cargarEstacionamientos()

        auth = FirebaseAuth.getInstance()
        locationUpdateHandler = Handler(Looper.getMainLooper())
        locationUpdateRunnable = object : Runnable {
            override fun run() {
                guardarUsuarioUbicacionFirebase()
                locationUpdateHandler.postDelayed(this, 7000)
            }
        }
    }

    private fun guardarUsuarioUbicacionFirebase() {
        val user = auth.currentUser
        val uid = user?.uid

        if (uid != null && currentLocation != null) {
            val database = FirebaseDatabase.getInstance()
            val userLocationRef = database.getReference("usuarios").child(uid).child("ubicacion")

            val locationData = hashMapOf(
                "latitud" to currentLocation!!.latitude,
                "longitud" to currentLocation!!.longitude,
                "altitud" to currentLocation!!.altitude
            )

            userLocationRef.setValue(locationData)
                .addOnSuccessListener {
                    Log.d("Firebase", "Location saved successfully for user: $uid")
                }
                .addOnFailureListener { e ->
                    Log.e("Firebase", "Failed to save location for user: $uid", e)
                }
        } else {
            Log.d("Firebase", "User not logged in or current location not available.")
        }
    }

    private fun startSavingLocationUpdates() {
        locationUpdateHandler.post(locationUpdateRunnable)
    }

    private fun stopSavingLocationUpdates() {
        locationUpdateHandler.removeCallbacks(locationUpdateRunnable)
    }



    // Nueva función para cargar datos de estacionamiento desde Firebase
    private fun cargarEstacionamientos() {
        // Referencia a la base de datos de Firebase, específicamente a la tabla "estacionamientos"
        val database = FirebaseDatabase.getInstance()
        val estacionamientoRef = database.getReference("estacionamientos")

        estacionamientoRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                estacionamientoMarkers.forEach { marker ->
                    map.overlays.remove(marker)
                }
                estacionamientoMarkers.clear()


                for (estacionamientoSnapshot in snapshot.children) {
                    try {
                        val parkingId = estacionamientoSnapshot.key ?: "Unknown Parking"
                        val ubicacionSnapshot = estacionamientoSnapshot.child("ubicacion")
                        val latitud = ubicacionSnapshot.child("latitud").getValue(Double::class.java)
                        val longitud = ubicacionSnapshot.child("longitud").getValue(Double::class.java)
                        val altitud = ubicacionSnapshot.child("altitud").getValue(Double::class.java)
                        val capacidad = estacionamientoSnapshot.child("capacidad").getValue(Int::class.java)
                        val disponibilidad = estacionamientoSnapshot.child("disponibilidad").getValue(Boolean::class.java)
                        val nombre = parkingId
                        if (latitud != null && longitud != null) {
                            // Crear punto para el marcador
                            val punto = if (altitud != null) {
                                GeoPoint(latitud, longitud, altitud)
                            } else {
                                GeoPoint(latitud, longitud)
                            }

                            // Crear marcador de estacionamiento
                            val marker = createMarker(
                                punto,
                                nombre,
                                "Capacidad: ${capacidad ?: "N/A"}, Disponible: ${disponibilidad ?: "N/A"}", // Example snippet info
                                R.drawable.star_ic
                            )

                            // Añadir marcador al mapa y a la lista
                            marker?.let {
                                map.overlays.add(it)
                                estacionamientoMarkers.add(it)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("FIREBASE", "Error al cargar estacionamiento: ${e.message}")
                        Log.e("FIREBASE", "Error details for snapshot: ${estacionamientoSnapshot.key}")
                    }
                }

                // Refrescar el mapa para mostrar los marcadores
                map.invalidate()
                Toast.makeText(baseContext, "Se cargaron ${estacionamientoMarkers.size} estacionamientos", Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FIREBASE", "Error al cargar estacionamientos: ${error.message}")
                Toast.makeText(baseContext, "Error al cargar estacionamientos", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Configuración del botón de ruta
    private fun setupRouteButton() {
        binding.botonGo.setOnClickListener {
            val searchText = binding.editUbicacion.text.toString().trim()

            // Siempre intentamos buscar la ubicación cuando hay texto
            if (searchText.isNotEmpty()) {
                Toast.makeText(this, "Buscando ubicación...", Toast.LENGTH_SHORT).show()

                // La búsqueda actualizará destinationLocation y moverá la cámara
                if (searchLocation()) {
                    // Aseguramos que se trace la ruta después de la búsqueda
                    drawRouteAfterDelay()
                }
            }
            // Si no hay texto pero sí hay destino, solo trazamos la ruta
            else if (currentLocation != null && destinationLocation != null) {
                Toast.makeText(this, "Calculando ruta...", Toast.LENGTH_SHORT).show()

                // Primero movemos la cámara al destino
                map.controller.animateTo(destinationLocation)
                map.controller.setZoom(16.0)

                // Luego trazamos la ruta
                drawDirectLine(currentLocation!!, destinationLocation!!)
            }
            // Si no hay destino ni texto, mostramos mensaje
            else {
                Toast.makeText(this, "Ingresa una dirección para crear la ruta", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Función para dibujar ruta después de un pequeño retraso
    private fun drawRouteAfterDelay() {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                Thread.sleep(500) // Espera menor, solo para asegurar que se complete la búsqueda
            }
            if (currentLocation != null && destinationLocation != null) {
                // Aseguramos que la cámara se mueva al destino
                map.controller.animateTo(destinationLocation)
                map.controller.setZoom(16.0)

                // Dibujamos la ruta
                drawDirectLine(currentLocation!!, destinationLocation!!)
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

            Log.i("SEARCH", "ADDRESS: $address")

            // Eliminar ruta anterior si existe
            routeOverlay?.let {
                map.overlays.remove(it)
                routeOverlay = null
            }

            // Guardar la ubicación del destino
            destinationLocation = loc

            // Remover el marcador anterior si existe
            destinationMarker?.let {
                map.overlays.remove(it)
            }

            if (!address.isNullOrEmpty()) {
                // Nos aseguramos de mover la cámara al destino
                map.controller.animateTo(loc)
                map.controller.setZoom(17.0)

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

                // Mostrar distancia si tenemos ubicación actual
                showDistanceBetweenPoints(currentLocation, loc)

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

    // Nueva función para calcular y mostrar distancia entre dos puntos
    private fun showDistanceBetweenPoints(startPoint: GeoPoint?, endPoint: GeoPoint?) {
        if (startPoint != null && endPoint != null) {
            val distancia = distance(
                startPoint.latitude,
                startPoint.longitude,
                endPoint.latitude,
                endPoint.longitude
            )
            Toast.makeText(
                baseContext,
                "Distancia: $distancia km",
                Toast.LENGTH_SHORT
            ).show()
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

        startSavingLocationUpdates()
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
        stopSavingLocationUpdates()
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

    // Función optimizada para dibujar la línea directa
    private fun drawDirectLine(start: GeoPoint, end: GeoPoint) {
        // Eliminar ruta anterior si existe
        routeOverlay?.let {
            map.overlays.remove(it)
        }

        // Crear nueva polyline
        val polyline = Polyline().apply {
            outlinePaint.color = Color.BLUE
            outlinePaint.strokeWidth = 10F
            setPoints(arrayListOf(start, end))
        }

        // Agregar polyline al mapa
        map.overlays.add(polyline)
        routeOverlay = polyline

        // Ajustar zoom para mostrar toda la ruta
        adjustZoomToShowRoute(start, end)

        // Refrescar el mapa
        map.invalidate()

        // Mostrar distancia
        showDistanceBetweenPoints(start, end)
    }

    // Nueva función para ajustar el zoom y mostrar la ruta completa
    private fun adjustZoomToShowRoute(start: GeoPoint, end: GeoPoint) {
        // Calculamos los límites máximos y mínimos
        val north = Math.max(start.latitude, end.latitude)
        val south = Math.min(start.latitude, end.latitude)
        val east = Math.max(start.longitude, end.longitude)
        val west = Math.min(start.longitude, end.longitude)

        // Añadimos un margen
        val latSpan = (north - south) * 1.5
        val lonSpan = (east - west) * 1.5

        // Recalculamos con el margen
        val newNorth = Math.min(north + latSpan * 0.25, 90.0)
        val newSouth = Math.max(south - latSpan * 0.25, -90.0)
        val newEast = Math.min(east + lonSpan * 0.25, 180.0)
        val newWest = Math.max(west - lonSpan * 0.25, -180.0)

        // Calculamos el centro
        val centerLat = (newNorth + newSouth) / 2
        val centerLon = (newEast + newWest) / 2

        // Aplicamos al mapa
        map.zoomToBoundingBox(org.osmdroid.util.BoundingBox(
            newNorth, newEast, newSouth, newWest
        ), true, 100)
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

    // Función updateUI optimizada
    fun updateUI(location: Location) {
        val newLocation = GeoPoint(location.latitude, location.longitude)
        val address = findAddress(LatLng(location.latitude, location.longitude))

        val shouldMoveCamera = currentLocation == null ||
                currentLocation!!.distanceToAsDouble(newLocation) > 30.0

        currentLocation = newLocation

        // Actualizar marcador de ubicación actual
        updateCurrentLocationMarker(newLocation, address)

        if (shouldMoveCamera) {
            map.controller.animateTo(newLocation)
        }

        // Actualizar ruta si existe destino
        destinationLocation?.let {
            if (routeOverlay != null) {
                drawDirectLine(newLocation, it)
            }
        }

        // Refrescar mapa
        map.invalidate()
    }

    // Nueva función para actualizar el marcador de ubicación actual
    private fun updateCurrentLocationMarker(location: GeoPoint, address: String?) {
        // Eliminar marcador anterior
        currentLocationMarker?.let {
            map.overlays.remove(it)
        }

        // Crear y añadir nuevo marcador
        val newMarker = createMarker(
            location,
            "Tu ubicación",
            address ?: "Estás aquí",
            R.drawable.baseline_location_alt_24
        )

        map.overlays.add(newMarker)
        currentLocationMarker = newMarker
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