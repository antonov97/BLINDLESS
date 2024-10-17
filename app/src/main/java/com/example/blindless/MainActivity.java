package com.example.blindless;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.speech.tts.TextToSpeech;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int REQUEST_CALL_PERMISSION = 1;
    private TextToSpeech tts; // Instancia para Text-to-Speech

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar Text-to-Speech
        tts = new TextToSpeech(this, this);

        // Geolocalización - Abrir Google Maps
        ImageButton ibGeolocalizacion = findViewById(R.id.ibGeolocalizacion);
        ibGeolocalizacion.setOnClickListener(v -> {
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=Radio+Shack");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            startActivity(mapIntent);
        });

        // Contactos - Abrir la lista de contactos
        ImageButton ibContactos = findViewById(R.id.ibContactos);
        ibContactos.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            startActivity(intent);
        });

        // Llamada de Emergencia
        ImageButton ibNumeroemergencia = findViewById(R.id.ibNumeroemergencia);
        ibNumeroemergencia.setOnClickListener(v -> makePhoneCall());

        // Lector QR - Abrir la cámara para escanear QR
        ImageButton ibQr = findViewById(R.id.ibQr);
        ibQr.setOnClickListener(v -> iniciarEscaneoQr()); // Ahora abre el lector QR

        // Compartir ubicación
        ImageButton ibCompartirubicacion = findViewById(R.id.ibCompartirubicacion);
        ibCompartirubicacion.setOnClickListener(v -> {
            String uriText = "geo:0,0?q=Radio+Shack";
            Uri uri = Uri.parse(uriText);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Mi ubicación: " + uri.toString());
            shareIntent.setType("text/plain");
            startActivity(Intent.createChooser(shareIntent, "Compartir ubicación"));
        });
    }

    // Método para iniciar el escaneo del código QR
    private void iniciarEscaneoQr() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Escaneando código QR...");
        integrator.setOrientationLocked(true);
        integrator.setBeepEnabled(true);
        integrator.initiateScan();
    }

    // Método para recibir el resultado del escaneo
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                String qrContent = result.getContents();
                try {
                    // Procesar contenido JSON del QR
                    JSONObject json = new JSONObject(qrContent);
                    String descripcion = json.getString("descripcion");
                    String fechadeCaducidad = json.getString("fechadeCaducidad");

                    // Mostrar como Toast
                    Toast.makeText(this, "Se ha escaneado un producto. La descripción del producto es: " + descripcion + ", y la fecha de expiración es: " + fechadeCaducidad, Toast.LENGTH_LONG).show();

                    // Leer en voz alta el contenido
                    String textoALeer = "Se ha escaneado un producto. La descripción del producto es: " + descripcion + ", y la fecha de expiración es: " + fechadeCaducidad;
                    speakOut(textoALeer); // Método para leer el texto en voz alta
                } catch (JSONException e) {
                    Toast.makeText(this, "QR no contiene un formato JSON válido", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "No se encontró contenido en el QR", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    // Método para realizar una llamada
    private void makePhoneCall() {
        String phoneNumber = "123456789"; // Número de emergencia
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PERMISSION);
        } else {
            startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumber)));
        }
    }

    // Método para manejar permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CALL_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                makePhoneCall();
            }
        }
    }

    // Inicializar Text-to-Speech
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.getDefault()); // Idioma predeterminado del dispositivo

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Lenguaje no soportado", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Inicialización de Text-to-Speech fallida", Toast.LENGTH_SHORT).show();
        }
    }

    // Método para leer el texto en voz alta
    private void speakOut(String text) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    // Detener TTS cuando la actividad se destruye
    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
