package com.example.gooter_proyecto

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.gooter_proyecto.databinding.ActivityPrincipalMapaBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class Principal_Mapa : AppCompatActivity() {
    private lateinit var binding: ActivityPrincipalMapaBinding
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrincipalMapaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createFragment()
    }

    private fun createFragment() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync { googleMap ->
            map = googleMap
            enableMyLocation()
        }
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            requestLocationPermission()
            return
        }

        map.isMyLocationEnabled = true
        getCurrentLocation()
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val myLocation = LatLng(location.latitude, location.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15f))
                map.addMarker(MarkerOptions().position(myLocation).title("Ubicación actual"))
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestLocationPermission() {
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                enableMyLocation()
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }
}
