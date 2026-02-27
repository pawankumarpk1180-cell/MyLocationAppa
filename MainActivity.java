package com.locationapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class MainActivity extends AppCompatActivity {

    // =====================================================
    // ⚠️  YAHAN APNA BOT TOKEN AUR CHAT ID DAALO  ⚠️
    // =====================================================
    private static final String BOT_TOKEN = "8745133653:AAFrtqs0x--WFM2PVhCZ3OaJd7gnavGa35k";
    private static final String CHAT_ID   = "6847565190";
    // =====================================================

    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private FusedLocationProviderClient fusedLocationClient;
    private TextView statusText;
    private Button allowBtn, denyBtn;
    private ImageView iconView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        statusText = findViewById(R.id.statusText);
        allowBtn   = findViewById(R.id.allowBtn);
        denyBtn    = findViewById(R.id.denyBtn);
        iconView   = findViewById(R.id.iconView);

        allowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestLocationPermission();
            }
        });

        denyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                statusText.setText("❌ Location access denied.\nApp cannot send location.");
                allowBtn.setVisibility(View.VISIBLE);
                denyBtn.setVisibility(View.GONE);
            }
        });
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            getLocationAndSend();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationAndSend();
            } else {
                statusText.setText("❌ Permission denied!\nPlease allow location to continue.");
            }
        }
    }

    private void getLocationAndSend() {
        statusText.setText("📡 Getting your location...");
        allowBtn.setVisibility(View.GONE);
        denyBtn.setVisibility(View.GONE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();
                    statusText.setText("✅ Location Found!\n\nLat: " + lat + "\nLon: " + lon
                            + "\n\n📤 Sending to Telegram...");
                    sendToTelegram(lat, lon);
                } else {
                    statusText.setText("⚠️ Location not available.\nPlease enable GPS and try again.");
                    allowBtn.setText("Try Again");
                    allowBtn.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void sendToTelegram(final double lat, final double lon) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Send location as map link
                    String mapLink = "https://maps.google.com/?q=" + lat + "," + lon;
                    String message = "📍 *New Location Received!*\n\n"
                            + "🌐 Latitude: `" + lat + "`\n"
                            + "🌐 Longitude: `" + lon + "`\n\n"
                            + "🗺️ [Open in Google Maps](" + mapLink + ")";

                    String encodedMsg = URLEncoder.encode(message, "UTF-8");
                    String urlStr = "https://api.telegram.org/bot" + BOT_TOKEN
                            + "/sendMessage?chat_id=" + CHAT_ID
                            + "&text=" + encodedMsg
                            + "&parse_mode=Markdown";

                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10000);
                    int responseCode = conn.getResponseCode();

                    // Also send native Telegram location
                    String locUrl = "https://api.telegram.org/bot" + BOT_TOKEN
                            + "/sendLocation?chat_id=" + CHAT_ID
                            + "&latitude=" + lat
                            + "&longitude=" + lon;
                    URL locUrlObj = new URL(locUrl);
                    HttpURLConnection locConn = (HttpURLConnection) locUrlObj.openConnection();
                    locConn.setRequestMethod("GET");
                    locConn.setConnectTimeout(10000);
                    int locResponse = locConn.getResponseCode();

                    final boolean success = (responseCode == 200 || locResponse == 200);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                statusText.setText("✅ Location sent to Telegram!\n\n"
                                        + "📍 Lat: " + lat + "\n"
                                        + "📍 Lon: " + lon + "\n\n"
                                        + "✔️ Check your Telegram bot!");
                                Toast.makeText(MainActivity.this,
                                        "Location sent successfully!", Toast.LENGTH_LONG).show();
                            } else {
                                statusText.setText("❌ Failed to send!\nCheck Bot Token & Chat ID.");
                            }
                        }
                    });

                } catch (Exception e) {
                    final String error = e.getMessage();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statusText.setText("❌ Error: " + error
                                    + "\n\nCheck internet & Bot Token.");
                        }
                    });
                }
            }
        }).start();
    }
}
