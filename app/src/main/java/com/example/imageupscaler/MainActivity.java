package com.example.imageupscaler;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_STORAGE_PERMISSION = 101;

    private Button buttonGallery, buttonCamera, enhanceButton, downloadButton;
    private ImageView imageView;
    private TextView processingInfo;
    private ProgressBar progressBar;

    private Uri currentImageUri;
    private Bitmap originalBitmap;
    private Bitmap enhancedBitmap;
    private File photoFile;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImage = result.getData().getData();
                    if (selectedImage != null) {
                        loadImageFromUri(selectedImage);
                    }
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (photoFile != null) {
                        Uri photoUri = FileProvider.getUriForFile(this,
                                getApplicationContext().getPackageName() + ".fileprovider",
                                photoFile);
                        loadImageFromUri(photoUri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        buttonGallery = findViewById(R.id.buttonGallery);
        buttonCamera = findViewById(R.id.buttonCamera);
        enhanceButton = findViewById(R.id.enhanceButton);
        downloadButton = findViewById(R.id.downloadButton);
        imageView = findViewById(R.id.imageView);
        processingInfo = findViewById(R.id.processingInfo);
        progressBar = findViewById(R.id.progressBar);

        // Set up button click listeners
        buttonGallery.setOnClickListener(v -> openGallery());
        buttonCamera.setOnClickListener(v -> openCamera());
        enhanceButton.setOnClickListener(v -> enhanceImage());
        downloadButton.setOnClickListener(v -> saveEnhancedImage());
    }

    private void openGallery() {
        if (checkStoragePermission()) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        } else {
            requestStoragePermission();
        }
    }

    private void openCamera() {
        if (checkCameraPermission()) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                try {
                    photoFile = createImageFile();
                    if (photoFile != null) {
                        Uri photoURI = FileProvider.getUriForFile(this,
                                getApplicationContext().getPackageName() + ".fileprovider",
                                photoFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        cameraLauncher.launch(takePictureIntent);
                    }
                } catch (IOException ex) {
                    Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            requestCameraPermission();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }

    private void loadImageFromUri(Uri imageUri) {
        try {
            currentImageUri = imageUri;
            InputStream imageStream = getContentResolver().openInputStream(imageUri);
            originalBitmap = BitmapFactory.decodeStream(imageStream);
            imageView.setImageBitmap(originalBitmap);
            processingInfo.setText("Image loaded - Ready to enhance");
            enhanceButton.setEnabled(true);
            downloadButton.setEnabled(false);
            enhancedBitmap = null;
        } catch (Exception e) {
            Toast.makeText(this, "Error loading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void enhanceImage() {
        // This will be replaced with actual AI enhancement code in the next steps
        // For now, just simulate enhancement with a delay
        if (originalBitmap != null) {
            progressBar.setVisibility(View.VISIBLE);
            processingInfo.setText("Enhancing image...");
            enhanceButton.setEnabled(false);

            // Simulate enhancement process
            new Thread(() -> {
                try {
                    Thread.sleep(2000); // Simulate processing time

                    // For now, just use the original bitmap
                    enhancedBitmap = originalBitmap;

                    runOnUiThread(() -> {
                        imageView.setImageBitmap(enhancedBitmap);
                        progressBar.setVisibility(View.GONE);
                        processingInfo.setText("Image enhanced successfully!");
                        downloadButton.setEnabled(true);
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        processingInfo.setText("Enhancement failed. Try again.");
                        enhanceButton.setEnabled(true);
                    });
                }
            }).start();
        }
    }

    private void saveEnhancedImage() {
        if (enhancedBitmap != null) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "upscaled_" + System.currentTimeMillis() + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ImageUpscaler");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
            }

            ContentResolver resolver = getContentResolver();
            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            try {
                if (imageUri != null) {
                    OutputStream outputStream = resolver.openOutputStream(imageUri);
                    if (outputStream != null) {
                        enhancedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                        outputStream.close();

                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            values.clear();
                            values.put(MediaStore.Images.Media.IS_PENDING, 0);
                            resolver.update(imageUri, values, null, null);
                        }

                        Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (IOException e) {
                Toast.makeText(this, "Error saving image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                REQUEST_CAMERA_PERMISSION);
    }

    private void requestStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                    REQUEST_STORAGE_PERMISSION);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Storage permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }
}