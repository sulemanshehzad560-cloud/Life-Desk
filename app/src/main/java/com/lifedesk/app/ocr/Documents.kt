package com.lifedesk.app.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Stores document photos privately inside the app (never in the public gallery). */
object DocumentStore {
    fun dir(context: Context): File = File(context.filesDir, "docs").apply { mkdirs() }

    fun newPhotoFile(context: Context): File = File(dir(context), "${UUID.randomUUID()}.jpg")

    suspend fun copyIn(context: Context, uri: Uri): File = withContext(Dispatchers.IO) {
        val out = newPhotoFile(context)
        context.contentResolver.openInputStream(uri)?.use { input -> out.outputStream().use { input.copyTo(it) } }
            ?: error("Could not open the image")
        out
    }

    /**
     * Renders the first [maxPages] pages of a PDF (e-invoices, statements, policies) to images inside the app,
     * so they can be read by OCR and kept as the document photo. Returns the page images in order.
     */
    suspend fun pdfPages(context: Context, uri: Uri, maxPages: Int = 4): List<File> = withContext(Dispatchers.IO) {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: error("Could not open the PDF")
        pfd.use { fd ->
            android.graphics.pdf.PdfRenderer(fd).use { renderer ->
                (0 until minOf(renderer.pageCount, maxPages)).map { i ->
                    renderer.openPage(i).use { page ->
                        // ~200 dpi for good OCR, capped to keep memory reasonable.
                        val scale = minOf(200f / 72f, 2400f / maxOf(page.width, page.height))
                        val bmp = Bitmap.createBitmap((page.width * scale).toInt(), (page.height * scale).toInt(), Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val out = newPhotoFile(context)
                        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                        bmp.recycle()
                        out
                    }
                }
            }
        }
    }

    /** Decodes a downscaled, correctly rotated bitmap for display. */
    fun loadBitmap(path: String, maxSize: Int = 1600): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (bounds.outWidth / sample > maxSize || bounds.outHeight / sample > maxSize) sample *= 2
        val bmp = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val degrees = when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) bmp else Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(degrees) }, true)
    }.getOrNull()
}

/** On-device OCR with ML Kit. Works offline; nothing leaves the phone. */
object TextReader {

    suspend fun read(context: Context, file: File): String {
        val image = withContext(Dispatchers.IO) { InputImage.fromFilePath(context, Uri.fromFile(file)) }
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val result = suspendCancellableCoroutine<Text> { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
            return toRows(result)
        } finally {
            recognizer.close()
        }
    }

    /**
     * ML Kit returns text in blocks, which often separates a label ("Total Premium") from its value
     * ("AED 2,850.00") when they sit in different columns. Re-assemble visual rows so the parser sees
     * "Total Premium  AED 2,850.00" on one line.
     */
    private fun toRows(result: Text): String {
        val lines = result.textBlocks.flatMap { it.lines }.filter { it.boundingBox != null }
        if (lines.isEmpty()) return result.text
        val rows = mutableListOf<MutableList<Pair<Rect, String>>>()
        for (line in lines.sortedBy { it.boundingBox!!.top }) {
            val box = line.boundingBox!!
            val row = rows.firstOrNull { r ->
                val ref = r.first().first
                val overlap = minOf(ref.bottom, box.bottom) - maxOf(ref.top, box.top)
                overlap > minOf(ref.height(), box.height()) * 0.5
            }
            if (row != null) row += box to line.text else rows += mutableListOf(box to line.text)
        }
        return rows.joinToString("\n") { r -> r.sortedBy { it.first.left }.joinToString("  ") { it.second } }
    }
}
