package com.example.mappingmemoriesapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    //Clase para recibir y manejar eventos de las geofences

    private static final String CHANNEL_ID = "geofence_channel";
    private static final int NOTIFICATION_ID = 123;
    private static final String GEOFENCE_ACTION = "com.example.ACTION_GEOFENCE";

    //Verifica si se entra en la geofence
    @Override
    public void onReceive(Context context, Intent intent) {

        if (GeofencingEvent.fromIntent(intent).getGeofenceTransition() == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Intent geofenceIntent = new Intent(GEOFENCE_ACTION);
            context.sendBroadcast(geofenceIntent);

            //mostrar la notificación
            showNotification(context);
        }
    }

    public void showNotification(Context context) {

        // Crear y configurar la notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("¡Ubicación guardada cerca de ti!")
                .setContentText("Estas pasando cerca de una ubicación que has guardado ¿quieres verla?")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Crear el canal de notificación
        CharSequence channelName = "Geofence Channel";
        String channelDescription = "Channel for geofence notifications";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, channelName, importance);
        channel.setDescription(channelDescription);

        // Registrar el canal de notificación en el administrador de notificaciones
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
        builder.setContentIntent(pendingIntent);

        // Mostrar la notificación
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        notificationManagerCompat.notify(NOTIFICATION_ID, builder.build());
    }
}
