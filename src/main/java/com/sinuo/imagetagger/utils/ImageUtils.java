package com.sinuo.imagetagger.utils;

import android.graphics.Bitmap;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

public class ImageUtils {

    /**
     * 将 Bitmap 转换为 Base64 字符串
     *
     * @param bitmap            要转换的 Bitmap 对象
     * @param compressionQuality 压缩质量 (0-100)
     * @return 转换后的 Base64 字符串
     */
    public static String bitmapToBase64(Bitmap bitmap, int compressionQuality) {
        if (bitmap == null) {
            return "";
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }
}
