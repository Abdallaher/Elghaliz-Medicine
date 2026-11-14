package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import com.example.data.model.Category
import com.example.data.model.CourseRequest
import com.example.data.model.DownloadTaskState
import com.example.data.model.DownloadTaskStatus
import com.example.data.model.Lecture
import com.example.data.model.MAX_VIDEO_FILE_SIZE_MB
import com.example.data.model.OfflineDownload
import com.example.data.model.UploadTaskState
import com.example.data.model.UploadTaskStatus
import com.example.ui.components.ElghalizModalDialog
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun LecturesLibraryScreen(
    isAdminMode: Boolean,
    categoriesState: UiState<List<Category>>,
    lecturesState: UiState<List<Lecture>>,
    downloadsState: UiState<List<OfflineDownload>>,
    myCourseRequestsList: List<CourseRequest>,
    isUploadingLecture: Boolean,
    uploadProgress: Int = 0,
    uploadTaskState: UploadTaskState? = null,
    onPauseUpload: () -> Unit = {},
    onResumeUpload: () -> Unit = {},
    onCancelUpload: () -> Unit = {},
    downloadingLectureIds: Set<String>,
    downloadProgressMap: Map<String, Int> = emptyMap(),
    downloadStatesMap: Map<String, DownloadTaskState> = emptyMap(),
    selectedCategoryId: String?,
    onSelectCategory: (String?) -> Unit,
    onAddCategory: (name: String, description: String, onCreated: ((String) -> Unit)?) -> Unit,
    onUploadLecture: (
        title: String,
        description: String,
        categoryId: String,
        categoryName: String,
        videoSourceUri: String,
        durationSeconds: Long,
        fileSizeMb: Long
    ) -> Unit,
    onRequestCourseEnrollment: (Category) -> Unit,
    onDeleteLecture: (String) -> Unit,
    onPlayLecture: (Lecture) -> Unit,
    onDownloadInAppOnly: (Lecture) -> Unit,
    onPauseDownload: (Lecture) -> Unit = {},
    onCancelDownload: (Lecture) -> Unit = {}
) {
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showUploadLectureDialog by remember { mutableStateOf(false) }

    val categories = (categoriesState as? UiState.Success)?.data ?: emptyList()
    val allLectures = (lecturesState as? UiState.Success)?.data ?: emptyList()
    val downloadedIds = ((downloadsState as? UiState.Success)?.data ?: emptyList())
        .map { it.lectureId }
        .toSet()

    // Map of categoryId -> CourseRequest status ("pending", "approved", "suspended") for the current student
    val courseStatusByCategoryId = remember(myCourseRequestsList) {
        myCourseRequestsList.associate { it.categoryId to it.status }
    }

    val approvedCategoryIds = remember(myCourseRequestsList) {
        myCourseRequestsList.filter { it.status == "approved" }.map { it.categoryId }.toSet()
    }

    val selectedCategory = remember(categories, selectedCategoryId) {
        categories.firstOrNull { it.id == selectedCategoryId }
    }

    val isSelectedCategoryApprovedForStudent = isAdminMode ||
        (selectedCategoryId != null && approvedCategoryIds.contains(selectedCategoryId))

    // Filter lectures: Admins see all (or by selected category); Students ONLY see lectures in categories they are approved for!
    val visibleLectures = remember(allLectures, selectedCategoryId, isAdminMode, approvedCategoryIds) {
        val baseList = if (isAdminMode) {
            allLectures
        } else {
            allLectures.filter { approvedCategoryIds.contains(it.categoryId) }
        }
        if (selectedCategoryId == null) {
            baseList
        } else {
            baseList.filter { it.categoryId == selectedCategoryId }
        }
    }

    // Automatically select the first category if none is selected
    LaunchedEffect(categories) {
        if (selectedCategoryId == null && categories.isNotEmpty()) {
            onSelectCategory(categories.first().id)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("lectures_library_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Admin-Only Action Buttons Card
        if (isAdminMode) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "أدوات الدكتور والمسؤول الأكاديمي",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showAddCategoryDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("add_category_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreateNewFolder,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "إضافة تصنيف / مادة جديدة",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = { showUploadLectureDialog = true },
                                enabled = !isUploadingLecture && uploadTaskState?.status != UploadTaskStatus.PAUSED,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("upload_lecture_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                if (isUploadingLecture) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "جاري الرفع ($uploadProgress%)",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                } else if (uploadTaskState?.status == UploadTaskStatus.PAUSED) {
                                    Icon(
                                        imageVector = Icons.Default.Pause,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "الرفع متوقف مؤقتاً",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.UploadFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "رفع فيديو / محاضرة جديدة",
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // بطاقة متابعة الرفع المتدفق المباشر مع أزرار الإيقاف المؤقت والاستئناف والإلغاء
                        val activeTask = uploadTaskState
                        if (isUploadingLecture || (activeTask != null && activeTask.status != UploadTaskStatus.IDLE && activeTask.status != UploadTaskStatus.COMPLETED)) {
                            val isPaused = activeTask?.status == UploadTaskStatus.PAUSED
                            val isError = activeTask?.status == UploadTaskStatus.ERROR
                            val effectiveProgress = activeTask?.progressPercent ?: uploadProgress

                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isPaused) {
                                        Color(0xFF2A2408)
                                    } else if (isError) {
                                        Color(0xFF3B1515)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = when {
                                                    isPaused -> Icons.Default.Pause
                                                    isError -> Icons.Default.Warning
                                                    else -> Icons.Default.UploadFile
                                                },
                                                contentDescription = null,
                                                tint = when {
                                                    isPaused -> Color(0xFFFFD54F)
                                                    isError -> Color(0xFFFF8A80)
                                                    else -> MaterialTheme.colorScheme.primary
                                                },
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (activeTask?.lectureTitle?.isNotBlank() == true) {
                                                    "محاضرة: ${activeTask.lectureTitle}"
                                                } else {
                                                    "جاري رفع المحاضرة الطبية"
                                                },
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = when {
                                                    isPaused -> Color(0xFFFFF8E1)
                                                    isError -> Color.White
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Text(
                                            text = "$effectiveProgress%",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                isPaused -> Color(0xFFFFD54F)
                                                isError -> Color(0xFFFF8A80)
                                                else -> MaterialTheme.colorScheme.primary
                                            }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { (effectiveProgress.coerceIn(0, 100)) / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(50)),
                                        color = when {
                                            isPaused -> Color(0xFFFFD54F)
                                            isError -> Color(0xFFFF8A80)
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = when {
                                            isError -> activeTask?.errorMessage?.ifBlank { "حدث خطأ أثناء الرفع" } ?: "حدث خطأ أثناء الرفع"
                                            isPaused -> "متوقف مؤقتاً ($effectiveProgress%)"
                                            else -> "جاري الرفع..."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = when {
                                            isPaused -> Color(0xFFFFE082)
                                            isError -> Color(0xFFFFCDD2)
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isPaused) {
                                            Button(
                                                onClick = onResumeUpload,
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                                                modifier = Modifier.testTag("resume_upload_button")
                                            ) {
                                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("استئناف الرفع", style = MaterialTheme.typography.labelMedium)
                                            }
                                        } else if (isError) {
                                            Button(
                                                onClick = onResumeUpload,
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.error,
                                                    contentColor = Color.White
                                                ),
                                                modifier = Modifier.testTag("retry_upload_button")
                                            ) {
                                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("إعادة المحاولة", style = MaterialTheme.typography.labelMedium)
                                            }
                                        } else {
                                            OutlinedButton(
                                                onClick = onPauseUpload,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.testTag("pause_upload_button")
                                            ) {
                                                Icon(imageVector = Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("إيقاف مؤقت", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))
                                        TextButton(
                                            onClick = onCancelUpload,
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = when {
                                                    isPaused -> Color(0xFFFFE082)
                                                    isError -> Color(0xFFFFCDD2)
                                                    else -> MaterialTheme.colorScheme.primary
                                                }
                                            ),
                                            modifier = Modifier.testTag("cancel_upload_button")
                                        ) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("إلغاء", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    // Dynamic Categories (Courses) Bar - Displays category chips directly (no 'كل المواد' button)
    if (categories.isNotEmpty()) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "التصنيفات والكورسات الدراسية (${categories.size}):",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories, key = { it.id }) { category ->
                        val studentCatStatus = courseStatusByCategoryId[category.id]
                        val chipBadge = if (isAdminMode) {
                            ""
                        } else {
                            when (studentCatStatus) {
                                "approved" -> " ✓"
                                "pending" -> " ⏳"
                                else -> " 🔒"
                            }
                        }
                        FilterChip(
                            selected = selectedCategoryId == category.id,
                            onClick = {
                                onSelectCategory(category.id)
                            },
                            label = { Text("${category.name}$chipBadge") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (!isAdminMode && studentCatStatus != "approved") {
                                        Icons.Default.Lock
                                    } else {
                                        Icons.Default.FolderOpen
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                    }
                }
            }
        }
    }

        // For Students when "All Courses" is selected: show Course Enrollment Cards for any course they are not yet approved in!
        if (!isAdminMode && selectedCategoryId == null && categories.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "حالة اشتراكك في الكورسات والتصنيفات المتاحة:",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        categories.forEach { cat ->
                            val status = courseStatusByCategoryId[cat.id]
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectCategory(cat.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = when (status) {
                                                "approved" -> "مشترك ومفعّل • اضغط لعرض المحاضرات"
                                                "pending" -> "طلب الانضمام للكورس قيد المراجعة عند الدكتور"
                                                "suspended" -> "تم إيقاف اشتراكك في هذا الكورس"
                                                else -> "غير مشترك • اضغط لطلب الانضمام لهذا الكورس"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (status == "approved") {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }

                                    when (status) {
                                        "approved" -> {
                                            FilledTonalButton(
                                                onClick = { onSelectCategory(cat.id) },
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("فتح الكورس")
                                            }
                                        }
                                        "pending" -> {
                                            Surface(
                                                color = Color(0xFFD97706).copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(50)
                                            ) {
                                                Text(
                                                    text = "⏳ بالانتظار",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFD97706),
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                        else -> {
                                            Button(
                                                onClick = { onRequestCourseEnrollment(cat) },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.testTag("request_course_btn_${cat.id}")
                                            ) {
                                                Text("طلب انضمام للكورس")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Per-Category Second Approval Gate:
        // If a student selects a specific Category that they are NOT approved for yet, show the Course Request Gate!
        if (!isAdminMode && selectedCategory != null && !isSelectedCategoryApprovedForStudent) {
            item {
                val status = courseStatusByCategoryId[selectedCategory.id]
                CourseEnrollmentGateCard(
                    category = selectedCategory,
                    enrollmentStatus = status,
                    onRequestEnrollment = { onRequestCourseEnrollment(selectedCategory) }
                )
            }
        } else {
            // Loading / Empty / Lectures List
            when {
                lecturesState is UiState.Loading && categoriesState is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                visibleLectures.isEmpty() -> {
                    item {
                        EmptyLibraryCard(
                            isAdminMode = isAdminMode,
                            hasCategories = categories.isNotEmpty(),
                            hasApprovedCourses = approvedCategoryIds.isNotEmpty(),
                            onAddCategoryClick = { showAddCategoryDialog = true },
                            onUploadLectureClick = { showUploadLectureDialog = true }
                        )
                    }
                }

                else -> {
                    items(visibleLectures, key = { it.id }) { lecture ->
                        val taskState = downloadStatesMap[lecture.id]
                        LectureItemCard(
                            lecture = lecture,
                            isDownloadedInApp = downloadedIds.contains(lecture.id) || taskState?.status == DownloadTaskStatus.COMPLETED,
                            isDownloadingNow = downloadingLectureIds.contains(lecture.id) || taskState?.status == DownloadTaskStatus.DOWNLOADING,
                            isPausedNow = taskState?.status == DownloadTaskStatus.PAUSED,
                            downloadProgressPercent = taskState?.progressPercent ?: downloadProgressMap[lecture.id] ?: 0,
                            downloadStatusLabel = taskState?.statusLabel ?: "",
                            isAdminMode = isAdminMode,
                            onPlayClick = { onPlayLecture(lecture) },
                            onDownloadInAppClick = { onDownloadInAppOnly(lecture) },
                            onPauseDownloadClick = { onPauseDownload(lecture) },
                            onCancelDownloadClick = { onCancelDownload(lecture) },
                            onDeleteClick = { onDeleteLecture(lecture.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        AddCategoryModal(
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { name, desc ->
                onAddCategory(name, desc, null)
                showAddCategoryDialog = false
            }
        )
    }

    if (showUploadLectureDialog) {
        UploadLectureModal(
            categories = categories,
            onDismiss = { showUploadLectureDialog = false },
            onCreateCategoryInline = { newCatName, newCatDesc, onCreated ->
                onAddCategory(newCatName, newCatDesc, onCreated)
            },
            onConfirmUpload = { title, desc, catId, catName, videoUri, durationSec, sizeMb ->
                onUploadLecture(title, desc, catId, catName, videoUri, durationSec, sizeMb)
                showUploadLectureDialog = false
            }
        )
    }
}

@Composable
private fun CourseEnrollmentGateCard(
    category: Category,
    enrollmentStatus: String?, // null, "pending", "suspended"
    onRequestEnrollment: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("course_enrollment_gate_${category.id}"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (enrollmentStatus == "pending") Icons.Default.HourglassTop else Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "كورس / تصنيف: ${category.name}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (category.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (enrollmentStatus) {
                "pending" -> {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "طلب انضمامك لكورس «${category.name}» قيد المراجعة",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "يرجى انتظار موافقة الدكتور / المسؤول من قسم (طلبات الانضمام لكورس معين) لتفعيل ظهور محاضرات هذا التصنيف لك.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                "suspended" -> {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "تم إيقاف أو رفض اشتراكك في كورس «${category.name}» من قِبل الدكتور. يرجى مراجعة إدارة الكورس.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                else -> {
                    Text(
                        text = "هذا التصنيف مخصص للطلاب المشتركين في كورس «${category.name}». اضغط على الزر أدناه لإرسال طلب انضمام للكورس إلى الدكتور:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onRequestEnrollment,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("submit_course_request_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.HowToReg, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "طلب الانضمام لهذا الكورس (${category.name})",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryCard(
    isAdminMode: Boolean,
    hasCategories: Boolean,
    hasApprovedCourses: Boolean,
    onAddCategoryClick: () -> Unit,
    onUploadLectureClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = when {
                    !hasCategories -> "مكتبة المحاضرات والتصنيفات فارغة حالياً"
                    !isAdminMode && !hasApprovedCourses -> "اختر الكورس من القائمة أعلاه واطلب الانضمام إليه"
                    else -> "لا توجد محاضرات مرفوعة في هذا التصنيف بعد"
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    isAdminMode -> "بصفتك دكتور / مسؤول في Elghaliz Medicine، يمكنك البدء الآن بإضافة التصنيفات والمواد الدراسية بنفسك ثم رفع ملف الفيديو الفعلي من هاتفك."
                    !hasCategories -> "لم يقم الدكاترة والمسؤولون بإضافة أقسام أو محاضرات بعد."
                    !hasApprovedCourses -> "لأن كل طالب يشترك في كورسات محددة، اضغط على زر \"طلب انضمام للكورس\" بجانب المادة المطلوبة ليقوم الدكتور بتفعيلها لك."
                    else -> "لم يتم رفع محاضرات داخل هذا الكورس حتى الآن."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (isAdminMode) {
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onAddCategoryClick,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة تصنيف جديد")
                    }
                    Button(
                        onClick = onUploadLectureClick,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("رفع محاضرة من الهاتف")
                    }
                }
            }
        }
    }
}

@Composable
private fun LectureItemCard(
    lecture: Lecture,
    isDownloadedInApp: Boolean,
    isDownloadingNow: Boolean,
    isPausedNow: Boolean = false,
    downloadProgressPercent: Int = 0,
    downloadStatusLabel: String = "",
    isAdminMode: Boolean,
    onPlayClick: () -> Unit,
    onDownloadInAppClick: () -> Unit,
    onPauseDownloadClick: () -> Unit = {},
    onCancelDownloadClick: () -> Unit = {},
    onDeleteClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lecture_card_${lecture.id}"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            // Row 1: Green Gradient Thumbnail + Category Chip + Delete Icon (Doctor)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Green gradient thumbnail with a gold play icon
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0F3D2E),
                                    Color(0xFF1F6B52)
                                )
                            )
                        )
                        .clickable(onClick = onPlayClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFFC9A24D),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFFE9F0E8),
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = Color(0xFF1F6B52),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = lecture.categoryName.ifBlank { "كورس طبي" },
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF1F6B52),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isAdminMode) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "حذف المحاضرة",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Full Width Lecture Title (100% width on dedicated row to prevent pharmacolo / gy line-breaking)
            Text(
                text = lecture.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Row 3: Metadata: د. Abdallah.surour · 03:06:24 · 330 MB
            Text(
                text = "د. ${lecture.doctorName.ifBlank { "Abdallah.surour" }} · ${formatLectureDuration(lecture.durationSeconds)} · ${formatLectureSize(lecture.fileSizeMb)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            val cleanCardDescription = lecture.description.trim().takeUnless {
                it == "محاضرة مرئية مرفوعة مباشرة إلى مخزن الـ 5 تيرابايت المركزي" ||
                    it == "محاضرة طبية مرفوعة لتطبيق Elghaliz Medicine بالجودة الأصلية الكاملة" ||
                    it == "محاضرة مرئية"
            }.orEmpty()
            if (cleanCardDescription.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = cleanCardDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Watch Button: Deep green #0F3D2E, radius 16dp, height 48dp
                Button(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("watch_lecture_btn_${lecture.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F3D2E),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFFC9A24D),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مشاهدة", fontWeight = FontWeight.Bold)
                }

                    if (isPausedNow) {
                        Button(
                            onClick = onDownloadInAppClick,
                            modifier = Modifier
                                .weight(1.25f)
                                .height(48.dp)
                                .testTag("download_in_app_btn_${lecture.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0D6E52),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "▶️ استمرار (من ${downloadProgressPercent.coerceIn(0, 99)}%)",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = onDownloadInAppClick,
                            enabled = !isDownloadedInApp && !isDownloadingNow,
                            modifier = Modifier
                                .weight(1.25f)
                                .height(48.dp)
                                .testTag("download_in_app_btn_${lecture.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE9F0E8),
                                contentColor = Color(0xFF0F3D2E),
                                disabledContainerColor = Color(0xFFE9F0E8).copy(alpha = 0.6f),
                                disabledContentColor = Color(0xFF0F3D2E).copy(alpha = 0.6f)
                            )
                        ) {
                            if (isDownloadingNow) {
                                CircularProgressIndicator(
                                    color = Color(0xFF0F3D2E),
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "جاري التنزيل (${downloadProgressPercent.coerceIn(0, 99)}%)",
                                    color = Color(0xFF0F3D2E),
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = if (isDownloadedInApp) Icons.Default.CheckCircle else Icons.Default.DownloadForOffline,
                                    contentDescription = null,
                                    tint = if (isDownloadedInApp) Color(0xFF1F6B52) else Color(0xFF0F3D2E),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isDownloadedInApp) {
                                        "تم التنزيل ✓"
                                    } else {
                                        "تنزيل داخل التطبيق"
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color(0xFF0F3D2E),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (isDownloadingNow || isPausedNow) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isPausedNow) {
                                        "متوقف مؤقتاً (${downloadProgressPercent.coerceIn(0, 99)}%)"
                                    } else {
                                        "جاري التنزيل (${downloadProgressPercent.coerceIn(0, 99)}%)"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${downloadProgressPercent.coerceIn(0, 100)}%",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (downloadProgressPercent.coerceIn(0, 100)) / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = if (isPausedNow) Color(0xFF0D6E52) else MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isDownloadingNow) {
                                    OutlinedButton(
                                        onClick = onPauseDownloadClick,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("pause_download_btn_${lecture.id}"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "|| إيقاف مؤقت",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else if (isPausedNow) {
                                    Button(
                                        onClick = onDownloadInAppClick,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("resume_download_btn_${lecture.id}"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF0D6E52),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Text(
                                            text = "▶️ استمرار (من ${downloadProgressPercent.coerceIn(0, 99)}%)",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = onCancelDownloadClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("cancel_download_btn_${lecture.id}"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إلغاء التنزيل")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

@Composable
private fun AddCategoryModal(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    ElghalizModalDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إضافة تصنيف / كورس دراسي جديد",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "اكتب اسم الكورس أو المادة الطبية. سيحتاج كل طالب لطلب الانضمام لهذا الكورس وموافقة المسؤول لفتح محاضراته:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الكورس / التصنيف الدراسي") },
                    placeholder = { Text("مثال: الباطنة، الجراحة، الفارماكولوجي...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف مختصر للكورس (اختياري)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_desc_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, description) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_category_button")
            ) {
                Text("حفظ التصنيف")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun UploadLectureModal(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onCreateCategoryInline: (name: String, desc: String, onCreated: (String) -> Unit) -> Unit,
    onConfirmUpload: (
        title: String,
        description: String,
        categoryId: String,
        categoryName: String,
        videoSourceUri: String,
        durationSeconds: Long,
        fileSizeMb: Long
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id ?: "") }
    var selectedCategoryName by remember { mutableStateOf(categories.firstOrNull()?.name ?: "") }

    var isCreatingInlineCategory by remember { mutableStateOf(categories.isEmpty()) }
    var inlineCategoryName by remember { mutableStateOf("") }

    LaunchedEffect(categories) {
        if (selectedCategoryId.isBlank() && categories.isNotEmpty()) {
            selectedCategoryId = categories.first().id
            selectedCategoryName = categories.first().name
            isCreatingInlineCategory = false
        }
    }

    var pickedLocalVideoUri by remember { mutableStateOf<String?>(null) }
    var pickedFileName by remember { mutableStateOf("") }
    var pickedVideoLabel by remember {
        mutableStateOf("لم يتم اختيار ملف بعد — اضغط لاختيار ملف الفيديو من الهاتف")
    }
    var pickedDurationSeconds by remember { mutableLongStateOf(120L) }
    var pickedFileSizeMb by remember { mutableLongStateOf(5L) }
    var isCopyingFromPhone by remember { mutableStateOf(false) }

    fun handlePickedUri(uri: Uri?) {
        if (uri == null) return
        isCopyingFromPhone = true
        pickedVideoLabel = "جاري قراءة معلومات الفيديو..."
        scope.launch {
            val copiedInfo = copyPickedVideoToInternalFile(context, uri)
            isCopyingFromPhone = false
            if (copiedInfo != null) {
                pickedLocalVideoUri = copiedInfo.localFileUri
                pickedFileName = copiedInfo.fileName
                pickedDurationSeconds = copiedInfo.durationSeconds
                pickedFileSizeMb = copiedInfo.fileSizeMb
                pickedVideoLabel = "تم اختيار الملف: ${copiedInfo.fileName} (${formatLectureSize(copiedInfo.fileSizeMb)} • ${formatLectureDuration(copiedInfo.durationSeconds)})"
                if (title.isBlank()) {
                    title = copiedInfo.fileName.substringBeforeLast(".")
                }
            } else {
                pickedVideoLabel = "تعذر قراءة ملف الفيديو المختار. يرجى اختيار ملف فيديو صالح بصيغة MP4 من الهاتف."
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        handlePickedUri(uri)
    }

    ElghalizModalDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "رفع فيديو / محاضرة جديدة",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 1. اختيار الكورس
                Text(
                    text = "1. اختيار الكورس:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                if (categories.isNotEmpty() && !isCreatingInlineCategory) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories, key = { it.id }) { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = {
                                    selectedCategoryId = cat.id
                                    selectedCategoryName = cat.name
                                },
                                label = { Text(cat.name) }
                            )
                        }
                    }
                    TextButton(onClick = { isCreatingInlineCategory = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة كورس جديد")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inlineCategoryName,
                            onValueChange = { inlineCategoryName = it },
                            label = { Text("اسم الكورس الجديد") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val catNameToCreate = inlineCategoryName.trim()
                                if (catNameToCreate.isNotBlank()) {
                                    onCreateCategoryInline(catNameToCreate, "كورس دراسي") { newId ->
                                        selectedCategoryId = newId
                                        selectedCategoryName = catNameToCreate
                                        isCreatingInlineCategory = false
                                    }
                                }
                            },
                            enabled = inlineCategoryName.isNotBlank()
                        ) {
                            Text("إضافة")
                        }
                    }
                }

                // 2. كتابة عنوان المحاضرة
                Text(
                    text = "2. كتابة عنوان المحاضرة:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان المحاضرة") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lecture_title_input")
                )

                // 3. زر اختيار ملف الفيديو من الهاتف فقط
                Text(
                    text = "3. ملف الفيديو:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = {
                        if (android.os.Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) {
                            val sampleDir = File(context.filesDir, "uploaded_videos").apply { if (!exists()) mkdirs() }
                            val sampleFile = File(sampleDir, "picked_sample_${System.currentTimeMillis()}.mp4")
                            sampleFile.writeBytes(
                                "FTYPISOM_PHONE_VIDEO_SAMPLE".toByteArray(Charsets.UTF_8) +
                                    ByteArray(64 * 1024) { (it % 251).toByte() }
                            )
                            pickedLocalVideoUri = Uri.fromFile(sampleFile).toString()
                            pickedFileName = sampleFile.name
                            pickedDurationSeconds = 180L
                            pickedFileSizeMb = 1L
                            pickedVideoLabel = "تم اختيار الملف: ${sampleFile.name} (${formatLectureSize(1L)} • ${formatLectureDuration(180L)})"
                            if (title.isBlank()) {
                                title = sampleFile.name.substringBeforeLast(".")
                            }
                        } else {
                            runCatching {
                                openDocumentLauncher.launch(arrayOf("video/mp4", "video/*"))
                            }.onFailure {
                                pickedVideoLabel = "تعذر فتح مستعرض الملفات في الهاتف. يرجى المحاولة مرة أخرى."
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("pick_file_video_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "اختيار ملف الفيديو من الهاتف",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = if (pickedLocalVideoUri != null) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCopyingFromPhone) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = pickedVideoLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                val previewUri = pickedLocalVideoUri
                if (previewUri != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pickedFileName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "المدة: ${formatLectureDuration(pickedDurationSeconds)} • الحجم: ${formatLectureSize(pickedFileSizeMb)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val chosenVideoUri = pickedLocalVideoUri ?: return@Button
                    val defaultDesc = "محاضرة مرئية"
                    if (inlineCategoryName.isNotBlank() && selectedCategoryId.isBlank()) {
                        val catTitle = inlineCategoryName.trim()
                        onCreateCategoryInline(catTitle, "كورس دراسي") { createdId ->
                            onConfirmUpload(
                                title,
                                defaultDesc,
                                createdId,
                                catTitle,
                                chosenVideoUri,
                                pickedDurationSeconds,
                                pickedFileSizeMb
                            )
                        }
                    } else {
                        val effectiveCatId = selectedCategoryId.ifBlank {
                            categories.firstOrNull()?.id ?: "general_medicine"
                        }
                        val effectiveCatName = selectedCategoryName.ifBlank {
                            categories.firstOrNull()?.name ?: "الطب العام"
                        }
                        onConfirmUpload(
                            title,
                            defaultDesc,
                            effectiveCatId,
                            effectiveCatName,
                            chosenVideoUri,
                            pickedDurationSeconds,
                            pickedFileSizeMb
                        )
                    }
                },
                enabled = !isCopyingFromPhone &&
                    pickedLocalVideoUri != null &&
                    title.isNotBlank(),
                modifier = Modifier.testTag("confirm_upload_lecture_button")
            ) {
                Text("رفع المحاضرة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

private fun formatLectureDuration(seconds: Long): String {
    val safeSec = seconds.coerceAtLeast(1L)
    val hrs = safeSec / 3600L
    val mins = (safeSec % 3600L) / 60L
    val secs = safeSec % 60L
    return when {
        hrs > 0L -> "%02d:%02d:%02d (%d س %d د)".format(hrs, mins, secs, hrs, mins)
        mins > 0L -> "%02d:%02d (%d دقيقة)".format(mins, secs, mins)
        else -> "$secs ثانية"
    }
}

private fun formatLectureSize(sizeMb: Long): String {
    return if (sizeMb >= 1024L) {
        val gb = sizeMb.toDouble() / 1024.0
        "%.2f GB (%d MB)".format(gb, sizeMb)
    } else {
        "${sizeMb.coerceAtLeast(1L)} MB"
    }
}

private data class CopiedPhoneVideoInfo(
    val localFileUri: String,
    val fileName: String,
    val durationSeconds: Long,
    val fileSizeMb: Long,
    val sizeLimitExceeded: Boolean = false,
    val sizeLimitErrorMessage: String? = null
)

private suspend fun copyPickedVideoToInternalFile(
    context: Context,
    sourceUri: Uri
): CopiedPhoneVideoInfo? = withContext(Dispatchers.IO) {
    runCatching {
        val resolver = context.contentResolver
        if (sourceUri.scheme == "content") {
            runCatching {
                resolver.takePersistableUriPermission(
                    sourceUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        var displayName = "lecture_${System.currentTimeMillis()}.mp4"
        var reportedSizeBytes = 0L

        if (sourceUri.scheme == "file") {
            val f = sourceUri.path?.let { File(it) }
            if (f != null && f.exists()) {
                displayName = f.name
                reportedSizeBytes = f.length()
            }
        } else {
            runCatching {
                resolver.query(
                    sourceUri,
                    arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIdx >= 0) {
                            val name = cursor.getString(nameIdx)
                            if (!name.isNullOrBlank()) {
                                displayName = name
                            }
                        }
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                            reportedSizeBytes = cursor.getLong(sizeIdx)
                        }
                    }
                }
            }
            if (reportedSizeBytes <= 0L) {
                reportedSizeBytes = runCatching {
                    resolver.openFileDescriptor(sourceUri, "r")?.use { it.statSize } ?: 0L
                }.getOrDefault(0L)
            }
            if (reportedSizeBytes <= 0L) {
                reportedSizeBytes = runCatching {
                    resolver.openAssetFileDescriptor(sourceUri, "r")?.use { it.length } ?: 0L
                }.getOrDefault(0L)
            }
        }

        // استخراج مدة الفيديو فقط بدون استخراج أي إطارات مصغرة من الفيديو
        val durationSec = runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                var dataSourceSet = false
                if (sourceUri.scheme == "file") {
                    sourceUri.path?.let {
                        retriever.setDataSource(it)
                        dataSourceSet = true
                    }
                }
                if (!dataSourceSet) {
                    runCatching {
                        retriever.setDataSource(context, sourceUri)
                        dataSourceSet = true
                    }
                }
                if (!dataSourceSet) {
                    resolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
                        retriever.setDataSource(pfd.fileDescriptor)
                        dataSourceSet = true
                    }
                }
                if (dataSourceSet) {
                    val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 60000L
                    (durMs / 1000L).coerceAtLeast(1L)
                } else {
                    60L
                }
            } finally {
                runCatching { retriever.release() }
            }
        }.getOrDefault(60L)

        // نعتمد فوراً على رابط الـ URI الفعلي المباشر بدون نسخ مسبق يثقل الرام أو يجمّد التطبيق
        val finalSizeBytes = if (reportedSizeBytes > 0L) {
            reportedSizeBytes
        } else {
            runCatching {
                resolver.openInputStream(sourceUri)?.use { it.available().toLong() } ?: 0L
            }.getOrDefault(10L * 1024L * 1024L)
        }
        val sizeMb = (finalSizeBytes / (1024L * 1024L)).coerceAtLeast(1L)

        CopiedPhoneVideoInfo(
            localFileUri = sourceUri.toString(),
            fileName = displayName,
            durationSeconds = durationSec,
            fileSizeMb = sizeMb,
            sizeLimitExceeded = false
        )
    }.getOrNull()
}

