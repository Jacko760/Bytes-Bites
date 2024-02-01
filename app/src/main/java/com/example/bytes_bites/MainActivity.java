package com.example.bytes_bites;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;


import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE =100 ;
    private ProgressDialog progressDialog;
    private static final int PICK_IMAGE =101 ;
    private static final int PERMISSION_REQUEST_CAMERA =102 ;
     ImageView ivSetImageview,copyText;
     Spinner spinnerRecipeCount;
     Spinner spinnerTargetLanguage;
     Button button , genarateBUtton;
     private final String GOOGLE_TTS_PACKAGE = "com.google.android.tts";
    private String currentLanguageTag;
    AlertDialog errorDialog;


    AlertDialog dialog;
    Integer apiLanguageCode;
    String apiRecipeCount;
    String selectedItem;
    String recipie;
    HashMap<String, String> lanDict = new HashMap<>();
    HashMap<String, Integer> recipDict = new HashMap<>();
    public Bitmap imageBitmap = null; // To hold the captured image
    private TextToSpeech textToSpeech;
    private TTSService ttsService;
    private boolean isBound = false;

    String recipeText;

    // Replace with your server's IP address and port
    private static final String SERVER_URL = "http://154.41.254.179:5002/upload_and_generate_recipe";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ivSetImageview=findViewById(R.id.ivSetImage);
        button=findViewById(R.id.btnUpload);
        genarateBUtton=findViewById(R.id.btnGenerate);
        spinnerRecipeCount = findViewById(R.id.spinnerRecipeType);
        spinnerTargetLanguage = findViewById(R.id.spinnerLanguage);
        initializeTTS();
        button.setOnClickListener(v -> selectImage());
        lanDict.put("Telugu", "te");
        lanDict.put("Hindi", "hi");
        lanDict.put("Kannada", "kn");
        lanDict.put("Tamil", "ta");
        lanDict.put("English", "en");
        spinnerRecipeCount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                recipie = parent.getItemAtPosition(position).toString();
                String languageCode = lanDict.get(recipie);

                if (languageCode != null) {
                    currentLanguageTag = languageCode;
                    setLanguageAndVoice(currentLanguageTag, "male");
                } else {
                    Log.e("TTS", "Language code not found for the selected language: " + selectedItem);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinnerTargetLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                 selectedItem = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
            genarateBUtton.setOnClickListener(v -> {
            if (imageBitmap != null) {
                uploadImageAndGetRecipe(imageBitmap);
            } else {
                Toast.makeText(MainActivity.this, "No image to upload", Toast.LENGTH_SHORT).show();
            }

        });

    }

    private void initializeTTS() {

        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    // Default gender set to male
                } else {
                    Log.e("TTS", "Initialization failed");
                }
            }
        }, GOOGLE_TTS_PACKAGE);
        textToSpeech.setSpeechRate(0.95f);
    }

    private void setLanguageAndVoice(String languageTag, String gender) {
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
        switch (languageTag) {
            case "hi":
                return gender.equals("male") ? "hi-in-x-hie-local" : "hi-in-x-hic-local";
            case "ta":
                return gender.equals("male") ? "ta-in-x-tad-local": "ta-IN-language";
            case "te":
                return gender.equals("male") ? "te-in-x-tem-local": "te-IN-language";
            case "kn":
                return gender.equals("male") ? "kn-in-x-knm-network": "kn-IN-language";
            default:
                return gender.equals("male") ? "en-us-x-iol-local" : "en-us-x-sfg-local"; // Default to US English
        }
    }

    private void selectImage() {
        final CharSequence[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Add Photo!");

        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Take Photo")) {
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_IMAGE_CAPTURE);
                } else {
                    openCamera();
                }
            } else if (options[item].equals("Choose from Gallery")) {
                Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickPhoto , PICK_IMAGE);
            } else if (options[item].equals("Cancel")) {
                dialog.dismiss();
            }
        });
        builder.show();
    }
    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is needed to use this feature", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_CANCELED) {
            switch (requestCode) {
                case PICK_IMAGE:
                    if (resultCode == RESULT_OK && data != null) {
                        Uri selectedImage = data.getData();
                        ivSetImageview.setImageURI(selectedImage);
                        try {
                            // Convert the image URI to Bitmap
                            imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                        } catch (IOException e) {
                            e.printStackTrace();
                            // Handle the exception
                        }
                    }
                    break;
                case REQUEST_IMAGE_CAPTURE:
                    if (resultCode == RESULT_OK && data != null) {
                        Bundle extras = data.getExtras();
                        imageBitmap = (Bitmap) extras.get("data");
                        ivSetImageview.setImageBitmap(imageBitmap);
                    }
                    break;
            }
        }
    }
    private void uploadImageAndGetRecipe(Bitmap imageBitmap) {
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Uploading image, please wait...");
        progressDialog.setCancelable(false); // Set to false if you want to disable dismissing the dialog by back button
        progressDialog.show();

        // Convert Bitmap to byte array
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        lanDict.put("Telugu", "te");
        lanDict.put("Hindi", "hi");
        lanDict.put("Kannada", "kn");
        lanDict.put("Tamil", "ta");
        lanDict.put("English", "en");

        recipDict.put("one", 1);
        recipDict.put("two", 2);
        recipDict.put("three", 3);


        String selectedLanguage = selectedItem;
        apiLanguageCode = recipDict.get(selectedLanguage); // Translate to API language code

        String selectedRecipeCount =recipie;
        apiRecipeCount = lanDict.get(selectedRecipeCount);



        RequestBody postBodyImage = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "androidFlask.jpg", RequestBody.create(MediaType.parse("image/*jpg"), byteArray))
                .addFormDataPart("recipie", String.valueOf(apiLanguageCode))
                .addFormDataPart("trgt_lang", apiRecipeCount)
                .build();

        postRequest(SERVER_URL, postBodyImage);
    }

    void postRequest(String url, RequestBody postBody) {
        int timeout = 30;

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .build();


        Request request = new Request.Builder()
                .url(url)
                .post(postBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                runOnUiThread(() -> {
                    serverErrorDialog();
                    progressDialog.dismiss();

                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull final Response response) throws IOException {
                String responseString = response.isSuccessful() ? response.body().string() : null;
                runOnUiThread(() -> {
                        progressDialog.dismiss();
                        if (response.isSuccessful()&&responseString != null) {
                            JSONObject jsonResponse = null;
                            try {
                                jsonResponse = new JSONObject(responseString);
                                 recipeText = jsonResponse.getString("recipe");
                                JSONArray labelsArray = jsonResponse.getJSONArray("labels_count");

                                StringBuilder labelsTextBuilder = new StringBuilder();
                                for (int i = 0; i < labelsArray.length(); i++) {
                                    JSONObject labelObject = labelsArray.getJSONObject(i);
                                    String name = labelObject.getString("name");
                                    int count = labelObject.getInt("count");
                                    labelsTextBuilder.append(name).append(": ").append(count).append("\n");
                                }

                                LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
                                View view = inflater.inflate(R.layout.alertdailog, null);
                                TextView textView = view.findViewById(R.id.dialogTextView);
                                TextView labelView=view.findViewById(R.id.dialoglabelcontView);
                                textView.setText(recipeText);
                                labelView.setText(labelsTextBuilder.toString());
                                ImageView copyIcon = view.findViewById(R.id.copyIcon);
                                ImageView textSpeech=view.findViewById(R.id.audioIcon);
                                textSpeech.setOnClickListener(v -> showGenderSelectionDialog());
                                copyIcon.setOnClickListener(v -> copyToClipboard(recipeText, copyIcon));
                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                builder.setView(view);
                                builder.setTitle("Recipe Details");
                                builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
                                dialog = builder.create();
                                dialog.show();
                                dialog.setOnDismissListener(dialogInterface -> {
                                    if (textToSpeech != null) {
                                        textToSpeech.stop();
                                    }
                                });
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        } else if (response.code() == 400) {
                            showErrorDialog();
                            progressDialog.dismiss();
                        } else {
                            showErrorDialog();
                            progressDialog.dismiss();
                        }


                });
            }


        });
    }

    private void showGenderSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_voice_gender, null);
        RadioGroup radioGroup = dialogView.findViewById(R.id.radioGroupGender);

        builder.setView(dialogView)
                .setTitle("Select Voice Gender")
                .setPositiveButton("OK", (dialog, which) -> {
                    int selectedId = radioGroup.getCheckedRadioButtonId();
                    RadioButton selectedRadioButton = dialogView.findViewById(selectedId);
                    String gender = selectedRadioButton.getText().toString().toLowerCase();
                    setLanguageAndVoice(currentLanguageTag, gender);
                    speakText(recipeText); // Replace with actual text to speak
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }
    private void speakText(String textToSpeak) {
        if (textToSpeech != null) {
            textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }



    private void copyToClipboard(String text, ImageView copyIcon) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Recipe Text", text);
        clipboard.setPrimaryClip(clip);
        copyIcon.setImageResource(R.drawable.twotone_content_copy_24);
        Toast.makeText(MainActivity.this, "Text copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void showErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setTitle("Error Processing Image");
        builder.setMessage("⚠️ Please check your image. \n\n📷✨ Encountering the 'Please check your image' error?\n\n" +
                "Our algorithm may not have been able to predict the content of your image. To improve results, consider the following:\n" +
                "👉 Verify image quality and resolution.\n" +
                "👉 Ensure the image is clear and well-lit.\n" +
                "👉 Check if the image meets our specified format requirements.\n" +
                "👉 Consider alternative images for better results.\n\n" +
                "Our aim is to provide accurate predictions, and addressing these aspects can make a significant difference. If the issue persists, please reach out to our support team. We're here to help! 🤝🔧");

        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());

        errorDialog = builder.create();
        errorDialog.show();

    }

    private void serverErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Error Processing Image");
        builder.setMessage("Our servers are busy Please try again in sometimes or Make sure you are connected to Internet");
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        AlertDialog dialogg = builder.create();
        dialogg.show();
    }


    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

}