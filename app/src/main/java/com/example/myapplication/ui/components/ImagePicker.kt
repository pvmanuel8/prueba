package com.example.myapplication.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.example.myapplication.util.PermissionHelper
import java.io.File

/**
 * Componente para seleccionar imágenes con gestión de permisos
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ImagePicker(
    onImageSelected: (Uri) -> Unit,
    onMultipleImagesSelected: (List<Uri>) -> Unit = {},
    allowMultiple: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }

    // Permisos de almacenamiento
    val storagePermissions = PermissionHelper.getRequiredStoragePermissions()
    val storagePermissionState = rememberMultiplePermissionsState(storagePermissions)

    // Permiso de cámara
    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )

    // Launcher para seleccionar imagen de la galería
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = if (allowMultiple) {
            ActivityResultContracts.GetMultipleContents()
        } else {
            ActivityResultContracts.GetContent()
        }
    ) { result ->
        when (result) {
            is Uri -> onImageSelected(result)
            is List<*> -> {
                @Suppress("UNCHECKED_CAST")
                val uris = result as? List<Uri>
                if (uris != null) {
                    onMultipleImagesSelected(uris)
                }
            }
        }
        showDialog = false
    }

    // Launcher para tomar foto con la cámara
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraFile?.let { file ->
                val uri = Uri.fromFile(file)
                onImageSelected(uri)
            }
        }
        showDialog = false
    }

    // Función para abrir la galería
    val openGallery = {
        if (storagePermissionState.allPermissionsGranted) {
            galleryLauncher.launch("image/*")
        } else {
            storagePermissionState.launchMultiplePermissionRequest()
        }
    }

    // Función para abrir la cámara
    val openCamera = {
        if (cameraPermissionState.status.isGranted) {
            // Crear archivo temporal para la foto
            try {
                val file = File(context.cacheDir, "temp_camera_${System.currentTimeMillis()}.jpg")
                tempCameraFile = file

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // Contenido del diálogo
    if (showDialog) {
        ImagePickerDialog(
            onDismiss = { showDialog = false },
            onGalleryClick = {
                openGallery()
            },
            onCameraClick = {
                openCamera()
            },
            showStorageRationale = storagePermissionState.permissions.any {
                it.status.shouldShowRationale
            },
            showCameraRationale = cameraPermissionState.status.shouldShowRationale,
            onRequestStoragePermission = {
                storagePermissionState.launchMultiplePermissionRequest()
            },
            onRequestCameraPermission = {
                cameraPermissionState.launchPermissionRequest()
            }
        )
    }

    // Botón para mostrar el diálogo
    Button(
        onClick = { showDialog = true },
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Collections,
            contentDescription = "Seleccionar imagen"
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (allowMultiple) "Seleccionar Imágenes" else "Seleccionar Imagen")
    }
}

/**
 * Diálogo para elegir entre galería o cámara
 */
@Composable
private fun ImagePickerDialog(
    onDismiss: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    showStorageRationale: Boolean,
    showCameraRationale: Boolean,
    onRequestStoragePermission: () -> Unit,
    onRequestCameraPermission: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Seleccionar imagen")
        },
        text = {
            Column {
                if (showStorageRationale || showCameraRationale) {
                    Text(
                        text = "Esta aplicación necesita permisos para acceder a tus imágenes y cámara.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Botón de galería
                OutlinedButton(
                    onClick = {
                        if (showStorageRationale) {
                            onRequestStoragePermission()
                        } else {
                            onGalleryClick()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Collections,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Galería")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Botón de cámara
                OutlinedButton(
                    onClick = {
                        if (showCameraRationale) {
                            onRequestCameraPermission()
                        } else {
                            onCameraClick()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cámara")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Botón simple para seleccionar múltiples imágenes
 */
@Composable
fun MultipleImagePickerButton(
    onImagesSelected: (List<Uri>) -> Unit,
    maxImages: Int = 10,
    modifier: Modifier = Modifier
) {
    ImagePicker(
        onImageSelected = { uri -> onImagesSelected(listOf(uri)) },
        onMultipleImagesSelected = { uris ->
            onImagesSelected(uris.take(maxImages))
        },
        allowMultiple = true,
        modifier = modifier
    )
}

/**
 * Botón simple para capturar foto con cámara
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraButton(
    onImageCaptured: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var tempCameraFile by remember { mutableStateOf<File?>(null) }

    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraFile?.let { file ->
                val uri = Uri.fromFile(file)
                onImageCaptured(uri)
            }
        }
    }

    Button(
        onClick = {
            if (cameraPermissionState.status.isGranted) {
                try {
                    val file = File(
                        context.cacheDir,
                        "temp_camera_${System.currentTimeMillis()}.jpg"
                    )
                    tempCameraFile = file

                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    cameraLauncher.launch(uri)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                cameraPermissionState.launchPermissionRequest()
            }
        },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary
        )
    ) {
        Icon(
            imageVector = Icons.Default.Camera,
            contentDescription = "Tomar foto"
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Tomar Foto")
    }
}