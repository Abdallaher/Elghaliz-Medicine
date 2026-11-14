@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import com.example.data.model.Lecture
import com.example.data.model.SessionPreferences
import com.example.data.storage.EncryptedVideoVault
import com.example.data.storage.GoogleDrive5TbVaultManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

private const val PLAYBACK_POSITIONS_PREFS = "elghaliz_lecture_playback_positions"
private const val MAX_VIDEO_SIMPLE_CACHE_BYTES = 250L * 1024L * 1024L // 250 MB SimpleCache limit
private const val CACHE_FRAGMENT_SIZE_BYTES = 256L * 1024L // 256 KB cache fragments for fast moov atom seeking
private val AVAILABLE_SPEEDS = listOf(1.0f, 1.25f, 1.5f, 2.0f)

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun getSavedPlaybackSecond(context: Context, lectureId: String): Int {
    if (lectureId.isBlank()) return 0
    val prefs = context.applicationContext.getSharedPreferences(PLAYBACK_POSITIONS_PREFS, Context.MODE_PRIVATE)
    return prefs.getInt("pos_$lectureId", 0).coerceAtLeast(0)
}

private fun savePlaybackSecond(context: Context, lectureId: String, second: Int, totalSeconds: Int) {
    if (lectureId.isBlank()) return
    val prefs = context.applicationContext.getSharedPreferences(PLAYBACK_POSITIONS_PREFS, Context.MODE_PRIVATE)
    val safeSec = if (totalSeconds > 10 && second >= totalSeconds - 3) 0 else second.coerceAtLeast(0)
    prefs.edit().putInt("pos_$lectureId", safeSec).apply()
}

private data class ResolvedMedia3StreamTarget(
    val uri: Uri,
    val cacheKey: String,
    val localVaultFile: File? = null,
    val isLocalOffline: Boolean = false
)

/**
 * مدير التخزين المؤقت الموحد (Singleton SimpleCache) بحد أقصى 250MB
 * لبث فيديوهات Google Drive عبر OkHttpDataSource بسرعة فائقة مع دعم التقديم والترجيع والسرعات
 * دون إعادة تحميل المشاهد السابقة.
 */
private object Media3VideoCacheHolder {
    @Volatile
    private var simpleCache: SimpleCache? = null

    val sharedOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    @Synchronized
    fun getOrCreateCache(context: Context): SimpleCache {
        val existing = simpleCache
        if (existing != null) return existing
        val cacheDir = File(context.applicationContext.cacheDir, "elghaliz_media3_simple_cache").apply {
            if (!exists()) mkdirs()
        }
        val evictor = LeastRecentlyUsedCacheEvictor(MAX_VIDEO_SIMPLE_CACHE_BYTES)
        val databaseProvider = StandaloneDatabaseProvider(context.applicationContext)
        return SimpleCache(cacheDir, evictor, databaseProvider).also {
            simpleCache = it
        }
    }
}

/**
 * DataSource مخصص داخل Media3 يفك تشفير أول 4096 بايت (4KB Header Lock) من ملفات (.elgdrm)
 * لحظياً في الذاكرة (RAM) أثناء القراءة (On-the-Fly) دون تعديل أو نسخ الملف على القرص نهائياً،
 * مع قراءة جميع البايتات بعد 4096 بسرعة القرص المباشرة (0ms CPU overhead)،
 * ليفتح الفيديو الأوفلاين (حتى لو حجمه 1GB) في أقل من 0.05 ثانية.
 */
private class ElgdrmOnTheFlyDataSource(
    private val lectureId: String
) : BaseDataSource(/* isNetwork = */ false) {

    private var randomAccessFile: RandomAccessFile? = null
    private var uri: Uri? = null
    private var bytesRemaining: Long = 0L
    private var currentFilePosition: Long = 0L
    private var opened: Boolean = false

    // Encryption modes:
    // 0 = Plain MP4 (no decryption needed)
    // 1 = Standard 4096-byte Header Lock (decrypt only when position < 4096L -> 0ms CPU overhead after 4KB)
    // 2 = Legacy Full-Stream Encrypted file (backward compatibility for older downloads)
    // 3 = Legacy Partially-Unlocked 64-byte file (bytes 0..63 plain on disk, bytes 64..4095 or full stream encrypted)
    private var encryptionMode: Int = 1
    private var encryptedStartOffset: Long = 0L

    override fun open(dataSpec: DataSpec): Long {
        val targetUri = dataSpec.uri
        uri = targetUri
        transferInitializing(dataSpec)

        val path = targetUri.path ?: throw IOException("مسار ملف الفيديو غير صالح: $targetUri")
        val file = File(path)
        if (!file.exists() || file.length() <= 0L) {
            throw IOException("ملف المحاضرة المحلي غير موجود")
        }

        val raf = RandomAccessFile(file, "r")
        randomAccessFile = raf
        val fileLength = raf.length()
        if (dataSpec.position > fileLength) {
            raf.close()
            randomAccessFile = null
            throw EOFException("Position out of range")
        }

        // فحص سريع في الذاكرة لتحديد وضع تشفير الترويسة (4096 بايت) أو التوافق الخلفي دون المساس بالملف على القرص
        detectEncryptionMode(raf, file, fileLength)

        raf.seek(dataSpec.position)
        currentFilePosition = dataSpec.position
        bytesRemaining = if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
            fileLength - dataSpec.position
        } else {
            minOf(dataSpec.length, fileLength - dataSpec.position)
        }

        opened = true
        transferStarted(dataSpec)
        return bytesRemaining
    }

    private fun detectEncryptionMode(raf: RandomAccessFile, file: File, fileLength: Long) {
        if (!file.name.endsWith(".elgdrm", ignoreCase = true) || fileLength < 16L) {
            encryptionMode = 0
            encryptedStartOffset = 0L
            return
        }
        runCatching {
            val probeLen = minOf(128L, fileLength).toInt()
            val probe = ByteArray(probeLen)
            raf.seek(0L)
            raf.readFully(probe, 0, probeLen)

            val hasPlainStart = GoogleDrive5TbVaultManager.hasPlainMp4BoxHeaderBytes(probe)
            if (!hasPlainStart) {
                // الترويسة مشفرة من البايت 0 (الوضع القياسي: أول 4096 بايت مشفرة)
                encryptedStartOffset = 0L
                encryptionMode = if (isLegacyEncryptedBeyond4096(raf, fileLength)) 2 else 1
            } else {
                // إذا كان أول 64 بايت قد فُك تشفيرها سابقاً على القرص في نسخة قديمة بينما باقي الملف مشفر
                if (isLegacyEncryptedAtOffset64(probe, raf, fileLength)) {
                    encryptedStartOffset = 64L
                    encryptionMode = if (isLegacyEncryptedBeyond4096(raf, fileLength)) 2 else 1
                } else {
                    encryptionMode = 0
                    encryptedStartOffset = 0L
                }
            }
        }.onFailure {
            encryptionMode = 1
            encryptedStartOffset = 0L
        }
    }

    private fun isLegacyEncryptedAtOffset64(probe: ByteArray, raf: RandomAccessFile, fileLength: Long): Boolean {
        if (probe.size < 16) return false
        // قراءة حجم الصندوق الأول (ftyp) من أول 4 بايتات
        val firstBoxSize = ((probe[0].toInt() and 0xFF) shl 24) or
            ((probe[1].toInt() and 0xFF) shl 16) or
            ((probe[2].toInt() and 0xFF) shl 8) or
            (probe[3].toInt() and 0xFF)
        if (firstBoxSize in 8..60 && firstBoxSize + 8 <= probe.size) {
            val secondBoxType = String(probe, firstBoxSize + 4, 4, Charsets.US_ASCII).lowercase()
            if (secondBoxType in setOf("moov", "mdat", "free", "wide", "skip", "uuid")) {
                // Check if bytes after 64 inside moov look encrypted
                if (fileLength > 128L) {
                    val sample = probe.copyOfRange(64, minOf(128, probe.size))
                    val decSample = sample.copyOf()
                    EncryptedVideoVault.transformBytesFullStreamAtOffset(lectureId, decSample, 0, decSample.size, 64L)
                    if (!containsKnownMp4Atom(sample) && containsKnownMp4Atom(decSample)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun isLegacyEncryptedBeyond4096(raf: RandomAccessFile, fileLength: Long): Boolean {
        if (fileLength <= EncryptedVideoVault.ENCRYPTED_HEADER_BYTES + 128L) {
            return false
        }
        return runCatching {
            val headerProbe = ByteArray(64)
            raf.seek(0L)
            raf.readFully(headerProbe)
            if (!GoogleDrive5TbVaultManager.hasPlainMp4BoxHeaderBytes(headerProbe)) {
                EncryptedVideoVault.transformBytesAtOffset(lectureId, headerProbe, 0, headerProbe.size, 0L)
            }
            // Locate second box offset (right after ftyp)
            val firstBoxSize = ((headerProbe[0].toInt() and 0xFF) shl 24) or
                ((headerProbe[1].toInt() and 0xFF) shl 16) or
                ((headerProbe[2].toInt() and 0xFF) shl 8) or
                (headerProbe[3].toInt() and 0xFF)
            if (firstBoxSize in 8..48 && firstBoxSize + 8 <= headerProbe.size) {
                val secondBoxSize = ((headerProbe[firstBoxSize].toInt() and 0xFF).toLong() shl 24) or
                    ((headerProbe[firstBoxSize + 1].toInt() and 0xFF).toLong() shl 16) or
                    ((headerProbe[firstBoxSize + 2].toInt() and 0xFF).toLong() shl 8) or
                    (headerProbe[firstBoxSize + 3].toInt() and 0xFF).toLong()
                val nextBoxOffset = firstBoxSize.toLong() + secondBoxSize
                if (nextBoxOffset >= EncryptedVideoVault.ENCRYPTED_HEADER_BYTES && nextBoxOffset + 8L <= fileLength) {
                    val boxHeader = ByteArray(8)
                    raf.seek(nextBoxOffset)
                    raf.readFully(boxHeader)
                    val rawType = String(boxHeader, 4, 4, Charsets.US_ASCII).lowercase()
                    if (rawType in setOf("moov", "mdat", "free", "wide", "skip", "uuid", "ftyp")) {
                        return@runCatching false // Plain after 4096!
                    }
                    val decBox = boxHeader.copyOf()
                    EncryptedVideoVault.transformBytesFullStreamAtOffset(lectureId, decBox, 0, 8, nextBoxOffset)
                    val decType = String(decBox, 4, 4, Charsets.US_ASCII).lowercase()
                    if (decType in setOf("moov", "mdat", "free", "wide", "skip", "uuid", "ftyp")) {
                        return@runCatching true // Legacy full-stream encrypted!
                    }
                }
            }
            // Also check offset 4096..4608 for known MP4 atoms
            val tailWindowLen = minOf(512L, fileLength - EncryptedVideoVault.ENCRYPTED_HEADER_BYTES).toInt()
            val tailBuf = ByteArray(tailWindowLen)
            raf.seek(EncryptedVideoVault.ENCRYPTED_HEADER_BYTES)
            raf.readFully(tailBuf)
            if (containsKnownMp4Atom(tailBuf)) {
                return@runCatching false
            }
            val decTail = tailBuf.copyOf()
            EncryptedVideoVault.transformBytesFullStreamAtOffset(
                lectureId,
                decTail,
                0,
                decTail.size,
                EncryptedVideoVault.ENCRYPTED_HEADER_BYTES
            )
            containsKnownMp4Atom(decTail)
        }.getOrDefault(false)
    }

    private fun containsKnownMp4Atom(bytes: ByteArray): Boolean {
        val known = setOf("mvhd", "trak", "tkhd", "mdia", "mdhd", "hdlr", "minf", "vmhd", "smhd", "dinf", "stbl", "stsd", "avc1", "mp4a", "mdat", "moov")
        if (bytes.size < 4) return false
        for (i in 0..bytes.size - 4) {
            val c0 = bytes[i].toInt()
            val c1 = bytes[i + 1].toInt()
            val c2 = bytes[i + 2].toInt()
            val c3 = bytes[i + 3].toInt()
            if (c0 in 97..122 && c1 in 97..122 && c2 in 97..122 && (c3 in 97..122 || c3 in 48..57)) {
                val atom = String(bytes, i, 4, Charsets.US_ASCII)
                if (atom in known) return true
            }
        }
        return false
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        val raf = randomAccessFile ?: return C.RESULT_END_OF_INPUT
        val bytesToRead = minOf(bytesRemaining, length.toLong()).toInt()
        val readStartPos = currentFilePosition
        val bytesRead = raf.read(buffer, offset, bytesToRead)

        if (bytesRead == -1) {
            if (bytesRemaining != C.LENGTH_UNSET.toLong() && bytesRemaining > 0L) {
                throw EOFException()
            }
            return C.RESULT_END_OF_INPUT
        }

        // فك التشفير اللحظي في الذاكرة (RAM) لأول 4096 بايت فقط (position < 4096L)
        // وعندما يكون position >= 4096L تُقرأ البايتات بسرعة القرص الصلب المباشرة (0ms CPU overhead)
        when (encryptionMode) {
            1 -> {
                if (readStartPos < EncryptedVideoVault.ENCRYPTED_HEADER_BYTES) {
                    val regionStart = maxOf(readStartPos, encryptedStartOffset)
                    val regionEnd = minOf(readStartPos + bytesRead, EncryptedVideoVault.ENCRYPTED_HEADER_BYTES)
                    if (regionEnd > regionStart) {
                        val subOffset = offset + (regionStart - readStartPos).toInt()
                        val subLength = (regionEnd - regionStart).toInt()
                        EncryptedVideoVault.transformBytesAtOffset(
                            lectureId = lectureId,
                            buffer = buffer,
                            bufferOffset = subOffset,
                            length = subLength,
                            startByteIndex = regionStart
                        )
                    }
                }
            }
            2 -> {
                val regionStart = maxOf(readStartPos, encryptedStartOffset)
                val regionEnd = readStartPos + bytesRead
                if (regionEnd > regionStart) {
                    val subOffset = offset + (regionStart - readStartPos).toInt()
                    val subLength = (regionEnd - regionStart).toInt()
                    EncryptedVideoVault.transformBytesFullStreamAtOffset(
                        lectureId = lectureId,
                        buffer = buffer,
                        bufferOffset = subOffset,
                        length = subLength,
                        startByteIndex = regionStart
                    )
                }
            }
        }

        currentFilePosition += bytesRead
        if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            bytesRemaining -= bytesRead
        }
        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        uri = null
        try {
            randomAccessFile?.close()
        } catch (e: IOException) {
            throw e
        } finally {
            randomAccessFile = null
            if (opened) {
                opened = false
                transferEnded()
            }
        }
    }
}

@Composable
fun MedicalVideoPlayerDialog(
    lecture: Lecture,
    isPreparingVideo: Boolean,
    preparingVideoProgress: Int = 0,
    isDownloadedInApp: Boolean,
    isDownloadingNow: Boolean,
    downloadProgressPercent: Int = 0,
    userEmail: String = "",
    onDownloadInAppClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { context.findActivity() }
    val layoutDirection = LocalLayoutDirection.current

    // منع تصوير الشاشة ولقطات الشاشة 100% داخل مشغل الفيديو
    DisposableEffect(Unit) {
        activity?.window?.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )
        onDispose {}
    }

    var isFullscreenLandscape by rememberSaveable { mutableStateOf(false) }
    var isScreenLocked by rememberSaveable { mutableStateOf(false) }
    var showOverlayControls by remember { mutableStateOf(true) }
    var controlsInteractionTick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var playbackSpeed by rememberSaveable { mutableFloatStateOf(1.0f) }

    // Pinch-to-Zoom (1x to 3x) & Pan state for reading small text on medical slides
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    // Double-tap visual feedback badge ("-10 ثوانٍ" / "+10 ثوانٍ")
    var seekFeedbackLabel by remember { mutableStateOf<String?>(null) }
    var seekFeedbackTick by remember { mutableLongStateOf(0L) }

    var isPlaying by remember { mutableStateOf(true) }
    var isPlayerPrepared by remember(lecture.id, lecture.videoSourceUri) { mutableStateOf(false) }
    var isBuffering by remember(lecture.id, lecture.videoSourceUri) { mutableStateOf(true) }
    var playbackErrorMessage by remember(lecture.id, lecture.videoSourceUri) { mutableStateOf<String?>(null) }
    var reloadAttempt by remember(lecture.id, lecture.videoSourceUri) { mutableIntStateOf(0) }

    // Dynamic moving watermark step (changes position gently every 6 seconds to prevent external camera piracy)
    var watermarkStep by remember { mutableIntStateOf(0) }
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(6000L)
            watermarkStep = (watermarkStep + 1) % 4
        }
    }

    val initialSavedSecond = remember(lecture.id) {
        getSavedPlaybackSecond(context, lecture.id)
    }
    var currentSecond by remember(lecture.id) { mutableIntStateOf(initialSavedSecond) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderDragSecond by remember { mutableIntStateOf(initialSavedSecond) }

    var totalSeconds by remember(lecture.durationSeconds) {
        mutableIntStateOf(lecture.durationSeconds.toInt().coerceAtLeast(1))
    }

    var streamTarget by remember(lecture.id, lecture.videoSourceUri) {
        mutableStateOf<ResolvedMedia3StreamTarget?>(null)
    }

    // تسخين مبدئي للتوكن من SessionPreferences في حال لم يكن في الذاكرة بعد
    remember(context) {
        GoogleDrive5TbVaultManager.seedCachedTokenIfEmpty(
            SessionPreferences.getLastKnownDriveToken(context)
        )
        true
    }

    // بناء مشغل Media3 ExoPlayer مع:
    // 1. ResolvingDataSource.Factory لحقن التوكن الفوري (0ms) وتجديده فقط عند 401/403
    // 2. SimpleCache (250MB) بقطعة كاش صغيرة 256KB وبدون FLAG_BLOCK_ON_CACHE لفتح ملفات 330MB-1GB فوراً
    // 3. Mp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS لتسريع فهرسة الفيديوهات الطويلة (3 ساعات+)
    // 4. DefaultLoadControl مضبوط للبدء الفوري السريع (bufferForPlaybackMs = 250ms)
    val exoPlayer = remember(lecture.id) {
        val appCtx = context.applicationContext
        val simpleCache = Media3VideoCacheHolder.getOrCreateCache(appCtx)

        val okHttpFactory = OkHttpDataSource.Factory(Media3VideoCacheHolder.sharedOkHttpClient)
            .setUserAgent("ElghalizMedicine-Media3/2.0 (Android)")

        // Upstream DataSource that retries automatically on 401 / 403 by fetching a fresh token from Apps Script
        val driveRetryUpstreamFactory = DataSource.Factory {
            val baseHttpDataSource = okHttpFactory.createDataSource()
            object : DataSource {
                override fun addTransferListener(transferListener: androidx.media3.datasource.TransferListener) {
                    baseHttpDataSource.addTransferListener(transferListener)
                }

                override fun open(dataSpec: DataSpec): Long {
                    return try {
                        baseHttpDataSource.open(dataSpec)
                    } catch (e: HttpDataSource.InvalidResponseCodeException) {
                        if (e.responseCode == 401 || e.responseCode == 403) {
                            runCatching { baseHttpDataSource.close() }
                            val refreshedToken = runBlocking(Dispatchers.IO) {
                                GoogleDrive5TbVaultManager.fetchUploadParamsFromAppsScript(forceRefresh = true)
                                    .getOrNull()
                                    ?.also { params ->
                                        if (params.accessToken.isNotBlank()) {
                                            SessionPreferences.saveLastKnownDriveToken(
                                                appCtx,
                                                params.accessToken,
                                                params.folderId
                                            )
                                        }
                                    }
                                    ?.accessToken
                                    ?.trim()
                                    .orEmpty()
                            }
                            if (refreshedToken.isNotBlank()) {
                                val updatedHeaders = HashMap(dataSpec.httpRequestHeaders)
                                updatedHeaders["Authorization"] = "Bearer $refreshedToken"
                                val retrySpec = dataSpec.buildUpon()
                                    .setHttpRequestHeaders(updatedHeaders)
                                    .build()
                                return baseHttpDataSource.open(retrySpec)
                            }
                        }
                        throw e
                    }
                }

                override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                    return baseHttpDataSource.read(buffer, offset, length)
                }

                override fun getUri(): Uri? = baseHttpDataSource.uri

                override fun getResponseHeaders(): Map<String, List<String>> = baseHttpDataSource.responseHeaders

                override fun close() {
                    baseHttpDataSource.close()
                }
            }
        }

        // ResolvingDataSource.Factory يحقن ترويسة Authorization: Bearer <token> تلقائياً من الذاكرة في 0ms
        val resolvingHttpFactory = ResolvingDataSource.Factory(
            driveRetryUpstreamFactory,
            object : ResolvingDataSource.Resolver {
                override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
                    val host = dataSpec.uri.host?.lowercase().orEmpty()
                    if (host.contains("googleapis.com") || host.contains("google.com")) {
                        var token = GoogleDrive5TbVaultManager.getInstantAccessTokenOrNull()
                            ?: SessionPreferences.getLastKnownDriveToken(appCtx).orEmpty()
                        if (token.isBlank() && !android.os.Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) {
                            // Only if memory & disk cache are completely empty on first-ever cold boot
                            token = runBlocking(Dispatchers.IO) {
                                GoogleDrive5TbVaultManager.fetchUploadParamsFromAppsScript(forceRefresh = false)
                                    .getOrNull()
                                    ?.also { params ->
                                        if (params.accessToken.isNotBlank()) {
                                            SessionPreferences.saveLastKnownDriveToken(
                                                appCtx,
                                                params.accessToken,
                                                params.folderId
                                            )
                                        }
                                    }
                                    ?.accessToken
                                    ?.trim()
                                    .orEmpty()
                            }
                        }
                        if (token.isNotBlank()) {
                            return dataSpec.withAdditionalHeaders(
                                mapOf("Authorization" to "Bearer $token")
                            )
                        }
                    }
                    return dataSpec
                }
            }
        )

        // CacheDataSource.Factory مع قطعة كاش 256KB وبدون FLAG_BLOCK_ON_CACHE لعدم تأخير قراءة moov atom في نهاية ملفات MP4 الكبيرة
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(resolvingHttpFactory)
            .setCacheWriteDataSinkFactory(
                androidx.media3.datasource.cache.CacheDataSink.Factory()
                    .setCache(simpleCache)
                    .setFragmentSize(CACHE_FRAGMENT_SIZE_BYTES) // 256 KB instead of 2 MB
            )
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val defaultDataSourceFactory = DefaultDataSource.Factory(appCtx, resolvingHttpFactory)

        val routingDataSourceFactory = DataSource.Factory {
            object : DataSource {
                private var activeDelegate: DataSource? = null

                override fun addTransferListener(transferListener: androidx.media3.datasource.TransferListener) {
                    // Handled per delegate
                }

                override fun open(dataSpec: DataSpec): Long {
                    val scheme = dataSpec.uri.scheme?.lowercase().orEmpty()
                    val path = dataSpec.uri.path.orEmpty()
                    val delegate: DataSource = when {
                        (scheme == "file" || scheme.isEmpty()) && path.endsWith(".elgdrm", ignoreCase = true) -> {
                            ElgdrmOnTheFlyDataSource(lecture.id)
                        }
                        scheme == "file" || scheme.isEmpty() -> {
                            FileDataSource()
                        }
                        scheme == "http" || scheme == "https" -> {
                            cacheDataSourceFactory.createDataSource()
                        }
                        else -> {
                            defaultDataSourceFactory.createDataSource()
                        }
                    }
                    activeDelegate = delegate
                    return delegate.open(dataSpec)
                }

                override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                    return activeDelegate?.read(buffer, offset, length) ?: C.RESULT_END_OF_INPUT
                }

                override fun getUri(): Uri? = activeDelegate?.uri

                override fun getResponseHeaders(): Map<String, List<String>> {
                    return activeDelegate?.responseHeaders ?: emptyMap()
                }

                override fun close() {
                    try {
                        activeDelegate?.close()
                    } finally {
                        activeDelegate = null
                    }
                }
            }
        }

        // تفعيل Mp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS لتسريع فهرسة الفيديوهات الطويلة (3 ساعات+)
        val extractorsFactory = DefaultExtractorsFactory()
            .setMp4ExtractorFlags(Mp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS)

        // ضبط DefaultLoadControl للبدء الفوري السريع (bufferForPlaybackMs = 250ms)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 2_500,
                /* maxBufferMs = */ 30_000,
                /* bufferForPlaybackMs = */ 250,
                /* bufferForPlaybackAfterRebufferMs = */ 600
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(30_000, true)
            .build()

        ExoPlayer.Builder(appCtx)
            .setMediaSourceFactory(DefaultMediaSourceFactory(routingDataSourceFactory, extractorsFactory))
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(10_000L)
            .setSeekForwardIncrementMs(10_000L)
            .build()
            .apply {
                playWhenReady = true
            }
    }

    // تحرير الذاكرة فوراً (Lifecycle Release) عند إغلاق المشغل لمنع أي استهلاك للرام أو لاج في التطبيق
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        isBuffering = true
                    }
                    Player.STATE_READY -> {
                        isBuffering = false
                        isPlayerPrepared = true
                        playbackErrorMessage = null
                        val durMs = exoPlayer.duration
                        if (durMs != C.TIME_UNSET && durMs > 0L) {
                            totalSeconds = (durMs / 1000L).toInt().coerceAtLeast(1)
                        }
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        isPlaying = false
                        currentSecond = totalSeconds
                        sliderDragSecond = totalSeconds
                        savePlaybackSecond(context, lecture.id, 0, totalSeconds)
                        showOverlayControls = true
                    }
                    Player.STATE_IDLE -> {
                        // Idle state
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                val badLocal = streamTarget?.localVaultFile
                if (badLocal != null &&
                    !EncryptedVideoVault.isValidEncryptedOrPlainVaultFile(lecture.id, badLocal)
                ) {
                    runCatching { badLocal.delete() }
                }
                isBuffering = false
                playbackErrorMessage = if (streamTarget?.isLocalOffline == true) {
                    "تعذر تشغيل ملف الفيديو المحلي. يمكنك الضغط على إعادة المحاولة أو إعادة تنزيل المحاضرة."
                } else {
                    "تعذر فتح دفق الفيديو. يرجى التحقق من اتصال الإنترنت والمحاولة مرة أخرى."
                }
            }
        }

        exoPlayer.addListener(listener)

        onDispose {
            val finalPosSec = (exoPlayer.currentPosition / 1000L).toInt().coerceAtLeast(0)
            savePlaybackSecond(context, lecture.id, finalPosSec, totalSeconds)
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.release()
        }
    }

    fun registerUserInteraction() {
        controlsInteractionTick = System.currentTimeMillis()
        showOverlayControls = true
    }

    BackHandler {
        savePlaybackSecond(context, lecture.id, currentSecond, totalSeconds)
        if (isScreenLocked) {
            isScreenLocked = false
            registerUserInteraction()
        } else if (isFullscreenLandscape) {
            isFullscreenLandscape = false
            zoomScale = 1f
            panOffsetX = 0f
            panOffsetY = 0f
        } else {
            onDismiss()
        }
    }

    // Apply Landscape Fullscreen orientation & immersive system bars when toggled
    DisposableEffect(isFullscreenLandscape) {
        val window = activity?.window
        val insetsController = if (window != null) {
            WindowCompat.getInsetsController(window, view)
        } else {
            null
        }

        if (isFullscreenLandscape) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            insetsController?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            isScreenLocked = false
            zoomScale = 1f
            panOffsetX = 0f
            panOffsetY = 0f
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            savePlaybackSecond(context, lecture.id, currentSecond, totalSeconds)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // 1. بدء التشغيل الفوري في 0ms بدون أي انتظار للشبكة (Zero-Latency Prepare):
    //    - في وضع الأوفلاين: يفتح ملف .elgdrm مباشرة عبر ElgdrmOnTheFlyDataSource (فك تشفير أول 4096 بايت في الذاكرة فقط).
    //    - في وضع الأونلاين: يبني رابط Google Drive المباشر ويبدأ exoPlayer.prepare() فوراً في 0ms بينما يتكفل ResolvingDataSource بحقن التوكن الفوري.
    LaunchedEffect(lecture.id, lecture.videoSourceUri, isDownloadedInApp, reloadAttempt) {
        isBuffering = true
        playbackErrorMessage = null

        val trimmed = lecture.videoSourceUri.trim()
        val localVaultFile = withContext(Dispatchers.IO) {
            resolveInstantLocalVaultVideoFile(context, lecture.id, trimmed)
        }

        val resolved: ResolvedMedia3StreamTarget? = when {
            localVaultFile != null && localVaultFile.exists() && localVaultFile.length() > 0L -> {
                ResolvedMedia3StreamTarget(
                    uri = Uri.fromFile(localVaultFile),
                    cacheKey = "offline_${lecture.id}",
                    localVaultFile = localVaultFile,
                    isLocalOffline = true
                )
            }
            trimmed.startsWith("content://") -> {
                ResolvedMedia3StreamTarget(
                    uri = Uri.parse(trimmed),
                    cacheKey = "content_${lecture.id}",
                    isLocalOffline = true
                )
            }
            else -> {
                val driveFileId = GoogleDrive5TbVaultManager.extractDriveFileId(trimmed)
                if (!driveFileId.isNullOrBlank()) {
                    // لا ننتظر الشبكة هنا نهائياً! يبدأ exoPlayer.prepare() في 0ms فوراً
                    if (reloadAttempt > 0) {
                        // عند ضغط المستخدم على "إعادة المحاولة" فقط نحدث التوكن في الخلفية
                        withContext(Dispatchers.IO) {
                            GoogleDrive5TbVaultManager.fetchUploadParamsFromAppsScript(forceRefresh = true)
                                .getOrNull()
                                ?.let { params ->
                                    if (params.accessToken.isNotBlank()) {
                                        SessionPreferences.saveLastKnownDriveToken(
                                            context,
                                            params.accessToken,
                                            params.folderId
                                        )
                                    }
                                }
                        }
                    }
                    val directDriveUrl = GoogleDrive5TbVaultManager.buildDirectDriveStreamUrl(driveFileId)
                    ResolvedMedia3StreamTarget(
                        uri = Uri.parse(directDriveUrl),
                        cacheKey = "drive_${driveFileId}",
                        isLocalOffline = false
                    )
                } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                    ResolvedMedia3StreamTarget(
                        uri = Uri.parse(trimmed),
                        cacheKey = "http_${lecture.id}",
                        isLocalOffline = false
                    )
                } else {
                    null
                }
            }
        }

        streamTarget = resolved
        if (resolved == null) {
            isBuffering = false
            playbackErrorMessage = "ملف المحاضرة غير متوفر حالياً. يرجى التحقق من اتصال الإنترنت أو إعادة تنزيل المحاضرة."
        } else {
            val mediaItem = MediaItem.Builder()
                .setUri(resolved.uri)
                .setMediaId(lecture.id)
                .setCustomCacheKey(resolved.cacheKey)
                .setMimeType(MimeTypes.VIDEO_MP4)
                .build()

            val resumeSec = currentSecond.coerceIn(0, (totalSeconds - 2).coerceAtLeast(0))
            val startPositionMs = if (resumeSec > 0) resumeSec * 1000L else 0L

            exoPlayer.setMediaItem(mediaItem, startPositionMs)
            exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
            exoPlayer.playWhenReady = isPlaying
            exoPlayer.prepare()
        }
    }

    // 2. Auto-Hide Controls after 3 seconds of playback
    LaunchedEffect(showOverlayControls, isPlaying, controlsInteractionTick, isScreenLocked, isDraggingSlider) {
        if (showOverlayControls && isPlaying && !isDraggingSlider) {
            delay(3200L)
            showOverlayControls = false
        }
    }

    // Clear double-tap seek indicator after 900ms
    LaunchedEffect(seekFeedbackTick) {
        if (seekFeedbackLabel != null) {
            delay(900L)
            seekFeedbackLabel = null
        }
    }

    // 3. Sync playback progress bar & persist position to SharedPreferences
    LaunchedEffect(exoPlayer, isPlayerPrepared, isPlaying, isDraggingSlider) {
        var tickCount = 0
        while (isPlaying && isPlayerPrepared) {
            if (!isDraggingSlider) {
                val posMs = exoPlayer.currentPosition
                if (posMs >= 0L) {
                    val durMs = exoPlayer.duration
                    if (durMs != C.TIME_UNSET && durMs > 0L) {
                        totalSeconds = (durMs / 1000L).toInt().coerceAtLeast(1)
                    }
                    val sec = (posMs / 1000L).toInt().coerceIn(0, totalSeconds)
                    currentSecond = sec
                    sliderDragSecond = sec
                    tickCount++
                    if (tickCount % 8 == 0) {
                        savePlaybackSecond(context, lecture.id, sec, totalSeconds)
                    }
                }
            }
            delay(250L)
        }
    }

    // 4. Apply playback speed (1x, 1.25x, 1.5x, 2x) smoothly via Media3 ExoPlayer
    LaunchedEffect(playbackSpeed, exoPlayer) {
        runCatching {
            exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
        }
    }

    fun seekToSecond(targetSec: Int) {
        val clamped = targetSec.coerceIn(0, totalSeconds)
        currentSecond = clamped
        sliderDragSecond = clamped
        savePlaybackSecond(context, lecture.id, clamped, totalSeconds)
        runCatching {
            exoPlayer.seekTo(clamped * 1000L)
        }
    }

    fun seekByDeltaSeconds(deltaSec: Int) {
        val target = (currentSecond + deltaSec).coerceIn(0, totalSeconds)
        seekToSecond(target)
        seekFeedbackLabel = if (deltaSec > 0) "⏩ +10 ثوانٍ" else "⏪ -10 ثوانٍ"
        seekFeedbackTick = System.currentTimeMillis()
    }

    fun togglePlayPause() {
        registerUserInteraction()
        runCatching {
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
                isPlaying = false
                savePlaybackSecond(context, lecture.id, currentSecond, totalSeconds)
            } else {
                if (exoPlayer.playbackState == Player.STATE_ENDED || currentSecond >= totalSeconds) {
                    exoPlayer.seekTo(0L)
                    currentSecond = 0
                    sliderDragSecond = 0
                }
                exoPlayer.play()
                exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
                isPlaying = true
            }
        }
    }

    @Composable
    fun PlayerBottomControlsBar(modifier: Modifier = Modifier) {
        val displayedSecond = if (isDraggingSlider) sliderDragSecond else currentSecond
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xCC000000), Color(0xF2000000))
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { registerUserInteraction() }
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Seek Slider
            Slider(
                value = displayedSecond.toFloat().coerceIn(0f, totalSeconds.toFloat().coerceAtLeast(1f)),
                onValueChange = { newSec ->
                    registerUserInteraction()
                    isDraggingSlider = true
                    sliderDragSecond = newSec.toInt().coerceIn(0, totalSeconds)
                    currentSecond = sliderDragSecond
                },
                onValueChangeFinished = {
                    val target = sliderDragSecond
                    isDraggingSlider = false
                    seekToSecond(target)
                },
                valueRange = 0f..totalSeconds.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFC9A24D),
                    activeTrackColor = Color(0xFFC9A24D),
                    inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                ),
                modifier = Modifier.height(26.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Main Transport Row: Time | -10s, Play/Pause, +10s | Lock & Fullscreen Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "\u200E${formatDurationHoursMinutesSeconds(displayedSecond)} / ${formatDurationHoursMinutesSeconds(totalSeconds)}\u200E",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            registerUserInteraction()
                            seekByDeltaSeconds(-10)
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "إرجاع 10 ثوان",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Big Gold Play/Pause Button
                    IconButton(
                        onClick = { togglePlayPause() },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFC9A24D))
                            .testTag("play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                            tint = Color(0xFF0F3D2E),
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            registerUserInteraction()
                            seekByDeltaSeconds(10)
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "تقديم 10 ثوان",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isFullscreenLandscape) {
                        IconButton(
                            onClick = {
                                isScreenLocked = true
                                showOverlayControls = true
                                controlsInteractionTick = System.currentTimeMillis()
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("screen_lock_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = "قفل الشاشة",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            registerUserInteraction()
                            isFullscreenLandscape = !isFullscreenLandscape
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("toggle_fullscreen_landscape_button")
                    ) {
                        Icon(
                            imageVector = if (isFullscreenLandscape) {
                                Icons.Default.FullscreenExit
                            } else {
                                Icons.Default.Fullscreen
                            },
                            contentDescription = if (isFullscreenLandscape) "تصغير الشاشة" else "تكبير الشاشة",
                            tint = Color(0xFFC9A24D),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Clean Speed Selector Row (1x, 1.25x, 1.5x, 2x)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("playback_speed_button"),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AVAILABLE_SPEEDS.forEach { speedOption ->
                    val isSelected = playbackSpeed == speedOption
                    val label = if (speedOption == 1.0f) {
                        "1x"
                    } else if (speedOption == 2.0f) {
                        "2x"
                    } else {
                        "${speedOption}x"
                    }
                    Surface(
                        color = if (isSelected) Color(0xFFC9A24D) else Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        onClick = {
                            registerUserInteraction()
                            playbackSpeed = speedOption
                        }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) Color(0xFF0F3D2E) else Color.White.copy(alpha = 0.88f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun VideoViewportContent(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .background(Color.Black)
                .clipToBounds()
                .pointerInput(isScreenLocked) {
                    if (!isScreenLocked) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (zoomScale * zoom).coerceIn(1f, 3f)
                            zoomScale = newScale
                            if (newScale > 1.01f) {
                                val maxDx = (size.width * (newScale - 1f)) / 2f
                                val maxDy = (size.height * (newScale - 1f)) / 2f
                                panOffsetX = (panOffsetX + pan.x).coerceIn(-maxDx, maxDx)
                                panOffsetY = (panOffsetY + pan.y).coerceIn(-maxDy, maxDy)
                            } else {
                                panOffsetX = 0f
                                panOffsetY = 0f
                            }
                        }
                    }
                }
                .pointerInput(isScreenLocked, layoutDirection) {
                    detectTapGestures(
                        onTap = {
                            controlsInteractionTick = System.currentTimeMillis()
                            showOverlayControls = !showOverlayControls
                        },
                        onDoubleTap = { tapOffset ->
                            if (!isScreenLocked) {
                                val isRightHalf = tapOffset.x > (size.width / 2f)
                                val delta = if (isRightHalf) 10 else -10
                                seekByDeltaSeconds(delta)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Media3 Compose PlayerSurface (Hardware Accelerated SurfaceView)
            PlayerSurface(
                player = exoPlayer,
                surfaceType = SURFACE_TYPE_SURFACE_VIEW,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = zoomScale,
                        scaleY = zoomScale,
                        translationX = panOffsetX,
                        translationY = panOffsetY
                    )
            )

            // Dynamic Moving Watermark for content protection (0.20f alpha, shifts position every 6s)
            val watermarkAlignment = when (watermarkStep) {
                0 -> Alignment.TopStart
                1 -> Alignment.BottomEnd
                2 -> Alignment.TopEnd
                else -> Alignment.BottomStart
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 22.dp),
                contentAlignment = watermarkAlignment
            ) {
                Text(
                    text = "${userEmail.ifBlank { "Elghaliz Medicine" }}\nID: ${lecture.id.take(8)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.22f),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer(rotationZ = -10f)
                )
            }

            // Lightweight spinner only while stream connects or buffers
            if ((isBuffering || isPreparingVideo) && playbackErrorMessage == null) {
                CircularProgressIndicator(
                    color = Color(0xFFC9A24D),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(42.dp)
                )
            }

            // Error Recovery Overlay
            if (playbackErrorMessage != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .background(Color(0xD9000000), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = playbackErrorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            reloadAttempt++
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFC9A24D),
                            contentColor = Color(0xFF0F3D2E)
                        ),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إعادة المحاولة", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Double-Tap Seek Feedback Badge
            AnimatedVisibility(
                visible = seekFeedbackLabel != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = seekFeedbackLabel.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFC9A24D),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                    )
                }
            }

            // Reset Zoom (1x) badge when zoomed in
            if (zoomScale > 1.05f && !isScreenLocked) {
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(50),
                    onClick = {
                        zoomScale = 1f
                        panOffsetX = 0f
                        panOffsetY = 0f
                        registerUserInteraction()
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomOutMap,
                            contentDescription = "إعادة ضبط التكبير",
                            tint = Color(0xFFC9A24D),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${"%.1f".format(zoomScale)}x • إعادة ضبط (1x)",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Screen Lock Mode Overlay (in Fullscreen)
            if (isScreenLocked) {
                AnimatedVisibility(
                    visible = showOverlayControls,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    Surface(
                        color = Color(0xFF0F3D2E),
                        shape = RoundedCornerShape(50),
                        onClick = {
                            isScreenLocked = false
                            registerUserInteraction()
                        },
                        modifier = Modifier.testTag("screen_unlock_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "فتح قفل الشاشة",
                                tint = Color(0xFFC9A24D),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🔒 الشاشة مقفلة • اضغط للفتح",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Fullscreen Top Bar (Lecture Title + Close Fullscreen)
                if (isFullscreenLandscape) {
                    AnimatedVisibility(
                        visible = showOverlayControls,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xCC000000), Color.Transparent)
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lecture.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { isFullscreenLandscape = false }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق ملء الشاشة",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // Auto-Hiding Bottom Controls Bar inside Video Viewport
                AnimatedVisibility(
                    visible = showOverlayControls,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    PlayerBottomControlsBar()
                }
            }
        }
    }

    // UNIFIED SINGLE-TREE PLAYER LAYOUT:
    // Keeps `VideoViewportContent` in the exact same Compose tree node across Portrait <-> Fullscreen Landscape
    // so ExoPlayer & PlayerSurface are NEVER destroyed or recreated on fullscreen rotation!
    val backdropInteraction = remember { MutableInteractionSource() }
    val cleanDescription = remember(lecture.description) {
        val trimmed = lecture.description.trim()
        if (trimmed == "محاضرة مرئية مرفوعة مباشرة إلى مخزن الـ 5 تيرابايت المركزي" ||
            trimmed == "محاضرة طبية مرفوعة لتطبيق Elghaliz Medicine بالجودة الأصلية الكاملة" ||
            trimmed == "محاضرة مرئية"
        ) {
            ""
        } else {
            trimmed
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(if (isFullscreenLandscape) 100f else 90f)
            .let {
                if (isFullscreenLandscape) {
                    it.background(Color.Black).testTag("fullscreen_landscape_video_container")
                } else {
                    it
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (!isFullscreenLandscape) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.68f))
                    .clickable(
                        interactionSource = backdropInteraction,
                        indication = null,
                        onClick = {
                            savePlaybackSecond(context, lecture.id, currentSecond, totalSeconds)
                            onDismiss()
                        }
                    )
            )
        }

        ElevatedCard(
            modifier = if (isFullscreenLandscape) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .padding(horizontal = 12.dp, vertical = 16.dp)
            },
            shape = if (isFullscreenLandscape) RectangleShape else RoundedCornerShape(26.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = if (isFullscreenLandscape) Color.Black else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = if (isFullscreenLandscape) 0.dp else 8.dp
            )
        ) {
            Column(
                modifier = if (isFullscreenLandscape) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                }
            ) {
                if (!isFullscreenLandscape) {
                    // Header: Category Badge + Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFFE9F0E8),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = lecture.categoryName.ifBlank { "محاضرة طبية" },
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF1F6B52),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                savePlaybackSecond(context, lecture.id, currentSecond, totalSeconds)
                                onDismiss()
                            },
                            modifier = Modifier.testTag("close_player_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق المشغل",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Persistent Video Viewport Node (expands seamlessly to 100% fullscreen in landscape)
                Card(
                    modifier = if (isFullscreenLandscape) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    },
                    shape = if (isFullscreenLandscape) RectangleShape else RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isFullscreenLandscape) 0.dp else 6.dp
                    )
                ) {
                    VideoViewportContent(modifier = Modifier.fillMaxSize())
                }

                if (!isFullscreenLandscape) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // Lecture Title & Doctor Name
                        Text(
                            text = lecture.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "د. ${lecture.doctorName}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF1F6B52),
                            fontWeight = FontWeight.Bold
                        )

                        if (cleanDescription.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = cleanDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Prominent Gold Download Button
                        Button(
                            onClick = onDownloadInAppClick,
                            enabled = !isDownloadedInApp && !isDownloadingNow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("player_download_in_app_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFC9A24D),
                                contentColor = Color(0xFF0F3D2E)
                            )
                        ) {
                            if (isDownloadingNow) {
                                CircularProgressIndicator(
                                    color = Color(0xFF0F3D2E),
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "جاري التنزيل (${downloadProgressPercent.coerceIn(0, 99)}%)",
                                    color = Color(0xFF0F3D2E),
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = if (isDownloadedInApp) Icons.Default.CheckCircle else Icons.Default.DownloadForOffline,
                                    contentDescription = null,
                                    tint = Color(0xFF0F3D2E),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isDownloadedInApp) {
                                        "تم التنزيل للمشاهدة بدون إنترنت ✓"
                                    } else {
                                        "تنزيل داخل التطبيق"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFF0F3D2E),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (isDownloadingNow) {
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (downloadProgressPercent.coerceIn(0, 100)) / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = Color(0xFFC9A24D)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun resolveInstantLocalVaultVideoFile(
    context: Context,
    lectureId: String,
    rawUri: String
): File? {
    val trimmed = rawUri.trim()
    return runCatching {
        val vaultDir = File(context.filesDir, "offline_vault")
        val candidates = mutableListOf<File>()

        if (lectureId.isNotBlank()) {
            // 1. الأولوية القصوى لملف الخزنة المشفر .elgdrm داخل offline_vault للتشغيل الفوري عبر ElgdrmOnTheFlyDataSource
            candidates.add(File(vaultDir, "$lectureId.elgdrm"))
            candidates.add(File(vaultDir, "$lectureId.mp4"))
            candidates.add(File(File(context.filesDir, "uploaded_videos"), "$lectureId.mp4"))
            candidates.add(File(File(context.filesDir, "stream_cache"), "$lectureId.mp4"))
        }

        // 2. إذا كان الرابط المباشر يشير إلى ملف محلي على الجهاز
        val directLocal = when {
            trimmed.startsWith("file://") -> Uri.parse(trimmed).path?.let { File(it) }
            trimmed.startsWith("/") -> File(trimmed)
            else -> null
        }
        if (directLocal != null && directLocal.exists()) {
            candidates.add(directLocal)
        }

        candidates.firstOrNull { file ->
            if (!file.exists() || file.length() <= 64L) return@firstOrNull false
            val valid = EncryptedVideoVault.isValidEncryptedOrPlainVaultFile(lectureId, file) ||
                GoogleDrive5TbVaultManager.isValidPlayableVideoFile(file)
            if (!valid) {
                runCatching { file.delete() }
            }
            valid
        }
    }.getOrNull()
}

fun formatDurationHoursMinutesSeconds(totalSec: Int): String {
    val safeSec = totalSec.coerceAtLeast(0)
    val hours = safeSec / 3600
    val minutes = (safeSec % 3600) / 60
    val seconds = safeSec % 60
    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

fun formatFileSize(sizeMb: Long): String {
    return if (sizeMb >= 1024L) {
        String.format(java.util.Locale.US, "%.2f GB", sizeMb / 1024.0)
    } else {
        "$sizeMb MB"
    }
}
