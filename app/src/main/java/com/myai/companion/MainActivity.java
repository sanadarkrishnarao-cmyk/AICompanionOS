package com.myai.companion;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView chatDisplay;
    private EditText messageInput;
    private ScrollView chatScroll;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chatDisplay = findViewById(R.id.chatDisplay);
        messageInput = findViewById(R.id.messageInput);
        chatScroll = findViewById(R.id.chatScroll);
        Button sendButton = findViewById(R.id.sendButton);
        Button btnWallpaper = findViewById(R.id.btnWallpaper);

        btnWallpaper.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
                intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        new ComponentName(MainActivity.this, AnimeWallpaperService.class));
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
                startActivity(intent);
            }
        });

        sendButton.setOnClickListener(v -> {
            String userText = messageInput.getText().toString().trim();
            if (!userText.isEmpty()) {
                appendChat("You", userText);
                messageInput.setText("");

                String reply = getOfflineResponse(userText);
                appendChat("AI Companion", reply);
            }
        });
    }

    private void appendChat(String sender, String message) {
        mainHandler.post(() -> {
            chatDisplay.append(sender + ": " + message + "\n\n");
            chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
        });
    }

    private String getOfflineResponse(String input) {
        String text = input.toLowerCase().trim();
        if (text.contains("wallpaper") || text.contains("anime")) {
            return "Upar diye gaye 'Set Anime Wallpaper' button par click karke aap mujhe apne phone ki live home-screen par set kar sakte hain!";
        }
        if (text.contains("namaste") || text.contains("hello") || text.contains("hi")) {
            return "Namaste! Main aapka live Anime Companion hoon!";
        }
        if (text.contains("time") || text.contains("samay")) {
            return "Abhi ka samay: " + new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        }
        return "Maine sun liya: \"" + input + "\". Wallpaper set karne ke liye upar pink button dabayein!";
    }
}
