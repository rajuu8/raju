package com.pcontrol.child.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Base64
import android.util.DisplayMetrics
import androidx.core.app.NotificationCompat
import com.pcontrol.child.R
import com.pcontrol.child.network.ApiClient
import com.pcontrol.child.network.DeviceConfig
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import android.graphics.Bitmap

/**
 * Screen mirroring service using Android's MediaProjection API.
 *
 * IMPORTANT / BY DESIGN:
 * - Android REQUIRES a user-visible consent dialog before this can start
 *   (system permission prompt showing "X will start capturing everything
 *   displayed on your screen").
 * - Android REQUIRES a persistent foreground notification for the entire
 *   duration capture is active. This cannot be hidden or suppressed - it's
 *   an OS-level protection, not an app choice.
 * - This captures periodic snapshots (not continuous video) and uploads
 *   them to the backend, which the parent app polls/receives via socket.
 *   For true continuous live video you would need to extend this with
 *   WebRTC + a signaling server.
 */
class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var capturing = false

    companion object {
        const val CHANNEL_ID = "screen_capture_channel"
        const val NOTIFICATION_ID = 2001
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val CAPTURE_INTERVAL_MS = 3000L // snapshot every 3 seconds while active
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED) ?: return START_NOT_STICKY
        val resultData: Intent? = intent.getParcelableExtra(EXTRA_RESULT_DATA)

        // This notification is MANDATORY while MediaProjection runs - Android enforces this,
        // it will stop capture automatically if the foreground service/notification is removed.
        startForeground(NOTIFICATION_ID, buildNotification())

        if (resultData != null) {
            startCapture(resultCode, resultData)
        }

        return START_NOT_STICKY
    }

    private fun startCapture(resultCode: Int, data: Intent) {
        val projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        val metrics = DisplayMetrics()
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
        windowManager.defaultDisplay.getMetrics(metrics)

        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture", width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        capturing = true
        scheduleNextCapture()
    }

    private fun scheduleNextCapture() {
        if (!capturing) return
        handler.postDelayed({
            captureFrame()
            scheduleNextCapture()
        }, CAPTURE_INTERVAL_MS)
    }

    private fun captureFrame() {
        val image: Image = imageReader?.acquireLatestImage() ?: return
        try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width

            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
            val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            val deviceId = DeviceConfig.getDeviceId(this)
            if (deviceId != null) {
                val json = JSONObject().apply { put("imageBase64", base64Image) }
                ApiClient.post("/api/screen/$deviceId", json) { _, _ -> }
            }

            bitmap.recycle()
        } catch (e: Exception) {
            // Skip this frame on error
        } finally {
            image.close()
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen sharing active")
            .setContentText("Your screen is being shared with parent app")
            .setSmallIcon(R.drawable.ic_shield)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // deliberately high visibility - not hidden
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Sharing",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        capturing = false
        handler.removeCallbacksAndMessages(null)
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
