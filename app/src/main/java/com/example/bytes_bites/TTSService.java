package com.example.bytes_bites;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;

import java.util.Locale;

public class TTSService extends Service {
    private final IBinder binder = new LocalBinder();
    private TextToSpeech textToSpeech;
    private String currentLanguageTag = "en-US";

    public class LocalBinder extends Binder {
        TTSService getService() {
            return TTSService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                setLanguageAndVoice(currentLanguageTag, "male");
            } else {
                Log.e("TTS", "Initialization failed");
            }
        });
    }
    // Other methods...

    public void setCurrentLanguageTag(String languageTag) {
        this.currentLanguageTag = languageTag;
        setLanguageAndVoice(currentLanguageTag, "male"); // You might want to handle gender as well dynamically
    }

// Existing methods...


    public void setLanguageAndVoice(String languageTag, String gender) {
        currentLanguageTag = languageTag;
        Locale locale = Locale.forLanguageTag(languageTag);
        int result = textToSpeech.setLanguage(locale);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            textToSpeech.setLanguage(Locale.US);
        }
        setSpecificVoice(languageTag, gender);
    }

    private void setSpecificVoice(String languageTag, String gender) {
        String voiceName = getVoiceNameForLanguageAndGender(languageTag, gender);
        Voice specificVoice = new Voice(voiceName, new Locale(languageTag), 400, 200, false, null);
        textToSpeech.setVoice(specificVoice);
    }

    private String getVoiceNameForLanguageAndGender(String languageTag, String gender) {
        // Example logic, replace voice names as per availability
        switch (languageTag) {
            case "hi":
                return gender.equals("male") ? "hi-in-x-hie-local" : "hi-in-x-hic-local";
            case "ta":
                return gender.equals("male") ? "ta-in-x-tad-local" : "ta-IN-language";
            case "te":
                return gender.equals("male") ? "te-in-x-tem-local" : "te-IN-language";
            case "kn":
                return gender.equals("male") ? "kn-in-x-knm-network" : "kn-IN-language";
            default:
                return gender.equals("male") ? "en-us-x-iol-local" : "en-us-x-sfg-local"; // Default to US English
        }
    }

    public void speak(String text) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
