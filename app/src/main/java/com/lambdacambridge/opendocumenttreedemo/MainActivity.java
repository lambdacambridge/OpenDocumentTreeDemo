package com.lambdacambridge.opendocumenttreedemo;

import android.app.Activity;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.ComponentActivity;
import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Objects;

public class MainActivity extends ComponentActivity {
    private static final int REQUEST_CODE_OPEN_DIRECTORY = 1;

    private Uri baseUri;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Button buttonRead = findViewById(R.id.buttonRead);
        Button buttonWrite = findViewById(R.id.buttonWrite);
        EditText fileNameText = findViewById(R.id.editTextFileName);
        EditText contentText = findViewById(R.id.editTextContent);

        contentText.setText("Time " + System.currentTimeMillis());

        // WRITING
        buttonWrite.setOnClickListener(v -> {
            String fileName = fileNameText.getText().toString();
            String content = contentText.getText().toString();

            DocumentFile directory = DocumentFile.fromTreeUri(this, baseUri);
            // If you want a sub-directory, use directory.createDirectory("Perm_Data")
            DocumentFile file = directory != null ? directory.findFile(fileName) : null;
            if (directory != null && file == null) {
                file = directory.createFile("text/plain", fileName);
            }
            if (file != null) {
                try {
                    OutputStream outputStream = getContentResolver().openOutputStream(file.getUri());
                    OutputStreamWriter writer = new OutputStreamWriter(outputStream);
                    writer.write(content);
                    writer.close();
                    contentText.setText("Written at " + System.currentTimeMillis());
                } catch (IOException e) {
                    System.out.println("Exception: " + e);
                }
            }
        });

        // READING
        buttonRead.setOnClickListener(v -> {
            String fileName = fileNameText.getText().toString();
            DocumentFile directory = DocumentFile.fromTreeUri(this, baseUri);
            DocumentFile file = directory != null ? directory.findFile(fileName) : null;
            if (file != null) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    try (InputStream inputStream =
                                 getContentResolver().openInputStream(file.getUri());
                         BufferedReader reader = new BufferedReader(
                                 new InputStreamReader(Objects.requireNonNull(inputStream)))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            stringBuilder.append(line);
                        }
                    }
                    contentText.setText(stringBuilder.toString());
                } catch (IOException e) {
                    System.out.println("Exception: " + e);
                }
            }
        });

        // PERMISSIONS
        List<UriPermission> existing = getContentResolver().getPersistedUriPermissions();
        if (existing.isEmpty()) {
            // Choose a directory using the system's file picker.
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

            startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY);
        } else {
            baseUri = existing.get(0).getUri();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == REQUEST_CODE_OPEN_DIRECTORY && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the directory the user selected.
            if (resultData != null) {
                Uri uri = resultData.getData();
                // Get a persistent Uri to this folder.
                final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(uri, takeFlags);
                baseUri = uri;
            }
        }
    }
}