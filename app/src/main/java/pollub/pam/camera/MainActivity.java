package pollub.pam.camera;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageCaptureException;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.annotation.NonNull;

import android.net.Uri;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;

import java.io.File;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.ListenableFuture;

public class MainActivity extends AppCompatActivity {
    private final int REQUEST_CODE_PERMISSIONS = 10;
    private final String REQUIRED_PERMISSIONS = Manifest.permission.CAMERA;
    private final String PHOTO_FILENAME = "mojaFotka.jpg";

    PreviewView previewView;
    Button takeButton;

    private ExecutorService cameraExecutor;
    private ImageCapture imageCapture;

    private File outputDirectory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.camera_view);
        takeButton = findViewById(R.id.capture_button);
        cameraExecutor = Executors.newSingleThreadExecutor();

        if(allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{REQUIRED_PERMISSIONS}, REQUEST_CODE_PERMISSIONS);
        }

        outputDirectory = getOutputDirectory();
        takeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhoto();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == REQUEST_CODE_PERMISSIONS) {
            startCamera();
        } else {
            Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    private boolean allPermissionsGranted() {
        int permissionGranted = PackageManager.PERMISSION_GRANTED;
        int permissionRequired = ContextCompat.checkSelfPermission(getBaseContext(),REQUIRED_PERMISSIONS);
        return permissionRequired == permissionGranted;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener( () -> {
            try {
                ProcessCameraProvider cameraProvider = (ProcessCameraProvider)cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder().build();

                Camera camera = cameraProvider.bindToLifecycle(
                        ((LifecycleOwner) this),
                        cameraSelector,
                        preview,
                        imageCapture);

                preview.setSurfaceProvider(
                        previewView.getSurfaceProvider());
            } catch (InterruptedException | ExecutionException e) {}
        }, ContextCompat.getMainExecutor(this));

    }

    private void takePhoto() {
        if(imageCapture == null)
            return;

        File photoFile = new File(outputDirectory, PHOTO_FILENAME);
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Uri savedUri = Uri.fromFile(photoFile);
                String msg = "Photo capture succeeded: " + savedUri;
                Toast.makeText(getBaseContext(),msg,Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Toast.makeText(getBaseContext(),"Failed to save picture!",Toast.LENGTH_SHORT).show();
            }
        });
    }

    private File getOutputDirectory() {
        File[] files = getExternalMediaDirs();
        return files[0];
    }
}