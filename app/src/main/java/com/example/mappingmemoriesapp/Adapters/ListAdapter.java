package com.example.mappingmemoriesapp.Adapters;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mappingmemoriesapp.DiaryPage;
import com.example.mappingmemoriesapp.Models.PageLocation;
import com.example.mappingmemoriesapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.MyViewHolder>  {

    private static final String TAG = "ListAdapter";

    //Adaptador de la lista de páginas guardadas (PageLocation) para el RecyclerView

    Context context;
    List<PageLocation> pagesList;
    List<PageLocation> pagesListOriginal;

    //Constructor
    public ListAdapter(Context context, List<PageLocation> pagesList){
        this.context= context;
        this.pagesList= pagesList;
        this.pagesListOriginal = pagesList;
    }

    //Infla el diseño de la view del elemento del recyclerView
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.item_view,parent,false));
    }

    //Para los datos del objeto PageLocation en la view
    @Override
    public void onBindViewHolder(@NonNull ListAdapter.MyViewHolder holder, int position) {
        PageLocation pageLocation= pagesList.get(position);
        holder.titleOutput.setText(pageLocation.getTitle());
        holder.directionOutput.setText(getDirection(pageLocation.getGeo_point().getLatitude(), pageLocation.getGeo_point().getLongitude()));
        holder.timeOutput.setText(pageLocation.getTimestamp().toString());

        //Si se pulsa unos segundos el item se puede eliminar
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                //Salta un menú para dar la opción de eliminar la página
                PopupMenu menu = new PopupMenu(context,v);
                menu.getMenu().add("DELETE");
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if(item.getTitle().equals("DELETE")){

                            Log.d(TAG, "onLongClick: delete clicked");

                            //Se borra el documento del Firebase después de encontrarlo
                            FirebaseFirestore mDb= FirebaseFirestore.getInstance();
                            CollectionReference collectionReference = mDb.collection("PageLocations");
                            Query query = collectionReference.whereEqualTo("geo_point", pageLocation.getGeo_point())
                                    .whereEqualTo("timestamp", pageLocation.getTimestamp())
                                    .whereEqualTo("user_id", FirebaseAuth.getInstance().getUid());
                            query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {

                                        Log.d(TAG, "onComplete: successfully found document to delete.");


                                        List<DocumentSnapshot> lista = task.getResult().getDocuments();
                                        if (!lista.isEmpty()) {
                                            String documentId = lista.get(0).getId();
                                            collectionReference.document(documentId).delete();
                                            Toast.makeText(context,"Página eliminada",Toast.LENGTH_SHORT).show();
                                        }
                                    }else{
                                        Log.d(TAG, "onComplete: document not found");
                                    }
                                }
                            });
                        }
                        return true;
                    }
                });
                menu.show();
                return true;
            }
        });

        //Si se pulsa una vez el item, se va a otra actividad que muestra la información detallada
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d(TAG, "onClick: item clicked");

                Double lat = pageLocation.getGeo_point().getLatitude();
                Double lon = pageLocation.getGeo_point().getLongitude();

                Intent intent = new Intent(context, DiaryPage.class);
                intent.putExtra("title", pageLocation.getTitle());
                intent.putExtra("snippet", pageLocation.getTimestamp().toString());
                intent.putExtra("markerLat", lat);
                intent.putExtra("markerLon", lon);
                intent.putExtra("isNewPage", false);
                context.startActivity(intent);
            }
        });
    }

    //Método para obtener la dirección correspondiente a unas coordenadas
    public String getDirection(Double lat, Double lon){
        String direction = "";

        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(context, Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(lat, lon, 1);
            String address = addresses.get(0).getAddressLine(0);
            direction = "[" + lat + "," + lon + "] " + address;

            Log.d(TAG, "getDirection: direction obtained: " + direction);

        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "getDirection: direction not obtained");
        }
        return direction;
    }

    //Filtro para realizar búsquedas en la lista de las PageLocation según el texto proporcionado
    //Puede buscarse el titulo, la fecha o la dirección
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                Log.d(TAG, "getFilter: performed filtering");

                FilterResults results = new FilterResults();
                List<PageLocation> filteredList = new ArrayList<>();

                if (pagesListOriginal == null) {
                    pagesListOriginal = new ArrayList<>(pagesList);
                }

                if (constraint == null || constraint.length() == 0) {
                    filteredList.addAll(pagesListOriginal);
                } else {
                    String filterPattern = constraint.toString().toLowerCase(Locale.getDefault());
                    for (PageLocation pageLocation : pagesListOriginal) {
                        GeoPoint geoPoint = pageLocation.getGeo_point();
                        String dir = getDirection(geoPoint.getLatitude(), geoPoint.getLongitude());

                        if (pageLocation.getTitle().toLowerCase(Locale.getDefault()).contains(filterPattern)
                            || pageLocation.getTimestamp().toString().toLowerCase(Locale.getDefault()).contains(filterPattern)
                            || dir.toLowerCase(Locale.getDefault()).contains(filterPattern)
                        ){
                            filteredList.add(pageLocation);
                        }
                    }
                }

                results.values = filteredList;
                results.count = filteredList.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {

                Log.d(TAG, "getFilter: publishResults");

                pagesList.clear();
                pagesList.addAll((List<PageLocation>) results.values);
                notifyDataSetChanged();
            }
        };
    }

    //Elementos de la lista
    @Override
    public int getItemCount() {
        return pagesList.size();
    }

    //Referencias de la view de cada elemento
    public class MyViewHolder extends RecyclerView.ViewHolder{

        TextView titleOutput;
        TextView directionOutput;
        TextView timeOutput;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            titleOutput = itemView.findViewById(R.id.titleoutput);
            directionOutput = itemView.findViewById(R.id.directionoutput);
            timeOutput = itemView.findViewById(R.id.timeoutput);
        }
    }


}
