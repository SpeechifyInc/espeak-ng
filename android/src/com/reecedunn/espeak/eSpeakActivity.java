/*
 * Grapheme-to-phoneme demo for eSpeak NG.
 * Converts English (en-US) input text into phoneme strings (IPA or ASCII).
 */

package com.reecedunn.espeak;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

public class eSpeakActivity extends Activity {
    private static final int REQUEST_DOWNLOAD = 100;
    private static final String PRIMARY_VOICE_ID = "en-us";
    private static final String FALLBACK_VOICE_ID = "en";

    private SpeechSynthesis mSynthesis;
    private EditText mInput;
    private CheckBox mUseIpa;
    private TextView mOutput;
    private TextView mOutputPairs;
    private Button mConvert;
    private PhonemeMapper mMapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mInput = (EditText)findViewById(R.id.editText1);
        mUseIpa = (CheckBox)findViewById(R.id.checkbox_ipa);
        mOutput = (TextView)findViewById(R.id.outputPhonemes);
        mOutputPairs = (TextView)findViewById(R.id.outputPairs);
        mConvert = (Button)findViewById(R.id.convert);

        Context storageContext = EspeakApp.getStorageContext();
        mSynthesis = new SpeechSynthesis(storageContext, null);
        try {
            mMapper = PhonemeMapper.fromAssets(this, "vitsMapper.json");
        } catch (Exception e) {
            mMapper = null;
        }
        if (!ensureVoiceData(storageContext)) {
            mConvert.setEnabled(false);
        } else {
            selectEnglishVoice();
        }

        mConvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                convertInput();
            }
        });
    }

    /**
     * Choose an English (en-us) voice if available, otherwise fall back to the
     * first English voice in the list.
     */
    private void selectEnglishVoice() {
        if (!mSynthesis.isInitialized()) {
            Toast.makeText(this, R.string.conversion_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        if (mSynthesis.setVoiceByName(PRIMARY_VOICE_ID)) {
            return;
        }
        if (mSynthesis.setVoiceByName(FALLBACK_VOICE_ID)) {
            return;
        }
        List<Voice> voices = mSynthesis.getAvailableVoices();
        Voice fallback = null;
        for (Voice voice : voices) {
            if (voice.identifier.equalsIgnoreCase("en-us")) {
                mSynthesis.setVoiceByName(voice.identifier);
                return;
            }
            if (voice.locale.getLanguage().equals(new Locale("en").getLanguage()) && fallback == null) {
                fallback = voice;
            }
        }
        if (fallback != null) {
            mSynthesis.setVoiceByName(fallback.identifier);
        }
    }

    private void convertInput() {
        final String text = mInput.getText().toString();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, R.string.enter_text_prompt, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!mSynthesis.isInitialized()) {
            Toast.makeText(this, R.string.conversion_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        // Ensure English voice is selected before each conversion.
        selectEnglishVoice();

        final boolean useIpa = mUseIpa.isChecked();
        final String phonemes = mSynthesis.textToPhonemes(text, useIpa);
        if (phonemes == null) {
            Toast.makeText(this, R.string.conversion_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        mOutput.setText(phonemes);

        if (mMapper != null) {
            mOutputPairs.setText(mMapper.formatPairs(phonemes));
        } else {
            mOutputPairs.setText("");
        }
    }

    private boolean ensureVoiceData(Context storageContext) {
        if (!CheckVoiceData.hasBaseResources(storageContext) || CheckVoiceData.canUpgradeResources(storageContext)) {
            startActivityForResult(new Intent(this, DownloadVoiceData.class), REQUEST_DOWNLOAD);
            Toast.makeText(this, R.string.installing_voice_data, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_DOWNLOAD) {
            Context storageContext = EspeakApp.getStorageContext();
            mSynthesis = new SpeechSynthesis(storageContext, null);
            selectEnglishVoice();
            mConvert.setEnabled(mSynthesis.isInitialized());
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
