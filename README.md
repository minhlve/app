# Offline Messenger — Android prototype

Prototype Android/Kotlin cho **device-to-device + mesh + store-and-forward**, không dùng Internet, dữ liệu di động, SMS hay máy chủ trung tâm. Ứng dụng không tuyên bố vượt qua giới hạn vật lý: một tin nhắn chỉ có thể di chuyển xa hơn khi tồn tại chuỗi thiết bị relay hoặc một liên kết vô tuyến được hỗ trợ.

## Hiện có

- UI tiếng Việt để khởi động mạng mesh và hiển thị trạng thái không có tuyến.
- Runtime permissions + foreground service cho BLE/Wi-Fi lân cận theo giới hạn Android hiện đại.
- BLE scanner chỉ phát hiện service UUID của ứng dụng; không lấy tên Bluetooth làm danh tính tin cậy.
- `MeshRouter`: packet ID khử trùng lặp, TTL/hết hạn, checksum, route cache, store-and-forward và tự flush khi có peer mới.
- `CryptoEngine`: RSA keypair trong Android Keystore, AES-GCM cho payload, AES key bọc bằng khoá công khai đích và chữ ký nguồn. Relay chỉ nhận ciphertext.

## Việc cần hoàn thiện trước khi phát hành

1. Xây dựng BLE GATT hoặc Wi-Fi Direct data channel đã xác thực và triển khai `MeshTransport` cho nó.
2. Lưu `PacketStore`, contacts, trusted public keys và file chunks bằng Room; thêm quota/rate limit/block-list bền vững.
3. Thêm bắt tay khoá công khai có fingerprint/QR và signature verification trước khi chấp nhận packet.
4. Thêm receipt được ký, discovery route advertisement có sequence number, retry backoff và resume file theo bitmap chunk.
5. Kiểm thử thực địa A→B→C trên nhiều phiên bản Android; Android có thể giới hạn quét nền và Wi-Fi Direct tùy hãng.

## Chạy

Cài Android SDK Platform 35 và đặt `local.properties` (`sdk.dir=...`), sau đó chạy `gradle :app:assembleDebug`. Phần routing/crypto có thể kiểm thử unit độc lập sau khi tách Android Keystore bằng interface.
