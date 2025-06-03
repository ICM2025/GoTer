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
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import models.Comunidad
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.*

class CrearCarrerasActivity : AppCompatActivity() {

    private val ETIQUETA = "CrearCarrerasActivity"
    private lateinit var binding: ActivityCrearCarrerasBinding
    private lateinit var adaptadorComunidades: ComunidadHomeAdapter
    private lateinit var autenticacion: FirebaseAuth
    private lateinit var database: FirebaseDatabase

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

    private lateinit var detectorGestos: GestureDetector

    private var modoDirecto: Boolean = false
    private var carreraIdExistente: String? = null

    // Add this at the top of your class for detailed Firebase debugging
    private fun debugFirebaseData() {
        val usuarioId = autenticacion.currentUser?.uid ?: return
        Log.d(ETIQUETA, "Debug Firebase - UID: $usuarioId")

        // Check database connection
        database.reference.child(".info/connected").addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                Log.d(ETIQUETA, "Firebase conexión: ${if (connected) "ACTIVA" else "INACTIVA"}")
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Log.e(ETIQUETA, "Error verificando conexión: ${error.message}")
            }
        })

        // Verifica estructura de datos de comunidades
        database.reference.child("comunidad").get()
            .addOnSuccessListener { snapshot ->
                Log.d(ETIQUETA, "Estructura de datos - Comunidades: ${snapshot.childrenCount}")
                if (snapshot.childrenCount > 0) {
                    // Imprime los detalles de cada comunidad
                    for (comSnap in snapshot.children) {
                        val comId = comSnap.key
                        val comNombre = comSnap.child("nombreGrupo").getValue(String::class.java)
                        val adminId = comSnap.child("administrador").getValue(String::class.java)
                        val isAdmin = adminId == usuarioId

                        // Estructurar participantes
                        val participantesSnap = comSnap.child("participantes")
                        val isParticipante = if (participantesSnap.exists()) {
                            var encontrado = false
                            for (partSnap in participantesSnap.children) {
                                if (partSnap.key == usuarioId) {
                                    encontrado = true
                                    break
                                }
                            }
                            encontrado
                        } else false

                        Log.d(ETIQUETA, "Comunidad: $comId ($comNombre)")
                        Log.d(ETIQUETA, "  - ¿Es admin? $isAdmin, ¿Es participante? $isParticipante")

                        // Si debería ver esta comunidad pero no aparece en la UI, hay un problema
                        if (isAdmin || isParticipante) {
                            Log.d(ETIQUETA, "  ¡USUARIO DEBERÍA VER ESTA COMUNIDAD!")
                        }
                    }
                } else {
                    Log.d(ETIQUETA, "No hay comunidades en la base de datos")
                }
            }
            .addOnFailureListener { e ->
                Log.e(ETIQUETA, "Error al obtener comunidades: ${e.message}")
            }
    }


    private val lanzadorPermisoUbicacion =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
            if (concedido) {
                iniciarActualizacionesUbicacion()
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearCarrerasBinding.inflate(layoutInflater)
        setContentView(binding.root)
        autenticacion = FirebaseAuth.getInstance()

        modoDirecto = intent.getBooleanExtra("modo_directo", false)
        if (modoDirecto) {
            carreraIdExistente = intent.getStringExtra("carrera_id")
            if (carreraIdExistente == null) {
                Toast.makeText(this, "Error: No se proporcionó ID de carrera en modo directo.", Toast.LENGTH_LONG).show()
            }
        }
        database = FirebaseDatabase.getInstance()

        val databases = FirebaseDatabase.getInstance().reference

        configurarVistaSegunModo()
        if (!modoDirecto) {
            // Asegurar que estas vistas están visibles antes de cargar datos
            binding.rvListaComunidades.visibility = View.VISIBLE
            binding.textViewSelectComunity.visibility = View.VISIBLE

            // Verificar estructura layout
            Log.d(ETIQUETA, "Verificando estructura de layout:")
            Log.d(ETIQUETA, "- RecyclerView null: ${binding.rvListaComunidades == null}")
            Log.d(ETIQUETA, "- TextView null: ${binding.textViewSelectComunity == null}")

            // Delay pequeño para asegurar que la UI esté inicializada
            binding.rvListaComunidades.post {
                cargarComunidades()
            }
        }

        Configuration.getInstance().load(this, androidx.preference.PreferenceManager.getDefaultSharedPreferences(this))
        mapa = binding.osmMap
        mapa.setTileSource(TileSourceFactory.MAPNIK)
        mapa.setMultiTouchControls(true)

        geocodificador = Geocoder(this)
        clienteUbicacion = LocationServices.getFusedLocationProviderClient(this)
        solicitudUbicacion = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(3000)
            .build()
        callbackUbicacion = object : LocationCallback() {
            override fun onLocationResult(resultado: LocationResult) {
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
            finish()
        }

        binding.buttonIniciar.setOnClickListener {
            if (ubicacionDestino == null) {
                Toast.makeText(this, "Primero selecciona un destino para la carrera", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (modoDirecto) {
                if (carreraIdExistente != null) {
                    val locationData = hashMapOf(
                        "latitud" to ubicacionDestino!!.latitude,
                        "longitud" to ubicacionDestino!!.longitude,
                        "altitud" to 0.0
                    )
                    databases.child("carreras").child(carreraIdExistente!!).child("location").setValue(locationData).addOnSuccessListener {
                        val intent = Intent(this, MapsActivity::class.java).apply {
                            putExtra("carrera_id", carreraIdExistente)
                            putExtra("admin", true)
                        }
                        Toast.makeText(this, "Iniciando seguimiento de carrera existente", Toast.LENGTH_SHORT).show()
                        startActivity(intent)
                    }.addOnFailureListener{ exception ->
                        Toast.makeText(this, "Error al guardar ubicación: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Error: ID de carrera no disponible para iniciar.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        detectorGestos = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val proyeccion = mapa.projection
                val punto = proyeccion.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                establecerDestino(punto)
                Toast.makeText(this@CrearCarrerasActivity, "Destino establecido por doble toque", Toast.LENGTH_SHORT).show()
                return true
            }
        })

        mapa.setOnTouchListener { _, evento -> detectorGestos.onTouchEvent(evento) }
    }

    private fun configurarVistaSegunModo() {
        Log.d(ETIQUETA, "Configurando vista según modo: $modoDirecto")

        if (modoDirecto) {
            binding.rvListaComunidades.visibility = View.GONE
            binding.textViewSelectComunity.visibility = View.GONE
            binding.buttonIniciar.visibility = View.VISIBLE
            Log.d(ETIQUETA, "Modo directo activado: ocultando lista de comunidades")
        } else {
            binding.rvListaComunidades.visibility = View.VISIBLE
            binding.textViewSelectComunity.visibility = View.VISIBLE
            binding.buttonIniciar.visibility = View.GONE
            Log.d(ETIQUETA, "Modo normal: mostrando lista de comunidades")

            // No llamamos a cargarComunidades aquí, lo haremos desde onCreate con un post()
        }
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
        val usuarioId = autenticacion.currentUser?.uid ?: run {
            Log.e(ETIQUETA, "No hay usuario autenticado")
            Toast.makeText(this, "No hay usuario autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(ETIQUETA, "Cargando comunidades para usuario: $usuarioId")

        binding.rvListaComunidades.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )

        adaptadorComunidades = ComunidadHomeAdapter(mutableListOf()) { comunidad ->
            if (ubicacionDestino == null) {
                Toast.makeText(
                    this@CrearCarrerasActivity,
                    "Primero selecciona un destino para la carrera",
                    Toast.LENGTH_SHORT
                ).show()
                return@ComunidadHomeAdapter
            }
            crearCarreraParaComunidad(comunidad)
        }
        binding.rvListaComunidades.adapter = adaptadorComunidades

        val ref = database.reference.child("comunidad")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(ETIQUETA, "Datos recibidos de Firebase: ${snapshot.childrenCount} comunidades en total")

                if (!snapshot.exists() || snapshot.childrenCount.toInt() == 0) {
                    binding.textViewSelectComunity.text = "No hay comunidades disponibles"
                    return
                }

                val listaComunidades = mutableListOf<Comunidad>()

                for (comunidadSnap in snapshot.children) {
                    try {
                        val comId = comunidadSnap.key ?: continue
                        val nombreGrupo = comunidadSnap.child("nombreGrupo").getValue(String::class.java) ?: "Sin nombre"
                        val adminId = comunidadSnap.child("administrador").getValue(String::class.java)
                        val chatId = comunidadSnap.child("chatId").getValue(String::class.java)?: "Sin chat"

                        // Leer URL de la imagen
                        val fotoUrl = comunidadSnap.child("fotoUrl").getValue(String::class.java) ?: ""

                        // Extraer participantes
                        val participantes = comunidadSnap.child("participantes").children.mapNotNull { it.getValue(String::class.java) }

                        // Verificar si el usuario es admin o participante
                        if (adminId == usuarioId || participantes.contains(usuarioId)) {
                            val miembros = comunidadSnap.child("miembros").getValue(Int::class.java) ?: participantes.size
                            listaComunidades.add(
                                Comunidad(
                                    id = comId,
                                    nombre = nombreGrupo,
                                    imagen = fotoUrl,          // ← Usar la URL
                                    miembros = miembros,
                                    participantes = participantes,
                                    idChat = chatId
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(ETIQUETA, "Error procesando comunidad: ${e.message}", e)
                    }
                }

                runOnUiThread {
                    if (listaComunidades.isEmpty()) {
                        binding.textViewSelectComunity.text = "No perteneces a ninguna comunidad"
                        return@runOnUiThread
                    }

                    (binding.rvListaComunidades.adapter as? ComunidadHomeAdapter)?.apply {
                        lista = listaComunidades
                        notifyDataSetChanged()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(ETIQUETA, "Error cargando comunidades: ${error.message}")
                Toast.makeText(
                    this@CrearCarrerasActivity,
                    "Error al cargar comunidades: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun crearNuevaCarreraDirecta() {
        val usuarioId = autenticacion.currentUser?.uid ?: return
        val ubicacionDestino = this.ubicacionDestino ?: return

        val distancia = ubicacionActual?.let {
            distancia(it.latitude, it.longitude, ubicacionDestino.latitude, ubicacionDestino.longitude)
        } ?: 0.0

        val carreraId = database.reference.child("carreras").push().key ?: return

        val carrera = mapOf(
            "id" to carreraId,
            "organizadorId" to usuarioId,
            "fechaInicio" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()),
            "estado" to "pendiente",
            "inicioCarrera" to mapOf(
                "latitud" to ubicacionActual?.latitude,
                "longitud" to ubicacionActual?.longitude,
                "altitud" to ubicacionActual?.altitude
            ),
            "location" to mapOf(
                "latitud" to ubicacionDestino.latitude,
                "longitud" to ubicacionDestino.longitude,
                "altitud" to ubicacionDestino.altitude
            ),
            "distanciaTotal" to distancia,
            "participantes" to mapOf(usuarioId to true),
            "modoDirecto" to true
        )

        database.reference.child("carreras").child(carreraId).setValue(carrera)
            .addOnSuccessListener {
                Toast.makeText(this, "Carrera directa creada correctamente", Toast.LENGTH_SHORT).show()
                actualizarUbicacionParticipante(carreraId)
                val intent = Intent(this, MapsActivity::class.java).apply {
                    putExtra("carrera_id", carreraId)
                    putExtra("admin", true)
                }
                startActivity(intent)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al crear carrera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun crearCarreraParaComunidad(comunidad: Comunidad) {
        val usuarioId = autenticacion.currentUser?.uid ?: return
        val ubicacionDestino = this.ubicacionDestino ?: return

        val distancia = ubicacionActual?.let {
            distancia(it.latitude, it.longitude, ubicacionDestino.latitude, ubicacionDestino.longitude)
        } ?: 0.0

        val carreraId = database.reference.child("carreras").push().key ?: return

        val participantesMap = comunidad.participantes.associateWith { true }

        val carrera = mapOf(
            "id" to carreraId,
            "organizadorId" to usuarioId,
            "fechaInicio" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()),
            "estado" to "pendiente",
            "inicioCarrera" to mapOf(
                "latitud" to ubicacionActual?.latitude,
                "longitud" to ubicacionActual?.longitude,
                "altitud" to ubicacionActual?.altitude
            ),
            "location" to mapOf(
                "latitud" to ubicacionDestino.latitude,
                "longitud" to ubicacionDestino.longitude,
                "altitud" to ubicacionDestino.altitude
            ),
            "distanciaTotal" to distancia,
            "participantes" to participantesMap,
            "comunidadId" to comunidad.nombre
        )

        database.reference.child("carreras").child(carreraId).setValue(carrera)
            .addOnSuccessListener {
                Toast.makeText(this, "Carrera creada para ${comunidad.nombre}", Toast.LENGTH_SHORT).show()
                enviarNotificacionesCarrera(comunidad, carreraId, usuarioId)
                actualizarUbicacionParticipante(carreraId)
                val intent = Intent(this, MapsActivity::class.java).apply {
                    putExtra("carrera_id", carreraId)
                    putExtra("admin", true)
                }
                startActivity(intent)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al crear carrera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun actualizarUbicacionParticipante(carreraId: String) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val usuarioId = autenticacion.currentUser?.uid ?: return

        val ubicacionCallback = object : LocationCallback() {
            override fun onLocationResult(resultado: LocationResult) {
                resultado.lastLocation?.let { ubicacion ->
                    val ubicacionData = mapOf(
                        "latitud" to ubicacion.latitude,
                        "longitud" to ubicacion.longitude,
                        "timestamp" to System.currentTimeMillis()
                    )

                    database.reference.child("carreras")
                        .child(carreraId)
                        .child("ubicacionesParticipantes") // Nodo para guardar ubicaciones por participante
                        .child(usuarioId)
                        .setValue(ubicacionData)
                }
            }
        }

        val solicitud = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        clienteUbicacion.requestLocationUpdates(solicitud, ubicacionCallback, Looper.getMainLooper())
    }

    private fun enviarNotificacionesCarrera(comunidad: Comunidad, carreraId: String, usuarioID: String) {

        val url = "https://us-central1-go-oter-ee454.cloudfunctions.net/notifyCommunityChallengeAvailable"

        // Crear mapa de datos para la petición
        val dataMap = mapOf(
            "nombreComunidad" to comunidad.nombre,
            "carreraUid" to carreraId,
            "creadorUid" to usuarioID
        )

        // Convertir a JSON con Gson
        val payload = Gson().toJson(dataMap)
        Log.d("EnviarNotificacion", "Enviando notificación de carrera: $payload")

        // Crear cuerpo de la petición
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = RequestBody.create(mediaType, payload)

        // Crear la petición
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        // Enviar la petición de forma asíncrona
        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("EnviarNotificacion", "Error al llamar a la Cloud Function", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e("EnviarNotificacion", "Error de Cloud Function: ${response.code}")
                } else {
                    // Procesar respuesta
                    val responseBody = response.body?.string()
                    Log.i("EnviarNotificacion", "Notificación enviada con éxito: $responseBody")

                    try {
                        // Parsear la respuesta JSON
                        val jsonResponse = JSONObject(responseBody)
                        val success = jsonResponse.optInt("success", 0)
                        val failure = jsonResponse.optInt("failure", 0)

                        // Opcional: Guardar registro
                    } catch (e: Exception) {
                        Log.e("EnviarNotificacion", "Error al procesar la respuesta", e)
                    }
                }

                response.close()
            }
        })

        //AQUI TAMBIEN CREARIA LA NOTIFICACION QUE SE VE EN EL HOME Y EN EL OTRO LADO

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
}