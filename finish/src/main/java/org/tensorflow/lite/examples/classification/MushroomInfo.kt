package org.tensorflow.lite.examples.classification

/**
 * Định nghĩa cấu trúc dữ liệu cho một cây nấm,
 * khớp với các trường trong file mushroom_vi.json
 */
data class MushroomInfo(
    val scientificName: String = "",
    val commonName: String = "",
    val toxicity: String = "",
    val toxicityLabel: String = "",
    val toxicityNote: String = "",
    val description: String = "",
    val sampleImage: String = "", // Sẽ dùng cho màn hình chi tiết
    val habitat: String = "",
    val symptoms: String = "",
    val reference: String = "", // Sẽ dùng cho màn hình chi tiết
    val medicinal: String? = null
)

/**
 * Định nghĩa đối tượng gốc trong file mushroom_vi.json,
 * chứa một map (ánh xạ) từ TênKhoaHọc -> thông tin chi tiết.
 */
data class MushroomDatabase(
    val mushrooms: Map<String, MushroomInfo> = emptyMap()
)