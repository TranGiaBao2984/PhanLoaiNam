package org.tensorflow.lite.examples.classification

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// Dùng @ServerTimestamp để Firebase tự động điền ngày giờ
// Thêm các trường rỗng để Firestore có thể tự động chuyển đổi
data class HistoryItem(
    val scientificName: String = "",
    val commonName: String = "",
    val confidence: Float = 0f,
    val sampleImage: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
)