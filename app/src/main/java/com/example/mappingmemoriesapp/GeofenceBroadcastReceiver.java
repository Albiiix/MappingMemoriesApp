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

    private  String token;
    private static final String CHANNEL_ID = "geofence_channel";
    private static final int NOTIFICATION_ID = 123;
    private static final String GEOFENCE_ACTION = "com.example.ACTION_GEOFENCE";

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (GeofencingEvent.fromIntent(intent).getGeofenceTransition() == Geofence.GEOFENCE_TRANSITION_ENTER) {
            // Crear un Intent con la acción personalizada
            Intent geofenceIntent = new Intent(GEOFENCE_ACTION);

            // Envía la transmisión con el Intent
            context.sendBroadcast(geofenceIntent);

            // Mostrar la notificación
            showNotification(context);
        }
    }

    public void showNotification(Context context) {

        // Crear y configurar la notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("Geofence Notification")
                .setContentText("You have entered a geofence")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Crear el canal de notificación para dispositivos con Android 8.0 o posterior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence channelName = "Geofence Channel";
            String channelDescription = "Channel for geofence notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, channelName, importance);
            channel.setDescription(channelDescription);

            // Registrar el canal de notificación en el administrador de notificaciones
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
        builder.setContentIntent(pendingIntent);

        // Mostrar la notificación
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        notificationManagerCompat.notify(NOTIFICATION_ID, builder.build());

        System.out.println("NOTIFICACION!!!!!!!: ");


/*
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if(!task.isSuccessful()){
                            Log.w("GeofenceReceiver", "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        token = task.getResult();
                        Log.d("GeofenceReceiver", "FCM registration token: " + token);

                        System.out.println("ESTA DEBERIA SER EL TOKEN!!!!!!!: " + token);

                        // Construir el mensaje de notificación
                        RemoteMessage.Builder remoteMessageBuilder = new RemoteMessage.Builder("122290392444" + "@fcm.googleapis.com")
                                .setMessageId(Integer.toString(getNextMessageId()))
                                .addData("title", title)
                                .addData("body", body)
                                .addData("click_action", "OPEN_ACTIVITY")
                                .addData("token", token); // Acción al hacer clic en la notificación

                        // Enviar la notificación mediante FirebaseMessaging
                        FirebaseMessaging.getInstance().send(remoteMessageBuilder.build());
                        System.out.println("DEBERIA HABERSE MANDADO MENSAJE!!!!!!!: ");


                    }
                });

*/

    }

    private int getNextMessageId() {
        // Generar un ID único para cada mensaje de notificación
        // Aquí puedes implementar tu lógica para generar un ID único, por ejemplo, utilizando un contador o una marca de tiempo
        return (int) System.currentTimeMillis();
    }
}
