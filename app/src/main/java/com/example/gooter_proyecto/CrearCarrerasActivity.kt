package com.example.gooter_proyecto

import adapters.ComunidadHomeAdapter
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gooter_proyecto.databinding.ActivityCrearCarrerasBinding
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import models.Comunidad
import kotlin.math.*
import android.view.KeyEvent

class CrearCarrerasActivity : AppCompatActivity() {

    private val ETIQUETA = "CrearCarrerasActivity"

    private lateinit var binding: ActivityCrearCarrerasBinding
    private lateinit var adaptadorComunidades: ComunidadHomeAdapter
    private lateinit var autenticacion: FirebaseAuth

    private lateinit var mapa: MapView
    private lateinit var clienteUbicacion: FusedLocationProviderClient
    private lateinit var solicitudUbicacion: LocationRequest
    private lateinit var callbackUbicacion: LocationCallback
    private lateinit var geocodificador: Geocoder
    private var ubicacionActual: GeoPoint? = null
    private var ubicacionDestino: GeoPoint? = null
    private var marcadorActual: Marker? = null
    private var marcadorDestino: Marker? = null
    private var lineaRuta: Polyline? = null
    private var DESTINO_CARRERA: GeoPoint? = null

    private lateinit var detectorGestos: GestureDetector

    private val lanzadorPermisoUbicacion =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
            if (concedido) {
                Log.d(ETIQUETA, "Permiso de ubicación concedido.")
                iniciarActualizacionesUbicacion()
            } else {
                Log.w(ETIQUETA, "Permiso de ubicación denegado.")
                Toast.makeText(this, "Permiso de ubicación denegado.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearCarrerasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(ETIQUETA, "onCreate iniciado.")

        autenticacion = FirebaseAuth.getInstance()
        cargarComunidades()

        Configuration.getInstance().load(this, androidx.preference.PreferenceManager.getDefaultSharedPreferences(this))
        mapa = binding.osmMap
        mapa.setTileSource(TileSourceFactory.MAPNIK)
        mapa.setMultiTouchControls(true)
        Log.d(ETIQUETA, "Mapa OSMDroid configurado.")

        geocodificador = Geocoder(this)
        clienteUbicacion = LocationServices.getFusedLocationProviderClient(this)
        solicitudUbicacion = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(3000)
            .build()
        callbackUbicacion = object : LocationCallback() {
            override fun onLocationResult(resultado: LocationResult) {
                Log.d(ETIQUETA, "Resultado de ubicación recibido.")
                resultado.lastLocation?.let { nuevaUbicacion(it) }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            lanzadorPermisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            iniciarActualizacionesUbicacion()
        }

        binding.editTextDestination.setOnEditorActionListener { v, actionId, event ->
            val manejado = if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.action == MotionEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)
            ) {
                buscarDestino(v.text.toString().trim())
                ocultarTeclado()
                true
            } else {
                false
            }
            manejado
        }
        binding.buttonBack.setOnClickListener{
            startActivity(Intent(this, CarreraActivity::class.java))
    }

        detectorGestos = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) { }
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val proyeccion = mapa.projection
                val punto = proyeccion.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                establecerDestino(punto)
                Toast.makeText(this@CrearCarrerasActivity, "Destino establecido por doble toque", Toast.LENGTH_SHORT).show()
                return true
            }
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean = false
        })

        mapa.setOnTouchListener { _, evento -> detectorGestos.onTouchEvent(evento) }

        Log.d(ETIQUETA, "onCreate finalizado.")
    }

    override fun onResume() {
        super.onResume()
        mapa.onResume()
        ubicacionActual?.let {
            mapa.controller.apply {
                animateTo(it)
                setZoom(18.0)
            }
        } ?: run {
            mapa.controller.apply {
                setCenter(ubicacionDestino ?: GeoPoint(4.60971, -74.08175))
                setZoom(mapa.zoomLevel.coerceAtLeast(12))
            }
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            iniciarActualizacionesUbicacion()
        }
    }

    override fun onPause() {
        super.onPause()
        mapa.onPause()
        clienteUbicacion.removeLocationUpdates(callbackUbicacion)
    }

    private fun cargarComunidades() {
        val usuarioId = autenticacion.currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().reference.child("comunidad")
        ref.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val lista = mutableListOf<Comunidad>()
                for (snap in snapshot.children) {
                    val admin = snap.child("administrador").getValue(String::class.java)
                    val participantesSnap = snap.child("participantes")
                    val participantes = if (participantesSnap.exists() && participantesSnap.value != null) {
                        participantesSnap.children.mapNotNull { it.getValue(String::class.java) }
                    } else emptyList()
                    if (admin == usuarioId || participantes.contains(usuarioId)) {
                        val nombre = snap.child("nombreGrupo").getValue(String::class.java) ?: "Sin nombre"
                        val miembros = snap.child("miembros").getValue(Int::class.java) ?: participantes.size
                        lista.add(Comunidad(nombre, R.drawable.ic_user, miembros))
                    }
                }
                adaptadorComunidades = ComunidadHomeAdapter(lista) { c ->
                    startActivity(Intent(this@CrearCarrerasActivity, ChatActivity::class.java).apply {
                        putExtra("nombreGrupo", c.nombre)
                    })
                }
                binding.rvListaComunidades.apply {
                    layoutManager = LinearLayoutManager(this@CrearCarrerasActivity, LinearLayoutManager.HORIZONTAL, false)
                    adapter = adaptadorComunidades
                }
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Toast.makeText(this@CrearCarrerasActivity, "Error al cargar comunidades: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun iniciarActualizacionesUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            clienteUbicacion.requestLocationUpdates(solicitudUbicacion, callbackUbicacion, Looper.getMainLooper())
        } else {
            lanzadorPermisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun nuevaUbicacion(loc: Location) {
        val punto = GeoPoint(loc.latitude, loc.longitude, loc.altitude)
        ubicacionActual = punto
        actualizarMarcadorActual(punto)
        ubicacionDestino?.let { dibujarRuta(punto, it) }
    }

    private fun actualizarMarcadorActual(pt: GeoPoint) {
        marcadorActual?.let { if (mapa.overlays.contains(it)) mapa.overlays.remove(it) }
        val m = Marker(mapa).apply {
            position = pt
            title = "Tu ubicación"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapa.overlays.add(m)
        marcadorActual = m
        mapa.invalidate()
    }

    private fun buscarDestino(direccion: String) {
        if (direccion.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa un destino.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!Geocoder.isPresent()) {
            Toast.makeText(this, "Servicio de geocodificación no disponible.", Toast.LENGTH_SHORT).show()
            return
        }
        Thread {
            val lista = try { geocodificador.getFromLocationName(direccion, 1) } catch (e: Exception) { null }
            runOnUiThread {
                if (lista.isNullOrEmpty()) {
                    Toast.makeText(this, "Destino no encontrado: \"$direccion\".", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                val addr = lista[0]
                val pt = GeoPoint(addr.latitude, addr.longitude)
                establecerDestino(pt)
            }
        }.start()
    }

    private fun establecerDestino(pt: GeoPoint) {
        ubicacionDestino = pt
        DESTINO_CARRERA = pt
        marcadorDestino?.let { if (mapa.overlays.contains(it)) mapa.overlays.remove(it) }
        marcadorDestino = Marker(mapa).apply {
            position = pt
            title = "Destino Carrera"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapa.overlays.add(marcadorDestino)
        mapa.controller.apply {
            animateTo(pt)
            setZoom(mapa.zoomLevel.coerceAtLeast(10).coerceAtMost(18))
        }
        ubicacionActual?.let { dibujarRuta(it, pt) } ?: Toast.makeText(this, "Ubicación actual no disponible para dibujar ruta.", Toast.LENGTH_SHORT).show()
    }

    private fun dibujarRuta(inicio: GeoPoint, fin: GeoPoint) {
        lineaRuta?.let { if (mapa.overlays.contains(it)) mapa.overlays.remove(it) }
        val linea = Polyline().apply {
            setPoints(listOf(inicio, fin))
            outlinePaint.color = try { resources.getColor(R.color.blue_buttons, theme) } catch (e: Exception) { android.graphics.Color.BLUE }
            outlinePaint.strokeWidth = 8f
        }
        mapa.overlays.add(linea)
        lineaRuta = linea
        mapa.invalidate()
        val km = distancia(inicio.latitude, inicio.longitude, fin.latitude, fin.longitude)
        Toast.makeText(
            this,
            "Distancia aproximada: ${String.format("%.2f", km)} km",
            Toast.LENGTH_LONG
        ).show()

    }

                private fun distancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6371.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat/2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon/2).pow(2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }

                private fun ocultarTeclado() {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.editTextDestination.windowToken, 0)
        }

                private fun mostrarTeclado() {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.editTextDestination, InputMethodManager.SHOW_IMPLICIT)
        }
    }
