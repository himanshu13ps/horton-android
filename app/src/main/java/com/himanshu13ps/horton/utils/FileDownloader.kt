package com.himanshu13ps.horton.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object FileDownloader {

    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val progress: Float) : DownloadState()
        data class Finished(val file: File) : DownloadState()
        data class Error(val message: String) : DownloadState()
    }

    fun downloadFile(urlStr: String, destinationFile: File): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0f))
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlStr)
            connection = url.openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                emit(DownloadState.Error("Server returned HTTP ${connection.responseCode} ${connection.responseMessage}"))
                return@flow
            }

            val fileLength = connection.contentLength
            
            // Ensure parent directory exists
            destinationFile.parentFile?.mkdirs()

            val input = connection.inputStream
            val output = FileOutputStream(destinationFile)

            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            var lastProgress = -1f

            while (input.read(data).also { count = it } != -1) {
                total += count.toLong()
                if (fileLength > 0) {
                    val progress = (total * 100 / fileLength).toFloat() / 100f
                    // Only emit on 1% changes to avoid spamming the UI
                    if (progress - lastProgress >= 0.01f || progress >= 1f) {
                        emit(DownloadState.Downloading(progress))
                        lastProgress = progress
                    }
                }
                output.write(data, 0, count)
            }
            output.flush()
            output.close()
            input.close()

            emit(DownloadState.Finished(destinationFile))
        } catch (e: Exception) {
            emit(DownloadState.Error(e.message ?: "Unknown error occurred"))
            // Clean up partial file
            if (destinationFile.exists()) {
                destinationFile.delete()
            }
        } finally {
            connection?.disconnect()
        }
    }.flowOn(Dispatchers.IO)
}
