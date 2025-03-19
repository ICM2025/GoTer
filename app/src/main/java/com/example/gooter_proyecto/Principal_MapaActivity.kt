package com.example.gooter_proyecto

import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.gooter_proyecto.databinding.ActivityPrincipalMapaBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task

class Principal_MapaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPrincipalMapaBinding
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    // Registro para solicitud de permisos
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Permiso de ubicación concedido", Toast.LENGTH_SHORT).show()
                checkLocationSettings()
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Registro para configuración de ubicación
    private val locationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
        { result ->
            if (result.resultCode == RESULT_OK) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, "Ubicación requerida activada", Toast.LENGTH_SHORT).show()
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrincipalMapaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationComponents()
        createFragment()
        binding.botonBack.setOnClickListener{
            startActivity(Intent(baseContext,HomeActivity::class.java))
        }    }

    private fun setupLocationComponents() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(5000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                result.lastLocation?.let {
                    updateMapLocation(it)
                }
            }
        }
    }

    private fun createFragment() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync { googleMap ->
            map = googleMap
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            checkLocationSettings()
        }
    }

    private fun checkLocationSettings() {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@addOnSuccessListener
            }
            map.isMyLocationEnabled = true
            startLocationUpdates()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                    locationSettingsLauncher.launch(intentSenderRequest)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Toast.makeText(this, "Error en configuración de ubicación", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun updateMapLocation(location: Location) {
        val currentLocation = LatLng(location.latitude, location.longitude)
        map.clear()
        map.addMarker(
            MarkerOptions()
                .position(currentLocation)
                .title("Tu ubicación actual")
        )
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
        Toast.makeText(this, "Ubicación actualizada", Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}