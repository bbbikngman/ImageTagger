package com.sinuo.imagetagger;

import android.content.Context;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.sinuo.imagetagger.utils.SettingsManager;
import java.util.Arrays;

public class SettingsActivity extends AppCompatActivity {
    private EditText apiUrlInput;
    private EditText apiKeyInput;
    private EditText languageAInput;
    private EditText languageBInput;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        initViews();
        loadCurrentSettings();
        setupSaveButton();
    }

    private void initViews() {
        apiUrlInput = findViewById(R.id.input_gpt_api_url);
        apiKeyInput = findViewById(R.id.input_gpt_api_key);
        languageAInput = findViewById(R.id.input_language_a);
        languageBInput = findViewById(R.id.input_language_b);
        saveButton = findViewById(R.id.button_save);
    }

    private void loadCurrentSettings() {
        // Using application context for settings
        Context context = getApplicationContext();

        apiUrlInput.setText(SettingsManager.getSetting(context, "gptApiUrl", "https://api.key77qiqi.cn/v1/chat/completions"));
        apiKeyInput.setText(SettingsManager.getSetting(context, "gptApiKey", "sk-FBYU2VJ3QtuA5wnfTFxNou9SONF9KBzRQLThrgtVvvAVZBly"));
        // Set default values for language inputs
        languageAInput.setText(SettingsManager.getSetting(context, "languageA", "English"));
        languageBInput.setText(SettingsManager.getSetting(context, "languageB", "Japanese"));

    }

    private void setupSaveButton() {
        saveButton.setOnClickListener(v -> {
            Context context = getApplicationContext();

            SettingsManager.saveSetting(context, "gptApiUrl", apiUrlInput.getText().toString());
            SettingsManager.saveSetting(context, "gptApiKey", apiKeyInput.getText().toString());
            SettingsManager.saveSetting(context, "languageA", languageAInput.getText().toString());
            SettingsManager.saveSetting(context, "languageB", languageBInput.getText().toString());

            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}