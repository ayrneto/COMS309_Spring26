package com.example.androidexample;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.json.JSONException;
import org.json.JSONObject;

import io.reactivex.disposables.CompositeDisposable;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;

public class WebSocketService extends Service {

    private StompClient stompClient;
    private CompositeDisposable compositeDisposable;
    private static final String TAG = "WebSocketService";

    @Override
    public void onCreate() {
        super.onCreate();
        compositeDisposable = new CompositeDisposable();
        connectWebSocket();
    }

    private void connectWebSocket() {
        // Get userId from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        String userId = prefs.getString("USER_ID", "-1");

        if (userId.equals("-1")) {
            Log.e(TAG, "No user logged in");
            return;
        }

        // Connect to STOMP WebSocket - use your backend URL
        String wsUrl = "ws://REDACTED:8080/websocket";  // TODO: Update with your backend IP
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl);

        // Listen for connection
        compositeDisposable.add(stompClient.lifecycle().subscribe(lifecycleEvent -> {
            switch (lifecycleEvent.getType()) {
                case OPENED:
                    Log.d(TAG, "WebSocket Connected");
                    subscribeToTopic(userId);
                    break;
                case ERROR:
                    Log.e(TAG, "WebSocket Error", lifecycleEvent.getException());
                    break;
                case CLOSED:
                    Log.d(TAG, "WebSocket Closed");
                    break;
            }
        }));

        stompClient.connect();
    }

    private void subscribeToTopic(String userId) {
        // Subscribe to user-specific topic
        compositeDisposable.add(stompClient.topic("/topic/user/" + userId)
                .subscribe(topicMessage -> {
                    Log.d(TAG, "Message received: " + topicMessage.getPayload());
                    handleTaskNotification(topicMessage.getPayload());
                }, throwable -> {
                    Log.e(TAG, "Error subscribing to topic", throwable);
                }));
    }

    private void handleTaskNotification(String message) {
        try {
            JSONObject json = new JSONObject(message);

            // Check event type
            String eventType = json.optString("eventType", "");

            long taskId = json.getLong("taskId");
            String title = json.getString("title");
            String description = json.getString("description");
            String dueDate = json.optString("dueDate", "No due date");
            String status = json.optString("status", "Not Started");

            if (eventType.equals("TASK_ASSIGNED")) {
                showNotification(taskId, title, description, dueDate, status);
            } else if (eventType.equals("TASK_REMINDER")) {
                showReminderNotification(taskId, title, description, dueDate);
            }

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing notification: " + e.getMessage());
        }
    }

    private void showReminderNotification(long taskId, String title, String description, String dueDate) {
        Intent intent = new Intent(this, TaskDetailsActivity.class);
        intent.putExtra("taskId", taskId);
        intent.putExtra("title", title);
        intent.putExtra("description", description);
        intent.putExtra("dueDate", dueDate);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (int) taskId + 1000,  // Different request code for reminders
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "TASK_CHANNEL")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Task Reminder")
                .setContentText(title + " - Due: " + dueDate)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(description))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "TASK_CHANNEL",
                    "Task Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify((int) taskId + 1000, builder.build());
    }

    private void showNotification(long taskId, String title, String description, String dueDate, String status) {
        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Notification permission not granted");
                return;
            }
        }

        Intent intent = new Intent(this, TaskDetailsActivity.class);
        intent.putExtra("taskId", taskId);
        intent.putExtra("title", title);
        intent.putExtra("description", description);
        intent.putExtra("dueDate", dueDate);
        intent.putExtra("status", status);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (int) taskId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "TASK_CHANNEL")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("New Task Assigned")
                .setContentText(title)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(description))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "TASK_CHANNEL",
                    "Task Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify((int) taskId, builder.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (stompClient != null) {
            stompClient.disconnect();
        }
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}