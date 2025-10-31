package com.cadetex.service

import com.cadetex.service.error
import com.cadetex.service.success
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ObjectCannedACL
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import java.io.InputStream
import java.util.*

private val logger = LoggerFactory.getLogger("S3Service")

/**
 * Servicio para interactuar con AWS S3
 */
class S3Service {
    private val s3Client: S3Client? by lazy {
        // Solo inicializar S3Client si NO estamos en modo test
        if (isTestMode()) {
            null
        } else {
            S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build()
        }
    }
    private val bucketName: String
    private val region: String

    init {
        // Leer configuración de entorno
        bucketName = System.getenv("S3_BUCKET_NAME") 
            ?: System.getProperty("S3_BUCKET_NAME") 
            ?: "kdt-photos-prod-sa-east-1"  // Fallback al bucket real
        
        region = System.getenv("AWS_REGION") 
            ?: System.getProperty("AWS_REGION") 
            ?: "sa-east-1"

        if (isTestMode()) {
            logger.info("S3Service initialized in TEST MODE (S3 uploads disabled)")
        } else {
            logger.info("S3Service initialized with bucket: $bucketName, region: $region")
        }
    }

    private fun isTestMode(): Boolean {
        // Si S3_DISABLED está explícitamente configurado, usar modo test
        if (System.getenv("S3_DISABLED") == "true" || System.getProperty("S3_DISABLED") == "true") {
            return true
        }
        
        // Si S3_BUCKET_NAME está configurado como variable de entorno, estamos en producción
        // En EC2 con IAM role, no necesitamos AWS_ACCESS_KEY_ID - se usa automáticamente
        val envBucketName = System.getenv("S3_BUCKET_NAME") ?: System.getProperty("S3_BUCKET_NAME")
        return envBucketName.isNullOrEmpty() // Modo test solo si no hay bucket configurado
    }

    /**
     * Sube un archivo a S3 y retorna la URL pública
     * @param key Ruta del archivo en S3 (ej: "tasks/task-123/photo.jpg")
     * @param inputStream Stream del archivo a subir
     * @param contentType Tipo MIME del archivo (ej: "image/jpeg")
     * @return URL pública del archivo en S3
     */
    suspend fun uploadFile(
        key: String,
        inputStream: InputStream,
        contentType: String = "image/jpeg"
    ): Result<String> {
        return try {
            val bytes = inputStream.readBytes()
            
            // En modo test, simular la subida y retornar URL simulada
            if (isTestMode()) {
                val publicUrl = "https://${bucketName}.s3.${region}.amazonaws.com/${key}"
                logger.info("S3 TEST MODE: Simulated upload for $key, URL: $publicUrl")
                return success(publicUrl)
            }
            
            // En producción, subir realmente a S3 con ACL público para lectura
            val putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .acl(ObjectCannedACL.PUBLIC_READ) // Permitir lectura pública del objeto
                .build()

            val putObjectResponse: PutObjectResponse = s3Client!!.putObject(
                putObjectRequest,
                RequestBody.fromBytes(bytes)
            )

            // Construir URL pública
            val publicUrl = "https://${bucketName}.s3.${region}.amazonaws.com/${key}"
            
            logger.info("File uploaded successfully to S3: $key, URL: $publicUrl")
            success(publicUrl)
        } catch (e: Exception) {
            logger.error("Error uploading file to S3: ${e.message}", e)
            error("Error al subir archivo a S3: ${e.message}")
        }
    }

    /**
     * Genera una clave única para una foto de tarea
     * Formato: tasks/{taskId}/{photoType}/{timestamp-uuid}.{extension}
     */
    fun generateTaskPhotoKey(
        taskId: String,
        photoType: String,
        extension: String = "jpg"
    ): String {
        val timestamp = System.currentTimeMillis()
        val uuid = UUID.randomUUID().toString().take(8)
        return "tasks/$taskId/$photoType/${timestamp}-${uuid}.$extension"
    }
}

