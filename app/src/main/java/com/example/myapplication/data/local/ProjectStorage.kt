package com.example.myapplication.data.local


import android.content.Context
import com.example.myapplication.data.model.ImageProject
import com.example.myapplication.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Gestión de almacenamiento local de proyectos
 */
class ProjectStorage(private val context: Context) {

    private val projectsFile: File by lazy {
        File(context.filesDir, Constants.PROJECTS_FILE)
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Guarda la lista completa de proyectos
     */
    suspend fun saveProjects(projects: List<ImageProject>): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val jsonString = json.encodeToString(projects)
                projectsFile.writeText(jsonString)
                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }

    /**
     * Carga todos los proyectos
     */
    suspend fun loadProjects(): Result<List<ImageProject>> =
        withContext(Dispatchers.IO) {
            try {
                if (!projectsFile.exists()) {
                    return@withContext Result.success(emptyList())
                }

                val jsonString = projectsFile.readText()
                val projects = json.decodeFromString<List<ImageProject>>(jsonString)
                Result.success(projects)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }

    /**
     * Añade un nuevo proyecto
     */
    suspend fun addProject(project: ImageProject): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val currentProjects = loadProjects().getOrNull() ?: emptyList()
                val updatedProjects = currentProjects + project
                saveProjects(updatedProjects)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Actualiza un proyecto existente
     */
    suspend fun updateProject(project: ImageProject): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val currentProjects = loadProjects().getOrNull() ?: emptyList()
                val updatedProjects = currentProjects.map {
                    if (it.id == project.id) project else it
                }
                saveProjects(updatedProjects)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Elimina un proyecto
     */
    suspend fun deleteProject(projectId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val currentProjects = loadProjects().getOrNull() ?: emptyList()
                val updatedProjects = currentProjects.filter { it.id != projectId }
                saveProjects(updatedProjects)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Obtiene un proyecto por ID
     */
    suspend fun getProject(projectId: String): Result<ImageProject?> =
        withContext(Dispatchers.IO) {
            try {
                val projects = loadProjects().getOrNull() ?: emptyList()
                val project = projects.find { it.id == projectId }
                Result.success(project)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Limpia proyectos antiguos
     */
    suspend fun cleanOldProjects(daysOld: Int = 30): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val threshold = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
                val currentProjects = loadProjects().getOrNull() ?: emptyList()

                val (old, recent) = currentProjects.partition { it.timestamp < threshold }

                saveProjects(recent)
                Result.success(old.size)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Obtiene el número total de proyectos
     */
    suspend fun getProjectCount(): Int {
        return loadProjects().getOrNull()?.size ?: 0
    }

    /**
     * Limpia todos los proyectos
     */
    suspend fun clearAllProjects(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (projectsFile.exists()) {
                projectsFile.delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}