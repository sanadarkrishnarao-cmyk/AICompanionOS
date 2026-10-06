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

                if (isOnline()) {
                    if (apiKey.isEmpty()) {
                        appendChat("AI (Notice)", "Internet ON hai, lekin API Key nahi mili. Upar 'API KEY' button dabakar apni key paste karein.");
                    } else {
                        callGeminiFlash(userText, apiKey);
                    }
                } else {
                    String localReply = getSmartOfflineAIResponse(userText);
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
                .setMessage("Paste your key from Google AI Studio")
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
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
                if (caps != null) {
                    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void appendChat(String sender, String message) {
        mainHandler.post(() -> {
            chatDisplay.append(sender + ": " + message + "\n\n");
            chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
        });
    }

    private void callGeminiFlash(String prompt, String apiKey) {
        appendChat("AI", "Thinking...");

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";

            JSONObject textPart = new JSONObject();
            textPart.put("text", prompt);

            JSONArray partsArray = new JSONArray();
            partsArray.put(textPart);

            JSONObject contentObject = new JSONObject();
            contentObject.put("parts", partsArray);

            JSONArray contentsArray = new JSONArray();
            contentsArray.put(contentObject);

            JSONObject jsonPayload = new JSONObject();
            jsonPayload.put("contents", contentsArray);

            RequestBody body = RequestBody.create(
                    jsonPayload.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("x-goog-api-key", apiKey)
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(() -> {
                        appendChat("AI (Offline Fallback)", getSmartOfflineAIResponse(prompt));
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.body() == null) {
                        mainHandler.post(() -> appendChat("AI", "Khali response mila."));
                        return;
                    }

                    String respStr = response.body().string();

                    if (!response.isSuccessful()) {
                        mainHandler.post(() -> appendChat("AI (API Error " + response.code() + ")", respStr));
                        return;
                    }

                    try {
                        JSONObject json = new JSONObject(respStr);
                        String aiText = json.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");

                        mainHandler.post(() -> appendChat("AI (Online)", aiText.trim()));
                    } catch (Exception ex) {
                        mainHandler.post(() -> appendChat("AI (Response)", respStr));
                    }
                }
            });
        } catch (Exception e) {
            appendChat("AI (Error)", e.getMessage());
        }
    }

    private String getSmartOfflineAIResponse(String input) {
        String text = input.toLowerCase().trim();

        if (text.contains("namaste") || text.contains("hello") || text.contains("hi") || text.contains("hey")) {
            return "Namaste! Main AI Companion hoon. Kahiye, main aapki kya madad karoon?";
        }
        if (text.contains("naam") || text.contains("name") || text.contains("who are you") || text.contains("kaun ho")) {
            return "Mera naam 'AI Companion OS' hai. Main aapka personal offline-online hybrid companion hoon.";
        }
        if (text.contains("kaise ho") || text.contains("how are you") || text.contains("kya haal")) {
            return "Main bilkul badiya aur active hoon! Aap batayein, aaj ka din kaisa chal raha hai?";
        }
        if (text.contains("time") || text.contains("samay") || text.contains("waqt") || text.contains("date") || text.contains("tarikh")) {
            String currentTime = new SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.getDefault()).format(new Date());
            return "Abhi ka samay aur tarikh: " + currentTime;
        }
        if (text.contains("story") || text.contains("kahani")) {
            return "[Offline Kahani]: Ek gaon me ek robot rehta tha jo bina internet ke bhi sabki madad karta tha. Ek din jab network chala gaya, tab bhi usne apne offline memory se gaon ke saare kaam poore karwa diye!";
        }
        if (text.contains("joke") || text.contains("chutkula")) {
            return "Ek programmer ne doosre se poocha: 'Duniya me 10 tarah ke log hote hain?' Dusra bola: 'Kaise?' Pehle ne kaha: 'Ek jo binary samajhte hain, aur doosre jo nahi!'";
        }

        return "Maine aapki baat note kar li hai: \"" + input + "\". Offline core active hai.";
    }
}
