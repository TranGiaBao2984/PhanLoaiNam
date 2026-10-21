# Ứng dụng Phân loại Nấm

Ứng dụng Android sử dụng TensorFlow Lite để nhận dạng và phân loại các loài nấm từ hình ảnh, cung cấp thông tin chi tiết bằng tiếng Việt.

## Giới thiệu

Đây là một dự án ứng dụng di động được xây dựng trên nền tảng Android, có khả năng phân tích hình ảnh nấm do người dùng cung cấp (thông qua camera hoặc thư viện ảnh) và đưa ra dự đoán về loài nấm đó. Ứng dụng sau đó hiển thị các thông tin hữu ích được trích xuất từ file `mushroom_vi.json`, bao gồm tên thông thường, tên khoa học, mức độ an toàn, mô tả, môi trường sống, và các triệu chứng ngộ độc (nếu có).

## Tính năng

*   **Nhận dạng Nấm:** Sử dụng camera hoặc chọn ảnh từ thư viện để phân loại nấm.
*   **Thông tin chi tiết:** Cung cấp thông tin đầy đủ về nấm đã nhận dạng:
    *   Tên khoa học và tên tiếng Việt.
    *   Mức độ độc tính (An toàn, Độc, Không xác định) kèm ghi chú.
    *   Mô tả đặc điểm nhận dạng.
    *   Môi trường sống và hình ảnh mẫu.
*   **Lịch sử Nhận dạng:** Xem lại các kết quả phân loại trước đó.
*   **Giao diện Sáng/Tối:** Hỗ trợ chuyển đổi giữa hai chế độ giao diện để phù hợp với sở thích người dùng.

## Công nghệ sử dụng

*   **Ngôn ngữ:** Kotlin
*   **Kiến trúc:** Dựa trên các thành phần của Android Jetpack.
*   **Machine Learning:** TensorFlow Lite để thực thi mô hình phân loại nấm trên thiết bị.
*   **UI:** Material Design Components.
*   **Camera:** CameraX.

## Cài đặt và Chạy dự án

1.  Mở dự án bằng phiên bản Android Studio mới nhất.
2.  Build và chạy ứng dụng trên máy ảo hoặc thiết bị thật.

## Nguồn dữ liệu

Thông tin chi tiết về các loài nấm được tham khảo và tổng hợp từ các nguồn:
*   Mushroom Observer (ME)
*   iNaturalist (iNat)
*   Global Biodiversity Information Facility (GBIF)
