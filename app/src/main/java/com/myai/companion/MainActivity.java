package com.myai.companion;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView chatDisplay;
    private EditText messageInput;
    private ScrollView chatScroll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chatDisplay = findViewById(R.id.chatDisplay);
        messageInput = findViewById(R.id.messageInput);
        chatScroll = findViewById(R.id.chatScroll);
        Button sendButton = findViewById(R.id.sendButton);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userText = messageInput.getText().toString().trim();
                if (!userText.isEmpty()) {
                    chatDisplay.append("You: " + userText + "\n\n");
                    messageInput.setText("");

                    // Assistant immediate response
                    String botReply = getAIResponse(userText);
                    chatDisplay.append("AI: " + botReply + "\n\n");

                    chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
                }
            }
        });
    }

    private String getAIResponse(String input) {
        String lower = input.toLowerCase();
        if (lower.contains("hello") || lower.contains("hi")) {
            return "Namaste! Main active hoon. Kahiye kya madad karoon?";
        } else if (lower.contains("kaise ho") || lower.contains("how are you")) {
            return "Main bilkul badiya hoon! Aapka system smoothly chal raha hai.";
        } else if (lower.contains("name") || lower.contains("naam")) {
            return "Main hoon aapka personal AI Companion OS assistant.";
        } else {
            return "Aapne kaha: \"" + input + "\". System fully connected hai.";
        }
    }
}
