package com.example.gooter_proyecto

import android.annotation.SuppressLint
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
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.StrictMode
import android.util.Log
import android.view.View
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
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.google.firebase.database.GenericTypeIndicator
import java.time.LocalDate

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
    private var carreraId: String = ""
    private var carreraEnCurso = false
    private var distanciaRecorrida = 0.0
    private var ultimaUbicacion: GeoPoint? = null
    private var carreraDestino: GeoPoint? = null
    private var permisoSolicitado = false
    private var gpsDialogShown = false

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
            permisoSolicitado = true
            if (it) {
                locationSettings()
            }
        }
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notificaciones permitidas en MapMenu", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this,
                "Las notificaciones están deshabilitadas en MapMenu",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        gpsDialogShown = false
        auth = FirebaseAuth.getInstance()
        // Solicita permiso para notificaciones
        NotificacionesDisponibles.getInstance().inicializar(this)
        carreraId = intent.getStringExtra("carrera_id") ?: ""

        if (carreraId.isNotEmpty())
        {
            binding.normalLayout.visibility = View.GONE
            binding.goOnlyButton.visibility = View.VISIBLE

            cargarDestinoCarrera()
            observarParticipantes()
            observarEstadoCarrera()

            binding.goOnlyButton.setOnClickListener {
                if (!carreraEnCurso) {
                    FirebaseDatabase.getInstance().getReference("carreras").child(carreraId).child("estado")
                        .setValue("en_curso")
                    borrarNotificacionesAsociadas()
                    Toast.makeText(this, "¡Carrera iniciada!", Toast.LENGTH_SHORT).show()
                    carreraEnCurso = true
                    binding.goOnlyButton.text = "FINISH RACE"
                } else {
                    finalizarCarreraYRegistrarEstadisticas()
                }
            }

        } else {
            binding.normalLayout.visibility = View.VISIBLE
            binding.goOnlyButton.visibility = View.GONE

            cargarEstacionamientos()

            binding.botonGo.setOnClickListener {
                val searchSuccessful = searchLocation()
                if (searchSuccessful && destinationLocation != null && currentLocation != null) {
                    drawDirectLine(currentLocation!!, destinationLocation!!)
                } else if (!searchSuccessful) {
                    // searchLocation already shows a toast if search fails
                } else {
                    Toast.makeText(this, "No se pudo trazar la ruta: ubicación actual desconocida.", Toast.LENGTH_SHORT).show()
                }
            }

            binding.editUbicacion.setOnEditorActionListener { _, i, _ ->
                if (i == EditorInfo.IME_ACTION_SEARCH) {
                    searchLocation()
                    hideKeyboard()
                    true
                }
                false
            }
            binding.iconoEditar.setOnClickListener {
                binding.editUbicacion.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.editUbicacion, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        sensorEventListener = createSensorEventListener()

        Configuration.getInstance().load(
            this,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        )
        map = binding.osmMap
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        geocoder = Geocoder(this)
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        inicializarSuscrLocalizacion()

        binding.btnMyLocation.setOnClickListener {
            goToMyLocation()
        }
        binding.botonBack.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        auth = FirebaseAuth.getInstance()
        locationUpdateHandler = Handler(Looper.getMainLooper())
        locationUpdateRunnable = object : Runnable {
            override fun run() {
                guardarUsuarioUbicacionFirebase()
                locationUpdateHandler.postDelayed(this, 7000)
            }
        }

        askNotificationPermission()

    }

    private fun inicializarSuscrLocalizacion() {
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = createLocationRequest()
        locationCallback = createLocationCallback()
        suscribirLocalizacion()
    }

    private fun suscribirLocalizacion() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            // Permiso concedido, continuar con la lógica de localización
            locationSettings()
        } else {
            // Permiso no concedido
            if (!permisoSolicitado && shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Mostrar explicación si el usuario ya lo denegó antes
                Toast.makeText(
                    this,
                    "El permiso es necesario para acceder a las funciones de localización.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            // Solicitar el permiso (ya sea la primera vez o después de la explicación)
            if (!permisoSolicitado) {
                locationPermission.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun cargarDestinoCarrera() {
        val carreraRef = FirebaseDatabase.getInstance().getReference("carreras").child(carreraId)
        carreraRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val destino = snapshot.child("location")
                val lat = destino.child("latitud").getValue(Double::class.java) ?: return
                val lon = destino.child("longitud").getValue(Double::class.java) ?: return
                val alt = destino.child("altitud").getValue(Double::class.java) ?: 0.0

                carreraDestino = GeoPoint(lat, lon, alt)
                destinationLocation = carreraDestino

                destinationMarker = createMarker(
                    carreraDestino!!,
                    "Destino de la carrera",
                    "Punto final de la competencia",
                    R.drawable.baseline_location_alt_24
                )
                destinationMarker?.let {
                    map.overlays.add(it)
                    map.invalidate()
                }

                currentLocation?.let {
                    drawDirectLine(it, carreraDestino!!)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MapsActivity, "Error al cargar destino de carrera", Toast.LENGTH_SHORT).show()
            }
        })
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

    private fun cargarEstacionamientos() {
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
                            val punto = if (altitud != null) {
                                GeoPoint(latitud, longitud, altitud)
                            } else {
                                GeoPoint(latitud, longitud)
                            }

                            val marker = createMarker(
                                punto,
                                nombre,
                                "Capacidad: ${capacidad ?: "N/A"}, Disponible: ${disponibilidad ?: "N/A"}",
                                R.drawable.star_ic
                            )

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

                map.invalidate()
                Toast.makeText(baseContext, "Se cargaron ${estacionamientoMarkers.size} estacionamientos", Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FIREBASE", "Error al cargar estacionamientos: ${error.message}")
                Toast.makeText(baseContext, "Error al cargar estacionamientos", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupRouteButton() {
    }

    private fun borrarNotificacionesAsociadas() {
        val db = FirebaseDatabase.getInstance().getReference("notificaciones")
        db.get().addOnSuccessListener { snapshot ->
            for (noti in snapshot.children) {
                val meta = noti.child("metadatos")
                if (meta.child("idCarrera").getValue(String::class.java) == carreraId) {
                    noti.ref.removeValue()
                }
            }
        }
    }

    @SuppressLint("NewApi")
    private fun finalizarCarreraYRegistrarEstadisticas() {
        val uid = auth.currentUser?.uid ?: return
        val hoy = LocalDate.now().toString()
        val statsRef = FirebaseDatabase.getInstance().getReference("estadisticasUsuarios").child(uid).child("diarias").child(hoy)

        statsRef.child("distanciaRecorrida").setValue(distanciaRecorrida)
        statsRef.child("tiempoActividad").setValue(0)

        FirebaseDatabase.getInstance().getReference("carreras").child(carreraId).removeValue()

        Toast.makeText(this, "Carrera finalizada. Distancia: ${distanciaRecorrida}km", Toast.LENGTH_LONG).show()
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun drawRouteAfterDelay() {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                Thread.sleep(500)
            }
            if (currentLocation != null && destinationLocation != null) {
                map.controller.animateTo(destinationLocation)
                map.controller.setZoom(16.0)

                drawDirectLine(currentLocation!!, destinationLocation!!)
            }
        }
    }

    private fun goToMyLocation() {
        currentLocation?.let { current ->
            map.controller.animateTo(current)
            map.controller.setZoom(18.0)
        }
    }

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

            routeOverlay?.let {
                map.overlays.remove(it)
                routeOverlay = null
            }

            destinationLocation = loc

            destinationMarker?.let {
                map.overlays.remove(it)
            }

            if (!address.isNullOrEmpty()) {
                map.controller.animateTo(loc)
                map.controller.setZoom(17.0)

                destinationMarker = createMarker(
                    loc,
                    "Destino",
                    address,
                    R.drawable.baseline_location_alt_24
                )
                destinationMarker?.let {
                    map.overlays.add(it)
                }

                showDistanceBetweenPoints(currentLocation, loc)

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
        } else if (!permisoSolicitado) {
            locationPermission.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val uims = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        if (uims.nightMode == UiModeManager.MODE_NIGHT_YES) {
            map.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
        }

        currentLocation?.let {
            map.controller.animateTo(it)
        } ?: run {
            map.controller.animateTo(bogota)
        }

        if (carreraDestino != null && currentLocation != null) {
            adjustZoomToShowRoute(currentLocation!!, carreraDestino!!)
        } else {
            currentLocation?.let {
                map.controller.animateTo(it)
            } ?: run {
                map.controller.animateTo(bogota)
            }
        }

        startSavingLocationUpdates()
    }

    private fun createSensorEventListener(): SensorEventListener {
        val sel = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (this@MapsActivity::map.isInitialized) {
                    if (event != null) {
                        if (event.values[0] > 5000) {
                            map.overlayManager.tilesOverlay.setColorFilter(null)
                        } else {
                            map.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            }
        }
        return sel
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("MapMenuActivity", "Notification permission already granted.")
                // Ya tienes el permiso
            } else if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
                // Opcional: Muestra una UI explicando por qué necesitas el permiso.
                // Por ahora, solo lo solicitamos.
                Log.d("MapMenuActivity", "Showing rationale or requesting permission.")
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // Solicita el permiso
                Log.d("MapMenuActivity", "Requesting notification permission.")
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
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

    private fun drawDirectLine(start: GeoPoint, end: GeoPoint) {
        routeOverlay?.let {
            map.overlays.remove(it)
        }

        val polyline = Polyline().apply {
            outlinePaint.color = Color.BLUE
            outlinePaint.strokeWidth = 10F
            setPoints(arrayListOf(start, end))
        }

        map.overlays.add(polyline)
        routeOverlay = polyline

        adjustZoomToShowRoute(start, end)

        map.invalidate()

        showDistanceBetweenPoints(start, end)
    }

    private fun adjustZoomToShowRoute(start: GeoPoint, end: GeoPoint) {
        val north = Math.max(start.latitude, end.latitude)
        val south = Math.min(start.latitude, end.latitude)
        val east = Math.max(start.longitude, end.longitude)
        val west = Math.min(start.longitude, end.longitude)

        val latSpan = (north - south) * 1.5
        val lonSpan = (east - west) * 1.5

        val newNorth = Math.min(north + latSpan * 0.25, 90.0)
        val newSouth = Math.max(south - latSpan * 0.25, -90.0)
        val newEast = Math.min(east + lonSpan * 0.25, 180.0)
        val newWest = Math.max(west - lonSpan * 0.25, -180.0)

        val centerLat = (newNorth + newSouth) / 2
        val centerLon = (newEast + newWest) / 2

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

    fun updateUI(location: Location) {
        val newLocation = GeoPoint(location.latitude, location.longitude)
        val address = findAddress(LatLng(location.latitude, location.longitude))

        ultimaUbicacion?.let {
            val d = distance(it.latitude, it.longitude, newLocation.latitude, newLocation.longitude)
            distanciaRecorrida += d
        }
        ultimaUbicacion = newLocation

        currentLocation = newLocation
        updateCurrentLocationMarker(newLocation, address)
        map.controller.animateTo(newLocation)

        carreraDestino?.let {
            drawDirectLine(newLocation, it)
        }
        map.invalidate()
    }

    private fun updateCurrentLocationMarker(location: GeoPoint, address: String?) {
        currentLocationMarker?.let {
            map.overlays.remove(it)
        }

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

    private fun observarParticipantes() {
        val userId = auth.currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("usuarios")

        FirebaseDatabase.getInstance().getReference("carreras").child(carreraId).child("jugadores")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val participantes = snapshot.children.mapNotNull {
                        val value = it.value
                        if (value is String) value else null
                    }

                    for (uid in participantes) {
                        if (uid != userId) {
                            ref.child(uid).child("ubicacion")
                                .addValueEventListener(object : ValueEventListener {
                                    override fun onDataChange(snap: DataSnapshot) {
                                        val lat = snap.child("latitud").getValue(Double::class.java) ?: return
                                        val lon = snap.child("longitud").getValue(Double::class.java) ?: return
                                        val alt = snap.child("altitud").getValue(Double::class.java) ?: 0.0

                                        val punto = GeoPoint(lat, lon, alt)
                                        val marker = createMarker(punto, "Participante", "", R.drawable.star_ic)
                                        map.overlays.add(marker)
                                        map.invalidate()
                                    }

                                    override fun onCancelled(error: DatabaseError) {}
                                })
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun observarEstadoCarrera() {
        val ref = FirebaseDatabase.getInstance().getReference("carreras").child(carreraId).child("estado")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                carreraEnCurso = snapshot.getValue(String::class.java) == "en_curso"
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}