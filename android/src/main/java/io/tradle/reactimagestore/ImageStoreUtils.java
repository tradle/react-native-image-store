package io.tradle.reactimagestore;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Base64OutputStream;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.nio.channels.FileChannel;

import javax.annotation.Nullable;

public class ImageStoreUtils {

  private static final String TEMP_FILE_PREFIX = "ImageStore_cache";
  private static final int BUFFER_SIZE = 8192;

  /** Compress quality of the output file. */
  private static final int COMPRESS_QUALITY = 90;

  public static String getFileExtensionForType(@Nullable String mimeType) {
    if ("image/png".equals(mimeType)) {
      return ".png";
    }
    if ("image/webp".equals(mimeType)) {
      return ".webp";
    }
    return ".jpg";
  }

  public static Bitmap.CompressFormat getCompressFormatForType(String type) {
    if ("image/png".equals(type)) {
      return Bitmap.CompressFormat.PNG;
    }
    if ("image/webp".equals(type)) {
      return Bitmap.CompressFormat.WEBP;
    }
    return Bitmap.CompressFormat.JPEG;
  }

  public static void compressBitmapToFile(Bitmap cropped, String mimeType, File tempFile, int compressionQuality)
          throws IOException {
    OutputStream out = new FileOutputStream(tempFile);
    try {
      cropped.compress(getCompressFormatForType(mimeType), compressionQuality, out);
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }

  public static void writeBytesToFile(byte[] bytes, File tempFile) throws IOException {
    FileOutputStream fos = new FileOutputStream(tempFile);
    BufferedOutputStream buf = new BufferedOutputStream(fos);
    try {
      buf.write(bytes);
    } finally {
      if (fos != null) {
        closeQuietly(fos);
      }

      if (buf != null) {
        closeQuietly(buf);
      }
    }
  }

  // https://en.wikipedia.org/wiki/List_of_file_signatures
  public static String getMimeTypeFromImageBytes(byte[] image) {
    int firstByte = image[0] & 0xFF;
    switch (firstByte) {
      case 255:
        return "image/jpeg";
      case 137:
        return "image/png";
      case 71:
        return "image/gif";
      case 73:
      case 77:
        return "image/tiff";
      case 37:
        return "application/pdf";
      case 208:
        return "application/vnd";
      case 70:
        return "text/plain";
      default:
        return "application/octet-stream";
    }
  }

  public static String getMimeTypeFromPath(String path) {
    return URLConnection.guessContentTypeFromName(new File(path).getName());
  }

  public static ImageData parseImageBase64(String imageBase64) {
    return parseImageBase64(imageBase64, null);
  }

  public static ImageData parseImageBase64(String imageBase64, @Nullable String mimeType) {
    byte[] imageBytes = Base64.decode(imageBase64, Base64.DEFAULT);
    if (mimeType == null) {
      mimeType = getMimeTypeFromImageBytes(imageBytes);
    }

    return new ImageData(imageBytes, mimeType);
  }

  /**
   * Write an image to a temporary file in the cache directory
   *
   * @param imageData image data to use to create file
   */
  private static Uri createTempFileForImageData(Context context, ImageData imageData)
          throws IOException {
    File tempFile = createTempFile(context, imageData.mimeType);
    writeBytesToFile(imageData.bytes, tempFile);
    return Uri.fromFile(tempFile);
  }

  public static Uri createTempFileForBase64Image(Context context, String base64)
          throws IOException {
    ImageData imageData = parseImageBase64(base64);
    return createTempFileForImageData(context, imageData);
  }

  public static Uri createTempFileForBase64Image(Context context, String base64, String mimeType)
          throws IOException {
    ImageData imageData = parseImageBase64(base64, mimeType);
    return createTempFileForImageData(context, imageData);
  }

  public static Uri createTempFileForImageBytes(Context context, byte[] imageBytes)
          throws IOException {
    String mimeType = getMimeTypeFromImageBytes(imageBytes);
    return createTempFileForImageBytes(context, imageBytes, mimeType);
  }

  public static Uri createTempFileForImageBytes(Context context, byte[] imageBytes, String mimeType)
          throws IOException {
    ImageData imageData = new ImageData(imageBytes, mimeType);
    return createTempFileForImageData(context, imageData);
  }

  public static Uri createTempFileForBitmap(Context context, Bitmap bitmap, String mimeType, int compressionQuality)
          throws IOException {
    File dest = createTempFile(context, mimeType);
    compressBitmapToFile(bitmap, mimeType, dest, compressionQuality);
    return Uri.fromFile(dest);
  }

  public static Uri copyFileToTempFile(Context context, Uri imageUri, String mimeType)
          throws IOException {
    File source = getFileFromUri(context, imageUri);
    File dest = createTempFile(context, mimeType);
    copyFile(source, dest);
    return Uri.fromFile(dest);
  }

  public static Uri copyFileToTempFile(Context context, String imageUriString, String mimeType)
          throws IOException {
    return copyFileToTempFile(context, Uri.parse(imageUriString), mimeType);
  }

  public static Uri copyFileToTempFile(Context context, Uri imageUri)
          throws IOException {
    return copyFileToTempFile(context, imageUri, getMimeTypeFromPath(imageUri.getPath()));
  }

  public static Uri copyFileToTempFile(Context context, String imageUriString)
          throws IOException {
    return copyFileToTempFile(context, Uri.parse(imageUriString));
  }

  public static void copyFile(File sourceFile, File destFile)
          throws IOException {
    FileChannel source = null;
    FileChannel destination = null;

    try {
      source = new FileInputStream(sourceFile).getChannel();
      destination = new FileOutputStream(destFile).getChannel();
      destination.transferFrom(source, 0, source.size());
    } finally {
      if (source != null) {
        closeQuietly(source);
      }
      if (destination != null) {
        closeQuietly(destination);
      }
    }
  }

  public static Uri getUriFromCachedFilename(Context context, String filename) throws IOException {
    File cacheDir = getCacheDir(context);
    Uri baseUri = Uri.fromFile(cacheDir);
    return Uri.withAppendedPath(baseUri, filename);
  }

  public static File getCacheDir(Context context)
      throws IOException {
  	return context.getCacheDir();
//    File externalCacheDir = context.getExternalCacheDir();
//    File internalCacheDir = context.getCacheDir();
//    File cacheDir;
//    if (externalCacheDir == null && internalCacheDir == null) {
//      throw new IOException("No cache directory available");
//    }
//    if (externalCacheDir == null) {
//      cacheDir = internalCacheDir;
//    }
//    else if (internalCacheDir == null) {
//      cacheDir = externalCacheDir;
//    } else {
//      cacheDir = externalCacheDir.getFreeSpace() > internalCacheDir.getFreeSpace() ?
//              externalCacheDir : internalCacheDir;
//    }
//
//    return cacheDir;
  }

  /**
   * Create a temporary file in the cache directory on either internal or external storage,
   * whichever is available and has more free space.
   *
   * @param mimeType the MIME type of the file to create (image/*)
   */
  public static File createTempFile(Context context, @Nullable String mimeType)
          throws IOException {
    File cacheDir = getCacheDir(context);
    return File.createTempFile(TEMP_FILE_PREFIX, getFileExtensionForType(mimeType), cacheDir);
  }

  public static @Nullable File getFileFromUri(Context context, Uri uri) {
    if (uri.getScheme().equals("file")) {
      return new File(uri.getPath());
    } else if (uri.getScheme().equals("content")) {
      Cursor cursor = context.getContentResolver()
              .query(uri, new String[] { MediaStore.MediaColumns.DATA }, null, null, null);
      if (cursor != null) {
        try {
          if (cursor.moveToFirst()) {
            String path = cursor.getString(0);
            if (!TextUtils.isEmpty(path)) {
              return new File(path);
            }
          }
        } finally {
          cursor.close();
        }
      }
    }

    return null;
  }

  public static ImageData getImageData(Context context, String uriString) throws IOException {
    Uri uri = Uri.parse(uriString);
    ContentResolver contentResolver = context.getContentResolver();
    InputStream is = contentResolver.openInputStream(uri);
    try {
      byte[] bytes = convertInputStreamToBytes(is);
      return new ImageData(bytes, getMimeTypeFromImageBytes(bytes));
    } finally {
      closeQuietly(is);
    }
  }

  public static String getImageBase64(Context context, String uriString) throws IOException {
    Uri uri = Uri.parse(uriString);
    ContentResolver contentResolver = context.getContentResolver();
    InputStream is = contentResolver.openInputStream(uri);
    try {
      return convertInputStreamToBase64OutputStream(is).toString();
    } finally {
      closeQuietly(is);
    }
  }

//  public static InputStream getInputStream(Context context, String uri) throws IOException {
//    return context.getContentResolver().openInputStream(Uri.parse(uri));
//  }

  public static byte[] convertInputStreamToBytes(InputStream is) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    int nRead;
    byte[] data = new byte[BUFFER_SIZE];
    while ((nRead = is.read(data, 0, data.length)) != -1) {
      buffer.write(data, 0, nRead);
    }

    return buffer.toByteArray();
  }

  public static String convertInputStreamToBase64OutputStream(InputStream is) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Base64OutputStream b64os = new Base64OutputStream(baos, Base64.NO_WRAP);
    byte[] buffer = new byte[BUFFER_SIZE];
    int bytesRead;
    try {
      while ((bytesRead = is.read(buffer)) > -1) {
        b64os.write(buffer, 0, bytesRead);
      }
    } finally {
      closeQuietly(b64os); // this also closes baos and flushes the final content to it
    }
    return baos.toString();
  }

  public static boolean isTmpImageFilename(String filename) {
    return filename.startsWith(ImageStoreUtils.TEMP_FILE_PREFIX);
  }

  protected static void closeQuietly(Closeable closeable) {
    try {
      closeable.close();
    } catch (IOException e) {
      // shhh
    }
  }

}
