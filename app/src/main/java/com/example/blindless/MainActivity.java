package com.example.blindless;

import static java.lang.Thread.sleep;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.speech.tts.TextToSpeech;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int REQUEST_CALL_PERMISSION = 1;
    private static final int REQUEST_LOCATION_PERMISSION = 2;
    private TextToSpeech tts; // Instancia para Text-to-Speech
    int[] bandera = new int[9]; // Bandera para controlar cada botón
    private final Handler handler = new Handler(); // Handler para gestionar los temporizadores
    private FusedLocationProviderClient fusedLocationClient; // Cliente para obtener la ubicación

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar Text-to-Speech
        tts = new TextToSpeech(this, this);

        // Inicializar cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Geolocalización - Obtener la ubicación y decir la calle actual
        ImageButton ibGeolocalizacion = findViewById(R.id.ibGeolocalizacion);
        ibGeolocalizacion.setContentDescription("Botón para abrir la geolocalización en Google Maps");
        ibGeolocalizacion.setOnClickListener(v -> {
            if (bandera[0] == 0) {
                String textoALeer = "Apretaste el botón para obtener tu ubicación. Si deseas saber dónde estás, vuelve a apretar el botón.";
                speakOut(textoALeer);
                bandera[0] = 1;
                resetBanderaAfterDelay(5);
            } else {
                obtenerUbicacionPrecisa();
                bandera[0] = 0;
            }
        });

        // Contactos - Abrir la lista de contactos
        ImageButton ibContactos = findViewById(R.id.ibContactos);
        ibContactos.setContentDescription("Botón para abrir la lista de contactos");
        ibContactos.setOnClickListener(v -> {
            if (bandera[1] == 0) {
                String textoALeer = "Apretaste el botón para abrir tus contactos. Si deseas continuar, vuelve a apretar el botón.";
                speakOut(textoALeer);
                bandera[1] = 1;
                resetBanderaAfterDelay(5);
            } else {
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivity(intent);
                bandera[1] = 0;
            }
        });

        // Llamada de Emergencia
        ImageButton ibNumeroemergencia = findViewById(R.id.ibNumeroemergencia);
        ibNumeroemergencia.setContentDescription("Botón para realizar una llamada de emergencia");
        ibNumeroemergencia.setOnClickListener(v -> {
            if (bandera[2] == 0) {
                String textoALeer = "Apretaste el botón para realizar una llamada de emergencia. Si deseas continuar, vuelve a apretar el botón.";
                speakOut(textoALeer);
                bandera[2] = 1;
                resetBanderaAfterDelay(5);
            } else {
                try {
                    makePhoneCall();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                bandera[2] = 0;
            }
        });

        // Lector QR - Abrir la cámara para escanear QR
        ImageButton ibQr = findViewById(R.id.ibQr);
        ibQr.setContentDescription("Botón para escanear un código QR");
        ibQr.setOnClickListener(v -> iniciarEscaneoQr());

        // Compartir ubicación
        ImageButton ibCompartirubicacion = findViewById(R.id.ibCompartirubicacion);
        ibCompartirubicacion.setContentDescription("Botón para compartir tu ubicación");
        ibCompartirubicacion.setOnClickListener(v -> {
            if (bandera[3] == 0) {
                String textoALeer = "Apretaste el botón para compartir tu ubicación. Si deseas continuar, vuelve a apretar el botón.";
                speakOut(textoALeer);
                bandera[3] = 1;
                resetBanderaAfterDelay(5);
            } else {
                obtenerUbicacionParaCompartir();
                bandera[3] = 0;
            }
        });
    }

    // Método para obtener la ubicación precisa
    private void obtenerUbicacionPrecisa() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        // Ubicación obtenida con éxito
                        if (location != null) {
                            obtenerDireccion(location);
                        } else {
                            String textoALeer = "No se pudo obtener la ubicación, por favor, verifica que tienes activado el GPS, y cierra y abre la aplicación nuevamente.";
                            speakOut(textoALeer);
                        }
                    });
        }
    }

    // Método para obtener la ubicación para compartir
    private void obtenerUbicacionParaCompartir() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            double latitud = location.getLatitude();
                            double longitud = location.getLongitude();
                            String uriText = "https://www.google.com/maps?q=" + latitud + "," + longitud;
                            String textoALeer = "Estás a punto de compartir tu ubicación, selecciona la aplicación de tu preferencia para compartirla.";
                            speakOut(textoALeer);
                            Uri uri = Uri.parse(uriText);
                            Intent shareIntent = new Intent(Intent.ACTION_SEND);
                            shareIntent.putExtra(Intent.EXTRA_TEXT, "Mi ubicación: " + uri.toString());
                            shareIntent.setType("text/plain");
                            startActivity(Intent.createChooser(shareIntent, "Compartir ubicación"));
                        } else {
                            String textoALeer = "No se pudo obtener la ubicación, por favor, verifica que tienes activado el GPS, y cierra y abre la aplicación nuevamente.";
                            speakOut(textoALeer);
                        }
                    });
        }
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
                    JSONObject json = new JSONObject(qrContent);
                    String descripcion = json.getString("descripcion");
                    String fechadeCaducidad = json.getString("fechadeCaducidad");
                    Toast.makeText(this, "Se ha escaneado un producto. La descripción del producto es: " + descripcion + ", y la fecha de expiración es: " + fechadeCaducidad, Toast.LENGTH_LONG).show();
                    String textoALeer = "Se ha escaneado un producto. La descripción del producto es: " + descripcion + ", y la fecha de expiración es: " + fechadeCaducidad;
                    speakOut(textoALeer);
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


    // Método para obtener la dirección a partir de la ubicación
    private void obtenerDireccion(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> direcciones = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (direcciones != null && !direcciones.isEmpty()) {
                Address direccion = direcciones.get(0);
                String calle = direccion.getThoroughfare();
                String numero = direccion.getSubThoroughfare();
                String colonia = direccion.getSubLocality();
                String ciudad = direccion.getLocality();
                String codigoPostal = direccion.getPostalCode();
                String textoALeer = "Estás en " + calle + ", número " + numero + ", en la colonia " + colonia + ", "
                        + ciudad + ", código postal " + codigoPostal + ".";
                speakOut(textoALeer);
            } else {
                String textoALeer = "No se pudo obtener una dirección precisa.";
                speakOut(textoALeer);
            }
        } catch (IOException e) {
            e.printStackTrace();
            String textoALeer = "Hubo un problema al obtener la dirección.";
            speakOut(textoALeer);
        }
    }

    // Método para iniciar el escaneo del código QR
    private void iniciarEscaneoQr() {
        if (bandera[4] == 0) {
            String textoALeer = "Apretaste el botón de escanear código QR. Si deseas escanear el producto, vuelve a apretar el botón.";
            speakOut(textoALeer);
            bandera[4] = 1;
            resetBanderaAfterDelay(5);
        } else {
            String textoALeer = "Se abrió la cámara para escanear el código QR. Acerca la cámara al código QR del producto.";
            speakOut(textoALeer);
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Escaneando código QR...");
            integrator.setOrientationLocked(true);
            integrator.setBeepEnabled(true);
            integrator.initiateScan();
            bandera[4] = 0;
        }
    }

    // Método para realizar una llamada
    private void makePhoneCall() throws InterruptedException {
        String phoneNumber = "088"; // Número de emergencia
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PERMISSION);
        } else {
            String textoALeer = "Se está llamando a emergencias.";
            speakOut(textoALeer);
            sleep(1800);
            startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumber)));
        }
    }


    // Método para hablar
    private void speakOut(String texto) {
        tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    // Método para restablecer las banderas después de un retraso
    private void resetBanderaAfterDelay(int index) {
        handler.postDelayed(() -> bandera[index] = 0, 2000); // Restablece la bandera después de 2 segundos
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CALL_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, puedes hacer la llamada
            } else {
                Toast.makeText(this, "Permiso para realizar llamadas denegado", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, puedes acceder a la ubicación
            } else {
                Toast.makeText(this, "Permiso para acceder a la ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.getDefault());
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Lenguaje no soportado", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Error en la inicialización del TTS", Toast.LENGTH_SHORT).show();
        }
    }
}
