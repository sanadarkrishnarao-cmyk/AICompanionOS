package com.myai.companion;

import android.content.Context;
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
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
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

    // Yahan apni Gemini API Key daal sakte hain
    private final String GEMINI_API_KEY = "YOUR_GEMINI_API_KEY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        httpClient = new OkHttpClient();
        chatDisplay = findViewById(R.id.chatDisplay);
        messageInput = findViewById(R.id.messageInput);
        chatScroll = findViewById(R.id.chatScroll);
        Button sendButton = findViewById(R.id.sendButton);

        sendButton.setOnClickListener(v -> {
            String userText = messageInput.getText().toString().trim();
            if (!userText.isEmpty()) {
                appendChat("You", userText);
                messageInput.setText("");

                if (isOnline()) {
                    callOnlineAI(userText);
                } else {
                    String localReply = getOfflineAIResponse(userText);
                    appendChat("AI (Offline)", localReply);
                }
            }
        });
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

    // ONLINE AI: Free Gemini API Call
    private void callOnlineAI(String prompt) {
        appendChat("AI", "Typing...");

        if (GEMINI_API_KEY.equals("YOUR_GEMINI_API_KEY")) {
            appendChat("AI (Online)", "Online brain connected! Reply dene ke liye apni free Google Gemini API key set karein.");
            return;
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + GEMINI_API_KEY;

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
                        String localReply = getOfflineAIResponse(prompt);
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
                            appendChat("AI", "Response parse nahi ho saka.");
                        }
                    } else {
                        mainHandler.post(() -> {
                            String localReply = getOfflineAIResponse(prompt);
                            appendChat("AI (Offline)", localReply);
                        });
                    }
                }
            });
        } catch (Exception e) {
            String localReply = getOfflineAIResponse(prompt);
            appendChat("AI (Offline)", localReply);
        }
    }

    // OFFLINE AI: Fast Smart Local Engine
    private String getOfflineAIResponse(String input) {
        String text = input.toLowerCase();

        if (text.contains("namaste") || text.contains("hello") || text.contains("hi")) {
            return "Namaste! Internet band hai, par main offline mode me active hoon.";
        } else if (text.contains("time") || text.contains("samay")) {
            return "Phone ke internal clock se check karein, main offline assistance provide kar raha hoon.";
        } else if (text.contains("kaise ho") || text.contains("how are you")) {
            return "Main offline mode me perfectly run ho raha hoon, battery safe aur fast!";
        } else if (text.contains("who are you") || text.contains("kaun ho")) {
            return "Main aapka hybrid AI Companion OS hoon — offline & online dono jagah active.";
        } else {
            return "[Offline Engine]: Data off hone ki wajah se basic assistance active hai. Online aate hi main deep detailed answer dunga!";
        }
    }
}
