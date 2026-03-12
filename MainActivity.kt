package com.example.locationtelegramapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val shareButton: Button = findViewById(R.id.shareLocationButton)
        shareButton.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            } else {
                getLocationAndSendToTelegram()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationAndSendToTelegram()
            } else {
                Toast.makeText(this, "Location access denied. Can't share location.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getLocationAndSendToTelegram() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                sendLocationToTelegramBot(location.latitude, location.longitude)
            } else {
                Toast.makeText(this, "Failed to get location.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendLocationToTelegramBot(latitude: Double, longitude: Double) {
        val botToken = "YOUR_TELEGRAM_BOT_TOKEN" // Replace with your bot token
        val chatId = "YOUR_CHAT_ID" // Replace with your chat ID

        val url = "https://api.telegram.org/bot$botToken/sendLocation"
        val data = JsonObject()
        data.addProperty("latitude", latitude)
        data.addProperty("longitude", longitude)

        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val outputStream = connection.outputStream
            val writer = java.io.OutputStreamWriter(outputStream)
            writer.write(data.toString())
            writer.flush()
            outputStream.close()

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                Toast.makeText(this, "Location sent to Telegram!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to send location. Code: $responseCode", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error sending location: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
