package com.example.myapplication;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ImageUtils {

    /**
     * Convierte una Uri de imagen (de galería o archivos) en un String Base64 comprimido en JPEG.
     */
    public static String uriToBase64(ContentResolver contentResolver, Uri uri) {
        if (uri == null || contentResolver == null) return null;
        try (InputStream inputStream = contentResolver.openInputStream(uri)) {
            if (inputStream == null) return null;
            Bitmap original = BitmapFactory.decodeStream(inputStream);
            if (original == null) return null;

            return bitmapToBase64(original);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Convierte un Bitmap en un String Base64 escalado y comprimido en JPEG.
     */
    public static String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return null;

        int maxDimension = 600;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap targetBitmap = bitmap;

        if (width > maxDimension || height > maxDimension) {
            float ratio = Math.min((float) maxDimension / width, (float) maxDimension / height);
            width = Math.round(width * ratio);
            height = Math.round(height * ratio);
            targetBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        targetBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    /**
     * Carga de forma segura una imagen en un ImageView intentando primero el String Base64,
     * luego la Uri local/remota y finalmente la imagen por defecto.
     */
    public static void cargarImagen(Context context, ImageView imageView, String imgUriStr, String imgBase64Str) {
        if (imageView == null) return;

        // 1. Intentar decodificar desde Base64 (guardado directamente en Firebase)
        if (imgBase64Str != null && !imgBase64Str.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(imgBase64Str, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                if (decodedByte != null) {
                    imageView.setImageBitmap(decodedByte);
                    return;
                }
            } catch (Exception ignored) {
            }
        }

        // 2. Intentar cargar desde Uri mediante ContentResolver para evitar SecurityException
        if (imgUriStr != null && !imgUriStr.isEmpty()) {
            try {
                Uri uri = Uri.parse(imgUriStr);
                if (context != null && "content".equalsIgnoreCase(uri.getScheme())) {
                    try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap);
                            return;
                        }
                    }
                } else {
                    imageView.setImageURI(uri);
                    return;
                }
            } catch (Exception ignored) {
            }
        }

        // 3. Fallback a la imagen por defecto
        imageView.setImageResource(R.drawable.gato__1_);
    }
}
