package com.example.locationmodule2mahendra

import android.Manifest
import android.annotation.SuppressLint

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.errors.ApiException
import com.google.maps.errors.OverQueryLimitException
import com.google.maps.model.DirectionsResult
import com.google.maps.model.TravelMode
import java.io.IOException
import java.lang.Math.atan2
import java.lang.Math.cos
import java.lang.Math.sin
import java.lang.Math.sqrt
class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val apiKey = "AIzaSyCR_3j59lgikagDIvdXRrLYkQjt8R8bN8U"
     private var totalDistance:Double?=0.0
    private val origin = createLocation(19.99727000, 73.79096000, "Nashik")
    var ori = origin
    private val deviceLocations = mutableListOf(
        createLocation(20.20000000, 73.83305556, "Dindori"),
        createLocation(18.520430, 73.856743, "Pune"),
        createLocation(19.218330, 72.978088, "Thane"),
        createLocation(28.535517, 77.391029, "Noida"),
    )
    private lateinit var mMap: GoogleMap
    private lateinit var near: TextView
    data class PolylineInfo(val ori: String, val dest: String,val distance: String)

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    override fun onCreate(savedInstanceState: Bundle?) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        near = findViewById(R.id.distanceNearest)
        if (isLocationPermissionGranted()) {
            initializeMap()
        } else {
            requestLocationPermission()
        }

    }
    @SuppressLint("SetTextI18n", "PotentialBehaviorOverride")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        try {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(origin.first, 8f))
            addMarker()

            try {

                while (deviceLocations.isNotEmpty()) {
                    //find minimum location
                    val minLocation = findNearestLocation(ori, deviceLocations)
                    if (minLocation != null) {
                        //draw route from ori and minLocation
                        val result = getDirections(ori.first, minLocation!!.first)
                        //getDirectionResponse : direction and distance
                        drawRoute(result.first!!, ori.second,minLocation.second, 0)
                        //change ori value to minLocation
                        ori=minLocation
                        //remove location from device location
                        removeLocation(minLocation.second)
                        //show message
                       // showToast(minLocation.second)
                       // showToast(ori.second)
                    } else {
                        //removeLocation()
                        showToast("No nearest location found")

                    }
                }

                if (deviceLocations.isEmpty()) {
                    showToast("Zero locations remaining")
                } else {
                    showToast("${deviceLocations.size} location(s) remaining")
                }


            } catch (e: Exception) {
                Toast.makeText(this, "error ${e.toString()}", Toast.LENGTH_SHORT).show()
            }
            Toast.makeText(this,"total distance $totalDistance", Toast.LENGTH_SHORT).show()
            near.text="total distance $totalDistance"
            //set on click listner on route
            mMap.setOnPolylineClickListener { polylines2 ->
                val polylineInfo = polylines2.tag as? PolylineInfo
                polylineInfo?.let {
                    val distance = it.distance
                    val ori = it.ori
                    val dest = it.dest
                    val distanceText2 = "Distance: $distance km, Origin: $ori ,Destination: $dest"
                    Toast.makeText(this@MainActivity, distanceText2, Toast.LENGTH_SHORT).show()
                }
            }


        } catch (e: Exception) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
        }
    }
    private fun addMarker() {
        mMap.addMarker(MarkerOptions().position(origin.first).title("origin"))

        for (loc in deviceLocations) {
            mMap.addMarker(MarkerOptions().position(loc.first).title(loc.second))
        }
    }
    //for creat location
    private fun createLocation(
        latitude: Double,
        longitude: Double,
        name: String
    ): Pair<LatLng, String> {
        return LatLng(latitude, longitude) to name
    }
    //for get Direction
    private fun getDirections(
        origin: LatLng,
        destination: LatLng
    ): Pair<DirectionsResult?, Double?> {
        val context = GeoApiContext.Builder().apiKey(apiKey).build()

        return try {
            val directions = DirectionsApi.newRequest(context).mode(TravelMode.DRIVING)
                .origin(com.google.maps.model.LatLng(origin.latitude, origin.longitude))
                .destination(
                    com.google.maps.model.LatLng(
                        destination.latitude,
                        destination.longitude
                    )
                ).await()

            val distanceInKm =
                directions?.routes?.getOrNull(0)?.legs?.getOrNull(0)?.distance?.inMeters?.toDouble()
                    ?.div(1000)
            //Toast.makeText(this, "$distanceInKm", Toast.LENGTH_SHORT).show()
            directions to distanceInKm

        } catch (e: ApiException) {
            Log.e("ApiException", e.message, e)
            null to null
        } catch (e: OverQueryLimitException) {
            Log.e("OverQueryLimitException", e.message, e)
            null to null
        } catch (e: IOException) {
            Log.e("IOException", e.message, e)
            null to null
        } catch (e: Exception) {
            Log.e("Exception", e.message, e)
            null to null
        }
    }
    //for draw  route
    private fun drawRoute(directionsResult: DirectionsResult,ori:String,dest: String, flag: Int) {
        val polylineOptions = PolylineOptions().width(5f).clickable(true).color(
                when (flag) {
                    0 -> Color.RED
                    1 -> Color.BLUE
                    else -> Color.BLUE
                }
            )
        val legs = directionsResult.routes[0].legs
        for (leg in legs) {
            val steps = leg.steps
            steps.forEach { step ->
                val points = step.polyline.decodePath()
                points.forEach { point ->
                    polylineOptions.add(LatLng(point.lat, point.lng))
                }
            }
        }
        val polylines2 = mMap.addPolyline(polylineOptions)
        //for find distance
        try {
            val distance2 = directionsResult.routes[0].legs.sumOf {
                it.distance.inMeters.toDouble() / 1000
            }.toFloat()
            totalDistance = totalDistance?.plus(distance2)
           // Toast.makeText(this, "total is $totalDistance", Toast.LENGTH_SHORT).show()
            polylines2.tag = PolylineInfo(ori,dest, distance2.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    //calculate Distance between origin and destination
    private fun calculateDistancefromOriginToDestApi(origin: LatLng, destination: LatLng): Double? {
        val context = GeoApiContext.Builder()
            .apiKey(apiKey)
            .build()

        return try {
            val directions = DirectionsApi.newRequest(context)
                .mode(TravelMode.DRIVING)
                .origin(com.google.maps.model.LatLng(origin.latitude, origin.longitude))
                .destination(com.google.maps.model.LatLng(destination.latitude, destination.longitude))
                .await()

            directions?.routes?.getOrNull(0)?.legs?.getOrNull(0)?.distance?.inMeters?.toDouble()?.div(1000)
        } catch (e: ApiException) {
            Log.e("ApiException", e.message, e)
            null
        } catch (e: OverQueryLimitException) {
            Log.e("OverQueryLimitException", e.message, e)
            null
        } catch (e: IOException) {
            Log.e("IOException", e.message, e)
            null
        } catch (e: Exception) {
            Log.e("Exception", e.message, e)
            null
        }
    }
    //for find Nearest Location
    //for check permission
    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    //request Location Permission
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }
    //Handle Permission if user allow , then initialize map otherwise close app
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, you can proceed
                    // Start using location-related functionality
                    initializeMap()
                } else {
                    // Permission denied, inform the user
                    // You might want to display a dialog or request the permission again
                    Toast.makeText(this, "Permission denied. Closing the app.", Toast.LENGTH_SHORT)
                        .show()
                    finish()
                }
            }
        }
    }
    //For Initialize Map
    private fun initializeMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        Toast.makeText(this, "Initialize Map", Toast.LENGTH_SHORT).show()
    }
    fun removeLocation(locationName: String) {
        val locationToRemove = deviceLocations.find {
            it.second == locationName
        }
        locationToRemove?.let {
            deviceLocations.remove(it)
        }
    }
    private fun findNearestLocation(
        origin: Pair<LatLng, String>,
        destinations: MutableList<Pair<LatLng, String>>
    ): Pair<LatLng, String>? {
        if (destinations.isEmpty()) return null

        var nearestLocation: Pair<LatLng, String>? = null
        var minDistance: Double? = null
       //  var distance: Double? =null
        for (destination in destinations) {
            //val distance = calculateDistance4(origin.first, destination.first)
            try {
                var distance = getDirections(origin.first, destination.first)
                // distance = calculateDistanceUsingEarthRadius(origin.first, destination.first)
                if (distance.second != null) {
                    if (minDistance == null || distance.second!! < minDistance) {
                        minDistance = distance.second
                        nearestLocation = destination
                    }
                } else {
                    Toast.makeText(this, "error : 433 nearest loc null", Toast.LENGTH_SHORT).show()
                    continue
                }
            }catch (e:Exception){
                Toast.makeText(this, "error 449 ${e.toString()}", Toast.LENGTH_SHORT).show()
            }

        }
             return nearestLocation
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    fun calculateDistanceUsingEarthRadius(from: LatLng, to: LatLng): Double {
        val earthRadius = 6371 // Earth's radius in kilometers

        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)

        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)

        val a =
            sin(dLat / 2) * sin(dLat / 2) + sin(dLon / 2) * sin(dLon / 2) * cos(lat1) * cos(lat2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c // Distance in kilometers
    }
}




