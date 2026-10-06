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

                String apiKey = prefs.getString("ai_key", "").trim();

                if (isOnline()) {
                    if (apiKey.isEmpty()) {
                        appendChat("AI (Notice)", "Online brain use karne ke liye upar 'API KEY' button par apni free Groq API key set karein (console.groq.com).");
                    } else {
                        callGroqAI(userText, apiKey);
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
        input.setHint("Paste Groq API Key (gsk_...)");
        input.setText(prefs.getString("ai_key", ""));

        new AlertDialog.Builder(this)
                .setTitle("Set High-Speed AI Key")
                .setMessage("Free Key: console.groq.com/keys")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String key = input.getText().toString().trim();
                    prefs.edit().putString("ai_key", key).apply();
                    appendChat("System", "Groq AI Key successfully save ho gayi!");
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

    // High Speed Groq Cloud Engine (Llama 3.3 70B)
    private void callGroqAI(String prompt, String apiKey) {
        appendChat("AI", "Thinking (Lightning fast)...");

        try {
            String url = "https://api.groq.com/openai/v1/chat/completions";

            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", "You are an intelligent, helpful, witty companion OS assistant. Reply in clear Hindi or Hinglish or English based on user query.");

            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);

            JSONArray messages = new JSONArray();
            messages.put(systemMsg);
            messages.put(userMsg);

            JSONObject payload = new JSONObject();
            payload.put("model", "llama-3.1-8b-instant");
            payload.put("messages", messages);

            RequestBody body = RequestBody.create(
                    payload.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
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
                        mainHandler.post(() -> appendChat("AI", "Empty response from server."));
                        return;
                    }

                    String respStr = response.body().string();

                    if (!response.isSuccessful()) {
                        mainHandler.post(() -> appendChat("AI (Error " + response.code() + ")", respStr));
                        return;
                    }

                    try {
                        JSONObject json = new JSONObject(respStr);
                        String answer = json.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");

                        mainHandler.post(() -> appendChat("AI (Online)", answer.trim()));
                    } catch (Exception ex) {
                        mainHandler.post(() -> appendChat("AI", "Response parse error: " + ex.getMessage()));
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
            return "Mera naam 'AI Companion OS' hai. Main offline aur high-speed online dono modes me chalta hoon.";
        }
        if (text.contains("kaise ho") || text.contains("how are you") || text.contains("kya haal")) {
            return "Main bilkul badiya aur active hoon! Aap batayein, aaj ka din kaisa chal raha hai?";
        }
        if (text.contains("time") || text.contains("samay") || text.contains("waqt") || text.contains("date") || text.contains("tarikh")) {
            String currentTime = new SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.getDefault()).format(new Date());
            return "Abhi ka samay: " + currentTime;
        }
        if (text.contains("story") || text.contains("kahani")) {
            return "[Offline Kahani]: Ek chhota sa robot tha jo bina network ke bhi zameen par har mushkil kaam akele hal kar leta tha. Uska usool tha: chahe signal ho ya na ho, kaam nahi rukna chahiye!";
        }
        if (text.contains("joke") || text.contains("chutkula")) {
            return "Ek dost ne poocha: 'Tera phone itna smart kaise ho gaya?'\nPehle ne kaha: 'Kyunki isme AI Companion OS install hai!'";
        }

        return "Maine aapki baat offline memory me note kar li hai: \"" + input + "\".";
    }
}
