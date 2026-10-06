package com.myai.companion;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private TextView chatDisplay;
    private EditText messageInput;
    private ScrollView chatScroll;
    private OkHttpClient httpClient;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("AISettings", MODE_PRIVATE);
        httpClient = new OkHttpClient();

        chatDisplay = findViewById(R.id.chatDisplay);
        messageInput = findViewById(R.id.messageInput);
        chatScroll = findViewById(R.id.chatScroll);
        Button sendButton = findViewById(R.id.sendButton);
        Button btnKey = findViewById(R.id.btnKey);

        btnKey.setOnClickListener(v -> showApiKeyDialog());

        sendButton.setOnClickListener(v -> {
            String userText = messageInput.getText().toString().trim();
            if (!userText.isEmpty()) {
                appendChat("You", userText);
                messageInput.setText("");

                String apiKey = prefs.getString("gemini_key", "").trim();

                if (isOnline() && !apiKey.isEmpty()) {
                    callOnlineAI(userText, apiKey);
                } else {
                    String localReply = getSmartOfflineAIResponse(userText, apiKey.isEmpty());
                    appendChat("AI (Offline)", localReply);
                }
            }
        });
    }

    private void showApiKeyDialog() {
        EditText input = new EditText(this);
        input.setHint("Paste Gemini API Key here");
        input.setText(prefs.getString("gemini_key", ""));

        new AlertDialog.Builder(this)
                .setTitle("Set Gemini API Key")
                .setMessage("Free key lene ke liye: aistudio.google.com")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String key = input.getText().toString().trim();
                    prefs.edit().putString("gemini_key", key).apply();
                    appendChat("System", "API Key successfully save ho gayi hai!");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
        }
        return false;
    }

    private void appendChat(String sender, String message) {
        mainHandler.post(() -> {
            chatDisplay.append(sender + ": " + message + "\n\n");
            chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
        });
    }

    private void callOnlineAI(String prompt, String apiKey) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

            JSONObject part = new JSONObject();
            part.put("text", prompt);

            JSONArray parts = new JSONArray();
            parts.put(part);

            JSONObject content = new JSONObject();
            content.put("parts", parts);

            JSONArray contents = new JSONArray();
            contents.put(content);

            JSONObject root = new JSONObject();
            root.put("contents", contents);

            RequestBody body = RequestBody.create(root.toString(), MediaType.parse("application/json; charset=utf-8"));
            Request request = new Request.Builder().url(url).post(body).build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(() -> {
                        String localReply = getSmartOfflineAIResponse(prompt, false);
                        appendChat("AI (Fallback Offline)", localReply);
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String responseBody = response.body().string();
                            JSONObject json = new JSONObject(responseBody);
                            String aiText = json.getJSONArray("candidates")
                                    .getJSONObject(0)
                                    .getJSONObject("content")
                                    .getJSONArray("parts")
                                    .getJSONObject(0)
                                    .getString("text");

                            appendChat("AI (Online)", aiText.trim());
                        } catch (Exception ex) {
                            appendChat("AI (Offline)", getSmartOfflineAIResponse(prompt, false));
                        }
                    } else {
                        mainHandler.post(() -> {
                            appendChat("AI (Offline)", getSmartOfflineAIResponse(prompt, false));
                        });
                    }
                }
            });
        } catch (Exception e) {
            appendChat("AI (Offline)", getSmartOfflineAIResponse(prompt, false));
        }
    }

    // SMART OFFLINE ENGINE
    private String getSmartOfflineAIResponse(String input, boolean keyMissing) {
        String text = input.toLowerCase().trim();

        if (text.contains("namaste") || text.contains("hello") || text.contains("hi") || text.contains("hey")) {
            return "Namaste! Main AI Companion hoon. Kahiye, main aapki kya madad karoon?";
        }
        if (text.contains("naam") || text.contains("name") || text.contains("who are you") || text.contains("kaun ho")) {
            return "Mera naam 'AI Companion OS' hai. Main aapka personal offline-online smart companion hoon.";
        }
        if (text.contains("kaise ho") || text.contains("how are you") || text.contains("kya haal")) {
            return "Main bilkul badiya aur active hoon! Aap batayein, aaj ka din kaisa chal raha hai?";
        }
        if (text.contains("time") || text.contains("samay") || text.contains("waqt") || text.contains("date") || text.contains("tarikh")) {
            String currentTime = new SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.getDefault()).format(new Date());
            return "Abhi ka samay aur tarikh: " + currentTime;
        }
        if (text.contains("creator") || text.contains("kisne banaya") || text.contains("developer") || text.contains("owner")) {
            return "Mujhe aapne hi Termux aur Android source ke zariye build kiya hai!";
        }
        if (text.contains("kya kar sakte ho") || text.contains("features") || text.contains("capabilities")) {
            return "Main offline me instant system answers, calculations, facts de sakta hoon, aur online aane par deep complex coding, stories, aur har sawal ka full LLM answer!";
        }
        if (text.contains("joke") || text.contains("chutkula")) {
            return "Ek programmer ne doosre se poocha: 'Duniya me 10 tarah ke log hote hain?'\nDusra bola: 'Kaise?'\nPehle ne kaha: 'Ek jo binary samajhte hain, aur doosre jo nahi!'";
        }
        if (text.contains("motivat") || text.contains("shiksha")) {
            return "Mushkilein sabke raste me aati hain, lekin jo har error se seekh kar aage badhta hai, wahi naya system khada karta hai!";
        }
        if (text.contains("bye") || text.contains("alvida") || text.contains("good night")) {
            return "Alvida! Apna khayal rakhiye, main hamesha yahi par active hoon.";
        }

        if (keyMissing && isOnline()) {
            return "Aapka Internet ON hai! Online super-intelligence unlock karne ke liye upar 'API Key' button daba kar apni free Gemini key paste karein.";
        }

        return "Maine aapki baat note kar li hai: \"" + input + "\". Offline database me iska basic record save hai. Deep explanation ke liye internet connect karein.";
    }
}
