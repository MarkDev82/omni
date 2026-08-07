package com.example.omni

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SilentCameraHelper(private val context: Context) {

    fun takeSilentPhoto(lifecycleOwner: LifecycleOwner, onPhotoTaken: (File?) -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                // Enforce Front Camera
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                // Unbind any previous use cases
                cameraProvider.unbindAll()

                // Bind to the Service Lifecycle
                cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, imageCapture
                )

                // Create a temporary file in cache
                val photoFile = File(context.cacheDir, "omni_stealth_${System.currentTimeMillis()}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                Log.d("OmniCamera", "Triggering silent photo capture...")
                
                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onError(exc: ImageCaptureException) {
                            Log.e("OmniCamera", "Photo capture failed: ${exc.message}", exc)
                            cameraProvider.unbindAll()
                            onPhotoTaken(null)
                        }

                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            Log.d("OmniCamera", "Photo capture succeeded: ${photoFile.absolutePath}")
                            cameraProvider.unbindAll()
                            onPhotoTaken(photoFile)
                        }
                    }
                )

            } catch (exc: Exception) {
                Log.e("OmniCamera", "Camera binding failed", exc)
                onPhotoTaken(null)
            }

        }, ContextCompat.getMainExecutor(context))
    }
}
