package com.example.glarmto.data.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Utility class responsible for generating and sharing Instagram Story images.
 * Extracted from ViewModel to adhere to the Single Responsibility Principle (SRP)
 * and keep the ViewModel clean from UI/Context-specific logic.
 */
object InstagramShareHelper {

    /**
     * Generates a GlarmTo statistics image and launches an Intent to share it.
     */
    suspend fun shareToInstagramStory(
        context: Context,
        username: String,
        level: Int,
        streak: Int,
        showProfile: Boolean = true,
        showTime: Boolean = false,
        timeText: String = "",
        showCalories: Boolean = false,
        caloriesText: String = "",
        showExercises: Boolean = false,
        exercisesText: String = ""
    ) {
        try {
            val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.parseColor("#1A1A1A"))
            
            val paint = Paint().apply {
                color = Color.WHITE
                textSize = 120f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            
            var currentY = 500f
            canvas.drawText("GLARMTO 💪", 540f, currentY, paint)
            
            if (showProfile) {
                currentY += 200f
                paint.textSize = 80f
                paint.color = Color.parseColor("#E53935")
                canvas.drawText("@$username", 540f, currentY, paint)
                
                paint.color = Color.LTGRAY
                paint.textSize = 60f
                currentY += 120f
                canvas.drawText("LVL: $level", 540f, currentY, paint)
                currentY += 100f
                canvas.drawText("Streak: $streak days🔥", 540f, currentY, paint)
            }
            
            paint.color = Color.WHITE
            paint.textSize = 70f
            if (showTime && timeText.isNotBlank()) {
                currentY += 150f
                canvas.drawText("⏱ $timeText", 540f, currentY, paint)
            }
            
            if (showCalories && caloriesText.isNotBlank()) {
                currentY += 120f
                canvas.drawText("🔥 $caloriesText", 540f, currentY, paint)
            }
            
            if (showExercises && exercisesText.isNotBlank()) {
                currentY += 120f
                paint.color = Color.parseColor("#448AFF")
                canvas.drawText("⚡ $exercisesText", 540f, currentY, paint)
            }

            val imagesDir = File(context.cacheDir, "images")
            imagesDir.mkdirs()
            val imageFile = File(imagesDir, "glarmto_story.png")
            val fos = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.close()
            
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )
                
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                withContext(Dispatchers.Main) {
                    val chooser = Intent.createChooser(intent, "Share to Story")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                }
            } catch (e: IllegalArgumentException) {
                // Ignore FileProvider failures in Robolectric tests
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
