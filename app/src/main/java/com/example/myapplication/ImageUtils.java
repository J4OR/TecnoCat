package com.example.myapplication;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageUtils {
    private static final ExecutorService IMAGE_EXECUTOR = Executors.newFixedThreadPool(2);
    private static final int MAX_DECODE_DIMENSION = 1000;
    private static final LruCache<String, Bitmap> IMAGE_CACHE = new LruCache<String, Bitmap>(12 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            return bitmap.getAllocationByteCount();
        }
    };

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
        Context appContext = context != null ? context.getApplicationContext() : null;
        String key = createImageKey(imgUriStr, imgBase64Str);
        imageView.setTag(R.id.image_request_tag, key);

        if (key == null) {
            imageView.setImageResource(R.drawable.ic_box);
            return;
        }

        Bitmap cachedBitmap = IMAGE_CACHE.get(key);
        if (cachedBitmap != null) {
            imageView.setImageBitmap(cachedBitmap);
            return;
        }

        imageView.setImageResource(R.drawable.ic_box);
        IMAGE_EXECUTOR.execute(() -> {
            Bitmap bitmap = decodeImage(appContext, imgUriStr, imgBase64Str);
            if (bitmap == null) return;
            IMAGE_CACHE.put(key, bitmap);
            imageView.post(() -> {
                Object currentKey = imageView.getTag(R.id.image_request_tag);
                if (key.equals(currentKey)) imageView.setImageBitmap(bitmap);
            });
        });
    }

    private static String createImageKey(String uri, String base64) {
        if (base64 != null && !base64.isEmpty()) {
            return "base64:" + base64.length() + ":" + base64.hashCode();
        }
        return uri == null || uri.isEmpty() ? null : "uri:" + uri;
    }

    private static Bitmap decodeImage(Context context, String uriText, String base64) {
        try {
            if (base64 != null && !base64.isEmpty()) {
                byte[] imageBytes = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bitmap = decodeSampled(imageBytes);
                if (bitmap != null) return bitmap;
            }
            if (uriText == null || uriText.isEmpty()) return null;

            Uri uri = Uri.parse(uriText);
            String scheme = uri.getScheme();
            if ("file".equalsIgnoreCase(scheme)) {
                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(uri.getPath(), bounds);
                BitmapFactory.Options options = samplingOptions(bounds);
                return BitmapFactory.decodeFile(uri.getPath(), options);
            }

            byte[] imageBytes;
            if (("content".equalsIgnoreCase(scheme) || "android.resource".equalsIgnoreCase(scheme))
                    && context != null) {
                try (InputStream input = context.getContentResolver().openInputStream(uri)) {
                    imageBytes = readBytes(input);
                }
            } else {
                HttpURLConnection connection = (HttpURLConnection) new URL(uriText).openConnection();
                connection.setConnectTimeout(7000);
                connection.setReadTimeout(7000);
                connection.setInstanceFollowRedirects(true);
                try (InputStream input = connection.getInputStream()) {
                    imageBytes = readBytes(input);
                } finally {
                    connection.disconnect();
                }
            }
            return decodeSampled(imageBytes);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static byte[] readBytes(InputStream input) throws Exception {
        if (input == null) return null;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        return output.toByteArray();
    }

    private static Bitmap decodeSampled(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, samplingOptions(bounds));
    }

    private static BitmapFactory.Options samplingOptions(BitmapFactory.Options bounds) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        int longestSide = Math.max(bounds.outWidth, bounds.outHeight);
        while (longestSide / options.inSampleSize > MAX_DECODE_DIMENSION) {
            options.inSampleSize *= 2;
        }
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        return options;
    }
}
