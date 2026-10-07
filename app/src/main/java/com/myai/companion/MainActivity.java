package com.myai.companion;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private VideoView animeVideoView;
    private TextView chatDisplay;
    private EditText messageInput;
    private ScrollView chatScroll;
    private TextToSpeech tts;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<String> videoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    saveAndPlayVideo(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        animeVideoView = findViewById(R.id.animeVideoView);
        chatDisplay = findViewById(R.id.chatDisplay);
        messageInput = findViewById(R.id.messageInput);
        chatScroll = findViewById(R.id.chatScroll);
        Button sendButton = findViewById(R.id.sendButton);
        Button btnAppDrawer = findViewById(R.id.btnAppDrawer);
        Button btnLoadVideo = findViewById(R.id.btnLoadVideo);

        try {
            tts = new TextToSpeech(this, status -> {
                if (status == TextToSpeech.SUCCESS && tts != null) {
                    tts.setLanguage(new Locale("hi", "IN"));
                }
            });
        } catch (Exception ignored) {}

        playSavedVideo();

        btnLoadVideo.setOnClickListener(v -> videoPickerLauncher.launch("video/*"));

        btnAppDrawer.setOnClickListener(v -> openAppDrawer());

        sendButton.setOnClickListener(v -> {
            String text = messageInput.getText().toString().trim();
            if (!text.isEmpty()) {
                appendChat("You", text);
                messageInput.setText("");

                String reply = getReply(text);
                appendChat("Companion", reply);
                speakText(reply);
            }
        });
    }

    private void saveAndPlayVideo(Uri uri) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            File dest = new File(getFilesDir(), "saved_anime.mp4");
            FileOutputStream out = new FileOutputStream(dest);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();

            Toast.makeText(this, "Anime Video Loaded!", Toast.LENGTH_SHORT).show();
            playSavedVideo();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void playSavedVideo() {
        File file = new File(getFilesDir(), "saved_anime.mp4");
        if (file.exists() && file.length() > 0) {
            animeVideoView.setVideoURI(Uri.fromFile(file));
            animeVideoView.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                mp.setVolume(0, 0);
                mp.start();
            });
        }
    }

    private void openAppDrawer() {
        PackageManager pm = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> list = pm.queryIntentActivities(mainIntent, 0);
        final List<String> appNames = new ArrayList<>();
        final List<Intent> launchIntents = new ArrayList<>();

        for (ResolveInfo info : list) {
            String pkg = info.activityInfo.packageName;
            if (!pkg.equals(getPackageName())) {
                String name = info.loadLabel(pm).toString();
                Intent launch = pm.getLaunchIntentForPackage(pkg);
                if (launch != null) {
                    appNames.add(name);
                    launchIntents.add(launch);
                }
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Installed Apps");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, appNames);
        builder.setAdapter(adapter, (dialog, which) -> {
            try {
                startActivity(launchIntents.get(which));
            } catch (Exception ignored) {}
        });
        builder.show();
    }

    private String getReply(String input) {
        String q = input.toLowerCase().trim();
        if (q.contains("namaste") || q.contains("hello") || q.contains("hi")) {
            return "Namaste! Main aapka AI Companion hoon. Kahiye kya madad karoon?";
        }
        if (q.contains("time") || q.contains("samay")) {
            return "Samay hai: " + new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        }
        if (q.contains("kaun ho") || q.contains("who are you")) {
            return "Main aapka live anime AI companion OS hoon!";
        }
        if (q.contains("apps") || q.contains("drawer")) {
            mainHandler.post(this::openAppDrawer);
            return "Apps open kar di hain.";
        }
        return "Maine suna: \"" + input + "\"";
    }

    private void speakText(String text) {
        try {
            if (tts != null) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        } catch (Exception ignored) {}
    }

    private void appendChat(String sender, String message) {
        mainHandler.post(() -> {
            chatDisplay.append(sender + ": " + message + "\n\n");
            chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (animeVideoView != null && !animeVideoView.isPlaying()) {
            animeVideoView.start();
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
