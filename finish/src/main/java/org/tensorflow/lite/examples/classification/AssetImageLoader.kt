package org.tensorflow.lite.examples.classification

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.IOException
import java.io.InputStream

/**
 * Object helper để tải ảnh từ thư mục assets của ứng dụng.
 */
object AssetImageLoader {

    /**
     * Tải Bitmap từ một đường dẫn trong thư mục assets.
     * @param context Context ứng dụng.
     * @param path Đường dẫn TƯƠNG ĐỐI bên trong assets (ví dụ: "mushrooms/agaricus_augustus.jpg").
     * @return Bitmap đã tải, hoặc null nếu có lỗi.
     */
    fun loadBitmapFromAssets(context: Context, path: String): Bitmap? {
        var inputStream: InputStream? = null
        try {
            // Mở luồng asset
            inputStream = context.assets.open(path)
            // Giải mã luồng thành Bitmap
            return BitmapFactory.decodeStream(inputStream)
        } catch (e: IOException) {
            // Ghi log nếu file không tồn tại hoặc không đọc được
            Log.e("AssetImageLoader", "Error loading image from assets: $path", e)
            return null
        } finally {
            // Luôn luôn đóng luồng
            try {
                inputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}