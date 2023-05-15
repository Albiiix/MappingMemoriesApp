package com.example.mappingmemoriesapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SearchView;

import com.example.mappingmemoriesapp.Adapters.ListAdapter;
import com.example.mappingmemoriesapp.Models.PageLocation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PagesList extends AppCompatActivity{

    private static final String TAG = "PagesList";

    List<PageLocation> pagesList = new ArrayList<>();
    RecyclerView recyclerView;
    ListAdapter listAdapter;

    //widgets
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pages_list);

        progressBar = findViewById(R.id.progressBar);
        swipeRefreshLayout= findViewById(R.id.swipeRefreshLayout);
        searchView = findViewById(R.id.searchView);
        
        loadPagesList();
        recyclerView= findViewById(R.id.recyclerviewPages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        listAdapter= new ListAdapter(PagesList.this, pagesList);
        recyclerView.setAdapter(listAdapter);

        //Para refrescar la página cuando se elimina o busca
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                pagesList.clear();
                loadPagesList();
            }
        });

        //Buscador por titulo/fecha/dirección
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                listAdapter.getFilter().filter(newText);
                return true;
            }
        });
    }

    //Carga las páginas de diario del usuario
    private void loadPagesList() {
        showDialog();
        FirebaseFirestore.getInstance().collection("PageLocations")
                .whereEqualTo("user_id", FirebaseAuth.getInstance().getUid()).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        hideDialog();
                        if(task.isSuccessful()){
                            List<DocumentSnapshot> lista = task.getResult().getDocuments();
                            if(!lista.isEmpty()){
                                for(int i = 0; i < lista.size(); i++){
                                    PageLocation pageLocation= new PageLocation();
                                    pageLocation.setGeo_point((GeoPoint) lista.get(i).getData().get("geo_point"));
                                    pageLocation.setTitle(lista.get(i).getData().get("title").toString());
                                    Timestamp timestamp= (Timestamp) lista.get(i).getData().get("timestamp");
                                    pageLocation.setTimestamp(timestamp.toDate());
                                    pagesList.add(pageLocation);
                                }
                                Log.d(TAG, "loadPagesList:onComplete: succesful" + task.isSuccessful());
                            }
                            listAdapter.notifyDataSetChanged();
                        }
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    private void showDialog(){
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideDialog(){
        progressBar.setVisibility(View.GONE);
    }
}