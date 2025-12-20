package com.example.myapplication.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Helper para gestionar permisos de la aplicación
 */
object PermissionHelper {

    /**
     * Permisos necesarios según la versión de Android
     */
    fun getRequiredStoragePermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            listOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            // Android 10-12 (API 29-32)
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    /**
     * Permiso de cámara
     */
    fun getCameraPermission(): String = Manifest.permission.CAMERA

    /**
     * Verifica si los permisos de almacenamiento están otorgados
     */
    fun hasStoragePermission(context: Context): Boolean {
        return getRequiredStoragePermissions().all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Verifica si el permiso de cámara está otorgado
     */
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Verifica si todos los permisos necesarios están otorgados
     */
    fun hasAllPermissions(context: Context): Boolean {
        return hasStoragePermission(context) && hasCameraPermission(context)
    }
}