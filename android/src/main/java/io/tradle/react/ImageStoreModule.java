
package io.tradle.react;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.facebook.react.bridge.GuardedAsyncTask;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class ImageStoreModule extends ReactContextBaseJavaModule implements JavaScriptModule {

  private final ReactApplicationContext reactContext;
  private static final String ERROR_CODE_IO = "io_error";
  private static final String ERROR_CODE_FILE_NOT_FOUND = "file_not_found";

  public ImageStoreModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    new CleanTask(getReactApplicationContext()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  @Override
  public String getName() {
    return "RNImageStore";
  }

  @Override
  public Map<String, Object> getConstants() {
    return Collections.emptyMap();
  }

  @Override
  public void onCatalystInstanceDestroy() {
    new CleanTask(getReactApplicationContext()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  /**
   * Asynchronous task that cleans up cache dirs (internal and, if available, external) of cropped
   * image files. This is run when the catalyst instance is being destroyed (i.e. app is shutting
   * down) and when the module is instantiated, to handle the case where the app crashed.
   */
  private static class CleanTask extends GuardedAsyncTask<Void, Void> {
    private final Context mContext;

    private CleanTask(ReactContext context) {
      super(context);
      mContext = context;
    }

    @Override
    protected void doInBackgroundGuarded(Void... params) {
      cleanDirectory(mContext.getCacheDir());
      File externalCacheDir = mContext.getExternalCacheDir();
      if (externalCacheDir != null) {
        cleanDirectory(externalCacheDir);
      }
    }

    private void cleanDirectory(File directory) {
      File[] toDelete = directory.listFiles(
              new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                  return ImageStoreUtils.isTmpImageFilename(filename);
                }
              });
      if (toDelete != null) {
        for (File file: toDelete) {
          file.delete();
        }
      }
    }
  }

  /**
   * Calculate the base64 representation for an image. The "tag" comes from iOS naming.
   *
   * @param uri the URI of the image, file:// or content://
   * @param promise to be resolved with the base64 string as the only argument
   */
  @ReactMethod
  public void getBase64ForTag(String uri, Promise promise) {
    new GetBase64Task(getReactApplicationContext(), uri, promise)
            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private class GetBase64Task extends GuardedAsyncTask<Void, Void> {
    private final String mUri;
    private final Promise mPromise;

    private GetBase64Task(
            ReactContext reactContext,
            String uri,
            Promise promise) {
      super(reactContext);
      mUri = uri;
      mPromise = promise;
    }

    @Override
    protected void doInBackgroundGuarded(Void... params) {
      try {
        mPromise.resolve(ImageStoreUtils.getImageBase64(getReactApplicationContext(), mUri));
      } catch (IOException e) {
        mPromise.reject(ERROR_CODE_IO, e.getMessage());
      }
    }
  }

  /**
   * Check if an image is present in the cache
   *
   * @param options "imageTag" (uri to the tmp file) and later other options
   * @param promise to be resolved with the boolean result
   */
  @ReactMethod
  public void hasImageForTag(ReadableMap options, Promise promise) {
    Uri uri = Uri.parse(options.getString("imageTag"));
    File file = new File(uri.getPath());
    promise.resolve(file.exists());
  }

  /**
   * Remove an image from the cache
   *
   * @param options "imageTag" (uri to the tmp file) and later other options
   * @param promise
   */
  @ReactMethod
  public void removeImageForTag(ReadableMap options, Promise promise) {
    Uri uri = Uri.parse(options.getString("imageTag"));
    File file = new File(uri.getPath());
    if (file.exists()) {
      file.delete();
    }

    promise.resolve(null);
  }

  /**
   * Add image to cache from base64 string
   *
   * @param options "base64" and optionally "mimeType"
   * @param promise to be resolved with the base64 string as the only argument
   */
  @ReactMethod
  public void addImageFromBase64(ReadableMap options, Promise promise) {
    String base64 = options.getString("base64");
    String mimeType = options.hasKey("mimeType") ? options.getString("mimeType") : null;
    new AddImageFromBase64Task(getReactApplicationContext(), base64, mimeType, promise)
            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private class AddImageFromBase64Task extends GuardedAsyncTask<Void, Void> {
    private final String mBase64;
    private final String mMimeType;
    private final Promise mPromise;

    private AddImageFromBase64Task(
            ReactContext reactContext,
            String base64,
            String mimeType,
            Promise promise) {
      super(reactContext);
      mBase64 = base64;
      mMimeType = mimeType;
      mPromise = promise;
    }

    @Override
    protected void doInBackgroundGuarded(Void... params) {
      try {
        Uri uri  = ImageStoreUtils.createTempFileForBase64Image(getReactApplicationContext(), mBase64, mMimeType);
        mPromise.resolve(uri.toString());
      } catch (IOException e) {
        mPromise.reject(ERROR_CODE_IO, e.getMessage());
      }
    }
  }

//  /**
//   * Add image to cache from raw image bytes
//   *
//   * @param bytes image bytes
//   * @param promise to be resolved with the base64 string as the only argument
//   */
//  @ReactMethod

  public void addImageFromBytes(byte[] bytes, Promise promise) {
    new AddImageFromBytesTask(getReactApplicationContext(), bytes, promise)
            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private class AddImageFromBytesTask extends GuardedAsyncTask<Void, Void> {
    private final byte[] mBytes;
    private final Promise mPromise;

    private AddImageFromBytesTask(
            ReactContext reactContext,
            byte[] bytes,
            Promise promise) {
      super(reactContext);
      mBytes = bytes;
      mPromise = promise;
    }

    @Override
    protected void doInBackgroundGuarded(Void... params) {
      try {
        Uri uri  = ImageStoreUtils.createTempFileForImageBytes(getReactApplicationContext(), mBytes);
        mPromise.resolve(uri.toString());
      } catch (IOException e) {
        mPromise.reject(ERROR_CODE_IO, e.getMessage());
      }
    }
  }

  /**
   * Add image to cache from raw image bytes
   *
   * @param options path and mimeType
   * @param promise to be resolved with the base64 string as the only argument
   */
  @ReactMethod
  public void addImageFromPath(ReadableMap options, Promise promise) {
    String path = options.getString("path");
    String mimeType = options.hasKey("mimeType") ? options.getString("mimeType") : ImageStoreUtils.getMimeTypeFromPath(path);
    Uri uri = Uri.fromFile(new File(path));
    new AddImageFromPath(getReactApplicationContext(), uri, mimeType, promise)
            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  public static Uri storeImageBytes(Context context, byte[] imageBytes) throws IOException {
    return ImageStoreUtils.createTempFileForImageBytes(context, imageBytes);
  }

  public static Uri storeImageBytes(Context context, byte[] imageBytes, String mimeType) throws IOException {
    return ImageStoreUtils.createTempFileForImageBytes(context, imageBytes, mimeType);
  }

  public static Uri storeImageAtUri(Context context, Uri uri, String mimeType) throws IOException {
    return ImageStoreUtils.copyFileToTempFile(context, uri, mimeType);
  }

  public static Uri storeImageAtUri(Context context, Uri uri) throws IOException {
    return ImageStoreUtils.copyFileToTempFile(context, uri);
  }

  public static byte[] getImageDataForTag(Context context, String uri) throws IOException {
    return ImageStoreUtils.getImageData(context, uri);
  }

  private class AddImageFromPath extends GuardedAsyncTask<Void, Void> {
    private final Uri mUri;
    private final String mMimeType;
    private final Promise mPromise;

    private AddImageFromPath(
            ReactContext reactContext,
            Uri uri,
            String mimeType,
            Promise promise) {
      super(reactContext);
      mUri = uri;
      mMimeType = mimeType;
      mPromise = promise;
    }

    @Override
    protected void doInBackgroundGuarded(Void... params) {
      try {
        Uri uri  = ImageStoreUtils.copyFileToTempFile(getReactApplicationContext(), mUri, mMimeType);
        mPromise.resolve(uri.toString());
      } catch (IOException e) {
        mPromise.reject(ERROR_CODE_IO, e.getMessage());
      }
    }
  }
}
