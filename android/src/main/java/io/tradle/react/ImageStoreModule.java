
package io.tradle.react;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.GuardedAsyncTask;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

public class ImageStoreModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;

  public ImageStoreModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    new CleanTask(getReactApplicationContext()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  @Override
  public String getName() {
    return "ImageStore";
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
   * @param success callback to be invoked with the base64 string as the only argument
   * @param error callback to be invoked on error (e.g. file not found, not readable etc.)
   */
  @ReactMethod
  public void getBase64ForTag(String uri, Callback success, Callback error) {
    new GetBase64Task(getReactApplicationContext(), uri, success, error)
            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private class GetBase64Task extends GuardedAsyncTask<Void, Void> {
    private final String mUri;
    private final Callback mSuccess;
    private final Callback mError;

    private GetBase64Task(
            ReactContext reactContext,
            String uri,
            Callback success,
            Callback error) {
      super(reactContext);
      mUri = uri;
      mSuccess = success;
      mError = error;
    }

    @Override
    protected void doInBackgroundGuarded(Void... params) {
      try {
        ContentResolver contentResolver = getReactApplicationContext().getContentResolver();
        Uri uri = Uri.parse(mUri);
        InputStream is = contentResolver.openInputStream(uri);
        try {
          mSuccess.invoke(ImageStoreUtils.convertInputStreamToBase64OutputStream(is));
        } catch (IOException e) {
          mError.invoke(e.getMessage());
        } finally {
          ImageStoreUtils.closeQuietly(is);
        }
      } catch (FileNotFoundException e) {
        mError.invoke(e.getMessage());
      }
    }
  }

  /**
   * Add image to cache from base64 string
   *
   * @param base64 the base64 string
   * @param success callback to be invoked with the base64 string as the only argument
   * @param error callback to be invoked on error (e.g. file not found, not readable etc.)
   */
  @ReactMethod
  public void addImageFromBase64(String base64, Callback success, Callback error) {
    new AddImageFromBase64Task(getReactApplicationContext(), base64, success, error)
            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private class AddImageFromBase64Task extends GuardedAsyncTask<Void, Void> {
    private final String mBase64;
    private final Callback mSuccess;
    private final Callback mError;

    private AddImageFromBase64Task(
            ReactContext reactContext,
            String base64,
            Callback success,
            Callback error) {
      super(reactContext);
      mBase64 = base64;
      mSuccess = success;
      mError = error;
    }

    @Override
    protected void doInBackgroundGuarded(Void... params) {
      try {
        String uri  = ImageStoreUtils.createTempFileForBase64Image(getReactApplicationContext(), mBase64);
        mSuccess.invoke(uri);
      } catch (IOException e) {
        mError.invoke(e.getMessage());
      }
    }
  }

  /**
   * Add image to cache from raw image bytes
   *
   * @param bytes image bytes
   * @param success callback to be invoked with the base64 string as the only argument
   * @param error callback to be invoked on error (e.g. file not found, not readable etc.)
   */
  @ReactMethod
  public void addImageFromBytes(byte[] bytes, Callback success, Callback error) {
    new AddImageFromBytesTask(getReactApplicationContext(), bytes, success, error)
            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private class AddImageFromBytesTask extends GuardedAsyncTask<Void, Void> {
    private final byte[] mBytes;
    private final Callback mSuccess;
    private final Callback mError;

    private AddImageFromBytesTask(
            ReactContext reactContext,
            byte[] bytes,
            Callback success,
            Callback error) {
      super(reactContext);
      mBytes = bytes;
      mSuccess = success;
      mError = error;
    }

    @Override
    protected void doInBackgroundGuarded(Void... params) {
      try {
        String uri  = ImageStoreUtils.createTempFileForImageBytes(getReactApplicationContext(), mBytes);
        mSuccess.invoke(uri);
      } catch (IOException e) {
        mError.invoke(e.getMessage());
      }
    }
  }

  /**
   * Add image to cache from raw image bytes
   *
   * @param path image path
   * @param mimeType image mime type
   * @param success callback to be invoked with the base64 string as the only argument
   * @param error callback to be invoked on error (e.g. file not found, not readable etc.)
   */
  @ReactMethod
  public void addImageFromPath(String path, String mimeType, Callback success, Callback error) {
    new AddImageFromPath(getReactApplicationContext(), path, mimeType, success, error)
            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private class AddImageFromPath extends GuardedAsyncTask<Void, Void> {
    private final String mPath;
    private final String mMimeType;
    private final Callback mSuccess;
    private final Callback mError;

    private AddImageFromPath(
            ReactContext reactContext,
            String path,
            String mimeType,
            Callback success,
            Callback error) {
      super(reactContext);
      mPath = path;
      mMimeType = mimeType;
      mSuccess = success;
      mError = error;
    }

    @Override
    protected void doInBackgroundGuarded(Void... params) {
      try {
        String uri  = ImageStoreUtils.copyFileToTempFile(getReactApplicationContext(),  mPath, mMimeType);
        mSuccess.invoke(uri);
      } catch (IOException e) {
        mError.invoke(e.getMessage());
      }
    }
  }
}