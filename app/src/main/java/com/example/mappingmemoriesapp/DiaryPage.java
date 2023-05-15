package com.example.mappingmemoriesapp;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mappingmemoriesapp.Models.PageLocation;
import com.example.mappingmemoriesapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.api.Page;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DiaryPage extends AppCompatActivity {

    private static final String TAG = "DiaryPage";

    //Firebase
    private FirebaseFirestore firebaseFirestore;

    private String documentId;
    private PageLocation pageLocation;

    Bundle extras;

    private ActivityResultLauncher<String> launcher;

    //widgets
    private ProgressBar progressBar;

    private TextView titletextview, geoPoint, timestamp;
    private EditText titleinput, textinput;
    private MaterialButton savebtn;
    private ImageView imageView;
    private Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_page);

        titletextview= findViewById(R.id.titletextview);
        geoPoint= findViewById(R.id.geopoint);
        titleinput= findViewById(R.id.titleinput);
        textinput= findViewById(R.id.textinput);
        savebtn= findViewById(R.id.savebtn);
        timestamp= findViewById(R.id.timestamp);
        imageView = findViewById(R.id.imageView);

        progressBar = findViewById(R.id.progressBar);

        firebaseFirestore = FirebaseFirestore.getInstance();

        //Obtiene el intent para comprobar si es una nueva página o una existente
        extras = getIntent().getExtras();
        if(extras.getBoolean("isNewPage")){
            setDirection(extras.getDouble("geoPoint_lat"), extras.getDouble("geoPoint_lon"));
        } else{
            setDirection(extras.getDouble("markerLat"), extras.getDouble("markerLon"));
            titleinput.setText(extras.getString("title"));
            titletextview.setText("Tu página de diario");

            searchDocument();
        }

        //Botón para guardar los cambios
        savebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(extras.getBoolean("isNewPage")){
                    saveNewPage();
                }else{
                    uploadPage();
                }
            }
        });

        //Launcher para cargar una imagen de galeria
        launcher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri result) {
                        if (result != null) {
                            uri= result;
                            imageView.setImageURI(result);
                        }
                    }
                }
        );

        //ImageView para mostrar una imagen de galeria
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launcher.launch("image/*");
            }
        });
    }

    //Actualiza la pagina existente con los datos que han sido modificados de la misma
    private void uploadPage() {
        showDialog();

        pageLocation = new PageLocation();
        pageLocation.setTitle(titleinput.getText().toString());
        pageLocation.setText(textinput.getText().toString());
        if(uri != null){
            pageLocation.setImage(uri.toString());
        } else {
            pageLocation.setImage("0");
        }

        firebaseFirestore.collection("PageLocations").document(documentId).update(
                "title", pageLocation.getTitle(),
                "text", pageLocation.getText(),
                "image", pageLocation.getImage()
        ).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                hideDialog();
                if (task.isSuccessful()) {
                    Log.d(TAG, "uploadPage: Updated page location in Firestore." +
                            "\npageLocation: " + pageLocation.toString());

                    Toast.makeText(DiaryPage.this, "Pagina guardada", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(DiaryPage.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });

    }

    //Busca el documento existente en la base de datos de Firebase
    private void searchDocument() {
        showDialog();

        CollectionReference collectionReference = firebaseFirestore.collection("PageLocations");
        Query query = collectionReference.whereEqualTo("geo_point", new GeoPoint(extras.getDouble("markerLat"), extras.getDouble("markerLon")))
                .whereEqualTo("title", extras.getString("title"))
                .whereEqualTo("user_id", FirebaseAuth.getInstance().getUid());

        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                hideDialog();
                if (task.isSuccessful()) {
                    List<DocumentSnapshot> lista = task.getResult().getDocuments();
                    if (!lista.isEmpty()) {
                        documentId = lista.get(0).getId();// ID del primer documento encontrado

                        Log.d(TAG, "searchDocument: document found:" + documentId);

                        loadData();
                    }
                }
            }
        });
    }

    //Se cargan los datos de una página existente en Firebase del usuario
    private void loadData() {
        showDialog();

        firebaseFirestore.collection("PageLocations").document(documentId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                hideDialog();

                if(task.isSuccessful()) {
                    DocumentSnapshot documentSnapshot = task.getResult();
                    if(documentSnapshot.exists()){
                        String text2 = documentSnapshot.getString("text");
                        textinput.setText(text2);
                        Timestamp gettimestamp = documentSnapshot.getTimestamp("timestamp");
                        if (gettimestamp != null) {
                            timestamp.setText(gettimestamp.toDate().toString());
                        }
                        Uri uri1 = Uri.parse(documentSnapshot.getString("image"));
                        imageView.setImageURI(uri1);

                        Log.d(TAG, "loadData: data loaded succesfully");
                    }
                }else{
                    Log.d(TAG, "loadData: data could not be loaded");
                }
            }
        });
    }

    //Guarda una nueva página en la base de datos
    private void saveNewPage() {
        showDialog();

        PageLocation pageLocation= new PageLocation();
        pageLocation.setUser_id(FirebaseAuth.getInstance().getUid());

        //Comprueba si hay imagen
        if(uri != null){
            pageLocation.setImage(uri.toString());
        } else {
            pageLocation.setImage("0");
        }
        pageLocation.setTitle(titleinput.getText().toString());
        pageLocation.setText(textinput.getText().toString());
        pageLocation.setTimestamp(null);
        pageLocation.setGeo_point(new GeoPoint(extras.getDouble("geoPoint_lat"), extras.getDouble("geoPoint_lon")));

        firebaseFirestore.collection("PageLocations").add(pageLocation).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
            @Override
            public void onComplete(@NonNull Task<DocumentReference> task) {
                hideDialog();

                if(task.isSuccessful()){
                    Log.d(TAG, "savePageLocation: \ninserted page location into database." +
                            "\n pageLocation: " + pageLocation.toString());
                    Toast.makeText(DiaryPage.this, "Pagina guardada", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(DiaryPage.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }

    //Método para establecer una dirección a partir de unas coordenadas
    public void setDirection(Double lat, Double lon){
        geoPoint.setText("[" + lat + "," + lon + "]");

        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(lat, lon, 1);
            String address = addresses.get(0).getAddressLine(0);
            geoPoint.setText("[" + lat + "," + lon + "] " + address);

            Log.d(TAG, "setDirection: direction set:" + geoPoint.getText());

        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "setDirection: direction not set");
        }

    }

    private void showDialog(){
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideDialog(){
        progressBar.setVisibility(View.GONE);
    }
}