package com.example.data.model

import android.content.Context
import android.provider.Settings
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class SavedSessionInfo(
    val email: String,
    val displayName: String,
    val roleMode: UserRoleMode,
    val autoSignInEnabled: Boolean
)

data class VerifiedAccountState(
    val uid: String,
    val email: String,
    val isVerifiedAdmin: Boolean,
    val isPrimaryAdmin: Boolean,
    val approvalStatus: String
)

data class TimestampedLectureNote(
    val id: String,
    val second: Int,
    val text: String,
    val createdAtMillis: Long
)

object SessionPreferences {
    private const val PREFS_NAME = "elghaliz_session_prefs"
    private const val PLAYBACK_POSITIONS_PREFS = "elghaliz_lecture_playback_positions"
    private const val KEY_LAST_EMAIL = "last_email"
    private const val KEY_LAST_DISPLAY_NAME = "last_display_name"
    private const val KEY_LAST_ROLE = "last_role"
    private const val KEY_AUTO_SIGN_IN = "auto_sign_in_enabled"
    private const val KEY_DARK_MODE = "dark_mode_enabled"
    private const val KEY_WIFI_ONLY_DOWNLOAD = "wifi_only_download"
    private const val KEY_AUTO_DELETE_WATCHED = "auto_delete_watched"
    private const val KEY_FONT_SIZE_SCALE = "font_size_scale"
    private const val KEY_APP_LANGUAGE = "app_language"

    fun isDarkMode(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }

    fun saveDarkMode(context: Context, isDark: Boolean) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_DARK_MODE, isDark)
            .apply()
    }

    fun isWifiOnlyDownload(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_WIFI_ONLY_DOWNLOAD, true)
    }

    fun saveWifiOnlyDownload(context: Context, enabled: Boolean) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_WIFI_ONLY_DOWNLOAD, enabled).apply()
    }

    fun isAutoDeleteWatched(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_DELETE_WATCHED, false)
    }

    fun saveAutoDeleteWatched(context: Context, enabled: Boolean) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_DELETE_WATCHED, enabled).apply()
    }

    fun getFontSizeScale(context: Context): Float {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_FONT_SIZE_SCALE, 1.0f).coerceIn(0.9f, 1.25f)
    }

    fun saveFontSizeScale(context: Context, scale: Float) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(KEY_FONT_SIZE_SCALE, scale.coerceIn(0.9f, 1.25f)).apply()
    }

    fun getAppLanguage(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_APP_LANGUAGE, "ar") ?: "ar"
    }

    fun saveAppLanguage(context: Context, lang: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_APP_LANGUAGE, lang).apply()
    }

    fun getCurrentDeviceId(context: Context): String {
        return runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "ELG_DEV_${android.os.Build.MODEL.take(8)}"
    }

    private const val ENCRYPTED_PREFIX = "ENC1:"

    private fun deriveLocalCipherKeyBytes(context: Context): ByteArray {
        val deviceId = getCurrentDeviceId(context)
        val seed = "ElghalizLocalPrefsCipher_v1|${context.packageName}|$deviceId"
        return java.security.MessageDigest.getInstance("SHA-256")
            .digest(seed.toByteArray(Charsets.UTF_8))
    }

    private fun encryptSensitivePref(context: Context, plainText: String): String {
        if (plainText.isBlank()) return ""
        val keyBytes = deriveLocalCipherKeyBytes(context)
        val inputBytes = plainText.toByteArray(Charsets.UTF_8)
        val xored = ByteArray(inputBytes.size)
        for (i in inputBytes.indices) {
            val k = keyBytes[i % keyBytes.size].toInt() xor ((i * 31 + 17) and 0xFF)
            xored[i] = (inputBytes[i].toInt() xor k).toByte()
        }
        val b64 = android.util.Base64.encodeToString(xored, android.util.Base64.NO_WRAP)
        return "$ENCRYPTED_PREFIX$b64"
    }

    private fun decryptSensitivePref(context: Context, storedValue: String?): String? {
        if (storedValue.isNullOrBlank()) return null
        if (!storedValue.startsWith(ENCRYPTED_PREFIX)) {
            return storedValue
        }
        return runCatching {
            val b64 = storedValue.removePrefix(ENCRYPTED_PREFIX)
            val xored = android.util.Base64.decode(b64, android.util.Base64.NO_WRAP)
            val keyBytes = deriveLocalCipherKeyBytes(context)
            val plainBytes = ByteArray(xored.size)
            for (i in xored.indices) {
                val k = keyBytes[i % keyBytes.size].toInt() xor ((i * 31 + 17) and 0xFF)
                plainBytes[i] = (xored[i].toInt() xor k).toByte()
            }
            String(plainBytes, Charsets.UTF_8)
        }.getOrNull()
    }

    fun registerAndGetActiveDevices(context: Context, uid: String): List<String> {
        if (uid.isBlank()) return listOf("Android_Primary")
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val deviceId = getCurrentDeviceId(context)

        val raw = prefs.getString("registered_devices_$uid", null)
        val devices = mutableListOf<String>()
        if (!raw.isNullOrBlank()) {
            runCatching {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val d = arr.optString(i)
                    if (d.isNotBlank() && !devices.contains(d)) {
                        devices.add(d)
                    }
                }
            }
        }
        if (devices.size < 1) {
            devices.add(deviceId)
            val newArr = JSONArray()
            devices.take(1).forEach { newArr.put(it) }
            prefs.edit().putString("registered_devices_$uid", newArr.toString()).apply()
        }
        return devices.take(1).ifEmpty { listOf(deviceId) }
    }

    fun syncRegisteredDevicesFromServer(context: Context, uid: String, serverDevices: List<String>) {
        if (uid.isBlank()) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        serverDevices.distinct().take(1).forEach { arr.put(it) }
        prefs.edit().putString("registered_devices_$uid", arr.toString()).apply()
    }

    fun resetRegisteredDevices(context: Context, uid: String): List<String> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val deviceId = getCurrentDeviceId(context)
        val arr = JSONArray().apply { put(deviceId) }
        prefs.edit().putString("registered_devices_$uid", arr.toString()).apply()
        return listOf(deviceId)
    }

    fun getLecturePlaybackSecond(context: Context, lectureId: String): Int {
        if (lectureId.isBlank()) return 0
        val prefs = context.applicationContext.getSharedPreferences(PLAYBACK_POSITIONS_PREFS, Context.MODE_PRIVATE)
        return prefs.getInt("pos_$lectureId", 0).coerceAtLeast(0)
    }

    fun isLectureCompleted(context: Context, lectureId: String): Boolean {
        if (lectureId.isBlank()) return false
        val prefs = context.applicationContext.getSharedPreferences(PLAYBACK_POSITIONS_PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean("completed_$lectureId", false)
    }

    fun saveLecturePlaybackSecond(context: Context, lectureId: String, second: Int, totalSeconds: Int): Boolean {
        if (lectureId.isBlank()) return false
        val prefs = context.applicationContext.getSharedPreferences(PLAYBACK_POSITIONS_PREFS, Context.MODE_PRIVATE)
        val isNowCompleted = totalSeconds > 10 && second >= (totalSeconds - 5)
        val safeSec = if (isNowCompleted) 0 else second.coerceAtLeast(0)
        val editor = prefs.edit().putInt("pos_$lectureId", safeSec)
        if (isNowCompleted) {
            editor.putBoolean("completed_$lectureId", true)
        }
        editor.apply()
        return isNowCompleted
    }

    fun markLectureCompleted(context: Context, lectureId: String) {
        if (lectureId.isBlank()) return
        val prefs = context.applicationContext.getSharedPreferences(PLAYBACK_POSITIONS_PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("pos_$lectureId", 0)
            .putBoolean("completed_$lectureId", true)
            .apply()
    }

    fun getAllPlaybackPositionsAndCompletions(context: Context): Pair<Map<String, Int>, Set<String>> {
        val prefs = context.applicationContext.getSharedPreferences(PLAYBACK_POSITIONS_PREFS, Context.MODE_PRIVATE)
        val posMap = mutableMapOf<String, Int>()
        val completedSet = mutableSetOf<String>()
        for ((k, v) in prefs.all) {
            if (k.startsWith("pos_") && v is Int && v > 0) {
                posMap[k.removePrefix("pos_")] = v
            } else if (k.startsWith("completed_") && v == true) {
                completedSet.add(k.removePrefix("completed_"))
            }
        }
        return posMap to completedSet
    }

    fun getLectureNotes(context: Context, lectureId: String): List<TimestampedLectureNote> {
        if (lectureId.isBlank()) return emptyList()
        val prefs = context.applicationContext.getSharedPreferences(PLAYBACK_POSITIONS_PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString("notes_$lectureId", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            val list = mutableListOf<TimestampedLectureNote>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    TimestampedLectureNote(
                        id = obj.optString("id", "note_$i"),
                        second = obj.optInt("second", 0),
                        text = obj.optString("text", ""),
                        createdAtMillis = obj.optLong("createdAtMillis", 0L)
                    )
                )
            }
            list.sortedBy { it.second }
        }.getOrDefault(emptyList())
    }

    fun addLectureNote(context: Context, lectureId: String, second: Int, text: String): List<TimestampedLectureNote> {
        val cleanText = text.trim()
        if (lectureId.isBlank() || cleanText.isBlank()) return getLectureNotes(context, lectureId)
        val current = getLectureNotes(context, lectureId).toMutableList()
        current.add(
            TimestampedLectureNote(
                id = "note_${System.currentTimeMillis()}",
                second = second.coerceAtLeast(0),
                text = cleanText,
                createdAtMillis = System.currentTimeMillis()
            )
        )
        val sorted = current.sortedBy { it.second }
        saveLectureNotesInternal(context, lectureId, sorted)
        return sorted
    }

    fun deleteLectureNote(context: Context, lectureId: String, noteId: String): List<TimestampedLectureNote> {
        val updated = getLectureNotes(context, lectureId).filterNot { it.id == noteId }
        saveLectureNotesInternal(context, lectureId, updated)
        return updated
    }

    private fun saveLectureNotesInternal(context: Context, lectureId: String, notes: List<TimestampedLectureNote>) {
        val prefs = context.applicationContext.getSharedPreferences(PLAYBACK_POSITIONS_PREFS, Context.MODE_PRIVATE)
        val arr = JSONArray()
        notes.forEach { n ->
            arr.put(
                JSONObject().apply {
                    put("id", n.id)
                    put("second", n.second)
                    put("text", n.text)
                    put("createdAtMillis", n.createdAtMillis)
                }
            )
        }
        prefs.edit().putString("notes_$lectureId", arr.toString()).apply()
    }

    /**
     * يحسب المساحة المستخدمة بالميجابايت على خيط الخلفية (Dispatchers.IO) حصراً
     */
    fun calculateUsedVaultStorageMbOnIo(context: Context): Long {
        return runCatching {
            val vaultDir = File(context.filesDir, "offline_vault")
            val uploadedDir = File(context.filesDir, "uploaded_videos")
            var bytes = 0L
            if (vaultDir.exists()) {
                vaultDir.listFiles()?.forEach { bytes += it.length() }
            }
            if (uploadedDir.exists()) {
                uploadedDir.listFiles()?.forEach { bytes += it.length() }
            }
            (bytes / (1024L * 1024L)).coerceAtLeast(0L)
        }.getOrDefault(0L)
    }

    fun saveSession(
        context: Context,
        email: String,
        displayName: String,
        roleMode: UserRoleMode,
        autoSignInEnabled: Boolean = true
    ) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (email.isNotBlank()) {
                putString(KEY_LAST_EMAIL, email.trim().lowercase())
            }
            if (displayName.isNotBlank()) {
                putString(KEY_LAST_DISPLAY_NAME, displayName.trim())
            }
            putString(KEY_LAST_ROLE, roleMode.name)
            putBoolean(KEY_AUTO_SIGN_IN, autoSignInEnabled)
            apply()
        }
    }

    fun saveRoleOnly(context: Context, roleMode: UserRoleMode) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_LAST_ROLE, roleMode.name)
            .apply()
    }

    fun markExplicitSignOut(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_AUTO_SIGN_IN, false)
            .apply()
    }

    fun getSavedSession(context: Context): SavedSessionInfo {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val email = prefs.getString(KEY_LAST_EMAIL, "") ?: ""
        val displayName = prefs.getString(KEY_LAST_DISPLAY_NAME, "") ?: ""
        val roleStr = prefs.getString(KEY_LAST_ROLE, UserRoleMode.ADMIN.name) ?: UserRoleMode.ADMIN.name
        val role = runCatching { UserRoleMode.valueOf(roleStr) }.getOrDefault(UserRoleMode.ADMIN)
        val autoSignIn = prefs.getBoolean(KEY_AUTO_SIGN_IN, true)
        return SavedSessionInfo(
            email = email,
            displayName = displayName,
            roleMode = role,
            autoSignInEnabled = autoSignIn
        )
    }

    fun saveVerifiedAccountState(
        context: Context,
        uid: String,
        email: String,
        isVerifiedAdmin: Boolean,
        isPrimaryAdmin: Boolean,
        approvalStatus: String
    ) {
        if (uid.isBlank()) return
        val cleanEmail = email.trim().lowercase()
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = JSONObject().apply {
            put("uid", uid)
            put("email", cleanEmail)
            put("isVerifiedAdmin", isVerifiedAdmin)
            put("isPrimaryAdmin", isPrimaryAdmin)
            put("approvalStatus", approvalStatus)
            put("updatedAt", System.currentTimeMillis())
        }
        val encryptedPayload = encryptSensitivePref(context, json.toString())
        prefs.edit()
            .putString("verified_account_$uid", encryptedPayload)
            .apply()
    }

    fun saveLastKnownDriveToken(context: Context, token: String, folderId: String = "") {
        if (token.isBlank()) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedToken = encryptSensitivePref(context, token.trim())
        prefs.edit()
            .putString("last_known_drive_token", encryptedToken)
            .putString("last_known_drive_folder_id", folderId.trim())
            .putLong("last_known_drive_token_timestamp", System.currentTimeMillis())
            .apply()
    }

    fun getLastKnownDriveToken(context: Context): String? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("last_known_drive_token", null) ?: return null
        return decryptSensitivePref(context, raw)?.takeIf { it.isNotBlank() }
    }

    fun getVerifiedAccountState(
        context: Context,
        uid: String,
        email: String
    ): VerifiedAccountState? {
        if (uid.isBlank()) return null
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("verified_account_$uid", null) ?: return null
        val decryptedJson = decryptSensitivePref(context, raw) ?: return null
        return runCatching {
            val json = JSONObject(decryptedJson)
            val savedUid = json.optString("uid", "")
            val savedEmail = json.optString("email", "")
            val cleanEmail = email.trim().lowercase()
            if (savedUid != uid) return null
            if (cleanEmail.isNotBlank() && savedEmail.isNotBlank() && savedEmail != cleanEmail) {
                return null
            }
            VerifiedAccountState(
                uid = savedUid,
                email = savedEmail,
                isVerifiedAdmin = json.optBoolean("isVerifiedAdmin", false),
                isPrimaryAdmin = json.optBoolean("isPrimaryAdmin", false),
                approvalStatus = json.optString("approvalStatus", "loading")
            )
        }.getOrNull()
    }

    fun saveLocalDownloadsCache(context: Context, uid: String, downloads: List<OfflineDownload>) {
        if (uid.isBlank()) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        downloads.forEach { dl ->
            arr.put(
                JSONObject().apply {
                    put("id", dl.id.ifBlank { dl.lectureId })
                    put("userId", dl.userId.ifBlank { uid })
                    put("lectureId", dl.lectureId)
                    put("title", dl.title)
                    put("description", dl.description)
                    put("categoryName", dl.categoryName)
                    put("doctorName", dl.doctorName)
                    put("internalFilePath", dl.internalFilePath)
                    put("durationSeconds", dl.durationSeconds)
                    put("fileSizeMb", dl.fileSizeMb)
                }
            )
        }
        prefs.edit().putString("offline_downloads_$uid", arr.toString()).apply()
    }

    fun upsertLocalDownloadItem(context: Context, uid: String, download: OfflineDownload) {
        if (uid.isBlank() || download.lectureId.isBlank()) return
        val existing = getLocalDownloadsCache(context, uid).toMutableList()
        existing.removeAll { it.lectureId == download.lectureId }
        existing.add(0, download)
        saveLocalDownloadsCache(context, uid, existing)
    }

    fun removeLocalDownloadItem(context: Context, uid: String, lectureId: String) {
        if (uid.isBlank() || lectureId.isBlank()) return
        val existing = getLocalDownloadsCache(context, uid).filterNot { it.lectureId == lectureId }
        saveLocalDownloadsCache(context, uid, existing)
    }

    fun getLocalDownloadsCache(context: Context, uid: String): List<OfflineDownload> {
        if (uid.isBlank()) return emptyList()
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("offline_downloads_$uid", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            val result = mutableListOf<OfflineDownload>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val lecId = obj.optString("lectureId", "")
                if (lecId.isNotBlank()) {
                    result.add(
                        OfflineDownload(
                            id = obj.optString("id", lecId),
                            userId = obj.optString("userId", uid),
                            lectureId = lecId,
                            title = obj.optString("title", "محاضرة طبية"),
                            description = obj.optString("description", ""),
                            categoryName = obj.optString("categoryName", "عام"),
                            doctorName = obj.optString("doctorName", "د. الغليظ"),
                            internalFilePath = obj.optString("internalFilePath", ""),
                            durationSeconds = obj.optLong("durationSeconds", 120L),
                            fileSizeMb = obj.optLong("fileSizeMb", 1L)
                        )
                    )
                }
            }
            result
        }.getOrDefault(emptyList())
    }

    fun saveLocalCategoriesCache(context: Context, categories: List<Category>) {
        if (categories.isEmpty()) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        categories.forEach { cat ->
            arr.put(
                JSONObject().apply {
                    put("id", cat.id)
                    put("name", cat.name)
                    put("description", cat.description)
                    put("createdByUid", cat.createdByUid)
                    put("createdByName", cat.createdByName)
                    put("visibility", cat.visibility)
                }
            )
        }
        prefs.edit().putString("cached_categories", arr.toString()).apply()
    }

    fun getLocalCategoriesCache(context: Context): List<Category> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("cached_categories", null) ?: return DefaultMedicalCategories.ALL
        return runCatching {
            val arr = JSONArray(raw)
            val list = mutableListOf<Category>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id", "")
                if (id.isNotBlank()) {
                    list.add(
                        Category(
                            id = id,
                            name = obj.optString("name", ""),
                            description = obj.optString("description", ""),
                            createdByUid = obj.optString("createdByUid", ""),
                            createdByName = obj.optString("createdByName", ""),
                            visibility = obj.optString("visibility", "published")
                        )
                    )
                }
            }
            list.ifEmpty { DefaultMedicalCategories.ALL }
        }.getOrDefault(DefaultMedicalCategories.ALL)
    }

    fun saveLocalLecturesCache(context: Context, lectures: List<Lecture>) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        lectures.forEach { lec ->
            arr.put(
                JSONObject().apply {
                    put("id", lec.id)
                    put("title", lec.title)
                    put("description", lec.description)
                    put("categoryId", lec.categoryId)
                    put("categoryName", lec.categoryName)
                    put("uploadedByUid", lec.uploadedByUid)
                    put("doctorName", lec.doctorName)
                    put("videoSourceUri", lec.videoSourceUri)
                    put("durationSeconds", lec.durationSeconds)
                    put("fileSizeMb", lec.fileSizeMb)
                    put("chunkCount", lec.chunkCount)
                    put("isRecordedMeeting", lec.isRecordedMeeting)
                    put("visibility", lec.visibility)
                }
            )
        }
        prefs.edit().putString("cached_lectures", arr.toString()).apply()
    }

    fun getLocalLecturesCache(context: Context): List<Lecture> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("cached_lectures", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            val list = mutableListOf<Lecture>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id", "")
                if (id.isNotBlank()) {
                    list.add(
                        Lecture(
                            id = id,
                            title = obj.optString("title", ""),
                            description = obj.optString("description", ""),
                            categoryId = obj.optString("categoryId", ""),
                            categoryName = obj.optString("categoryName", ""),
                            uploadedByUid = obj.optString("uploadedByUid", ""),
                            doctorName = obj.optString("doctorName", ""),
                            videoSourceUri = obj.optString("videoSourceUri", ""),
                            durationSeconds = obj.optLong("durationSeconds", 120L),
                            fileSizeMb = obj.optLong("fileSizeMb", 1L),
                            chunkCount = obj.optLong("chunkCount", 0L),
                            isRecordedMeeting = obj.optBoolean("isRecordedMeeting", false),
                            visibility = obj.optString("visibility", "published")
                        )
                    )
                }
            }
            list
        }.getOrDefault(emptyList())
    }

    fun saveLocalCourseRequestsCache(context: Context, uid: String, requests: List<CourseRequest>) {
        if (uid.isBlank()) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        requests.forEach { req ->
            arr.put(
                JSONObject().apply {
                    put("id", req.id)
                    put("userId", req.userId)
                    put("studentEmail", req.studentEmail)
                    put("studentName", req.studentName)
                    put("categoryId", req.categoryId)
                    put("categoryName", req.categoryName)
                    put("status", req.status)
                    put("scopeKey", req.scopeKey)
                }
            )
        }
        prefs.edit().putString("cached_course_reqs_$uid", arr.toString()).apply()
    }

    fun getLocalCourseRequestsCache(context: Context, uid: String): List<CourseRequest> {
        if (uid.isBlank()) return emptyList()
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("cached_course_reqs_$uid", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            val list = mutableListOf<CourseRequest>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val catId = obj.optString("categoryId", "")
                if (catId.isNotBlank()) {
                    list.add(
                        CourseRequest(
                            id = obj.optString("id", "${uid}_$catId"),
                            userId = obj.optString("userId", uid),
                            studentEmail = obj.optString("studentEmail", ""),
                            studentName = obj.optString("studentName", ""),
                            categoryId = catId,
                            categoryName = obj.optString("categoryName", ""),
                            status = obj.optString("status", "pending"),
                            scopeKey = obj.optString("scopeKey", "global")
                        )
                    )
                }
            }
            list
        }.getOrDefault(emptyList())
    }

    fun cleanDemoSessionExceptPrimaryAdmin(context: Context, primaryAdminEmail: String = "abdallahxh909@gmail.com") {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedEmail = prefs.getString(KEY_LAST_EMAIL, "")?.trim()?.lowercase().orEmpty()
        val isPrimary = savedEmail == primaryAdminEmail.trim().lowercase()
        if (!isPrimary && savedEmail.isNotBlank()) {
            prefs.edit().clear().apply()
        }
    }

    fun wipeAllData(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
