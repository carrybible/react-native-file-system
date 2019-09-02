
package com.rnfilesystem;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.SparseArray;
import android.media.MediaScannerConnection;
import android.net.Uri;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.RCTNativeAppEventEmitter;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@ReactModule(name = RNFileSystem.MODULE_NAME)
public class RNFileSystem extends ReactContextBaseJavaModule {
  static final String MODULE_NAME = "RNFileSystem";

  private static final String RNFSDocumentDirectoryPath = "RNFSDocumentDirectoryPath";
  private static final String RNFSExternalDirectoryPath = "RNFSExternalDirectoryPath";
  private static final String RNFSExternalStorageDirectoryPath = "RNFSExternalStorageDirectoryPath";
  private static final String RNFSPicturesDirectoryPath = "RNFSPicturesDirectoryPath";
  private static final String RNFSTemporaryDirectoryPath = "RNFSTemporaryDirectoryPath";
  private static final String RNFSCachesDirectoryPath = "RNFSCachesDirectoryPath";
  private static final String RNFSExternalCachesDirectoryPath = "RNFSExternalCachesDirectoryPath";
  private static final String RNFSDocumentDirectory = "RNFSDocumentDirectory";

  private static final String RNFSFileTypeRegular = "RNFSFileTypeRegular";
  private static final String RNFSFileTypeDirectory = "RNFSFileTypeDirectory";
  
  private final ReactApplicationContext reactContext;

  public RNFileSystem(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return this.MODULE_NAME;
  }

  private Uri getFileUri(String filepath, boolean isDirectoryAllowed) throws IORejectionException {
    Uri uri = Uri.parse(filepath);
    if (uri.getScheme() == null) {
      // No prefix, assuming that provided path is absolute path to file
      File file = new File(filepath);
      if (!isDirectoryAllowed && file.isDirectory()) {
        throw new IORejectionException("EISDIR", "EISDIR: illegal operation on a directory, read '" + filepath + "'");
      }
      uri = Uri.parse("file://" + filepath);
    }
    return uri;
  }

  private String getOriginalFilepath(String filepath, boolean isDirectoryAllowed) throws IORejectionException {
    Uri uri = getFileUri(filepath, isDirectoryAllowed);
    String originalFilepath = filepath;
    if (uri.getScheme().equals("content")) {
      try {
        Cursor cursor = reactContext.getContentResolver().query(uri, null, null, null, null);
        if (cursor.moveToFirst()) {
          originalFilepath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
        }
      } catch (IllegalArgumentException ignored) {
      }
    }
    return originalFilepath;
  }

  private InputStream getInputStream(String filepath) throws IORejectionException {
    Uri uri = getFileUri(filepath, false);
    InputStream stream;
    try {
      stream = reactContext.getContentResolver().openInputStream(uri);
    } catch (FileNotFoundException ex) {
      throw new IORejectionException("ENOENT", "ENOENT: " + ex.getMessage() + ", open '" + filepath + "'");
    }
    if (stream == null) {
      throw new IORejectionException("ENOENT", "ENOENT: could not open an input stream for '" + filepath + "'");
    }
    return stream;
  }

  private OutputStream getOutputStream(String filepath, boolean append) throws IORejectionException {
    Uri uri = getFileUri(filepath, false);
    OutputStream stream;
    try {
      stream = reactContext.getContentResolver().openOutputStream(uri, append ? "wa" : "w");
    } catch (FileNotFoundException ex) {
      throw new IORejectionException("ENOENT", "ENOENT: " + ex.getMessage() + ", open '" + filepath + "'");
    }
    if (stream == null) {
      throw new IORejectionException("ENOENT", "ENOENT: could not open an output stream for '" + filepath + "'");
    }
    return stream;
  }

  private static byte[] getInputStreamBytes(InputStream inputStream) throws IOException {
    byte[] bytesResult;
    ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
    int bufferSize = 1024;
    byte[] buffer = new byte[bufferSize];
    try {
      int len;
      while ((len = inputStream.read(buffer)) != -1) {
        byteBuffer.write(buffer, 0, len);
      }
      bytesResult = byteBuffer.toByteArray();
    } finally {
      try {
        byteBuffer.close();
      } catch (IOException ignored) {
      }
    }
    return bytesResult;
  }

  /**
     * String to byte converter method
     * @param data  Raw data in string format
     * @param encoding Decoder name
     * @return  Converted data byte array
     */
    private static byte[] stringToBytes(String data, String encoding) {
      if(encoding.equalsIgnoreCase("ascii")) {
          return data.getBytes(Charset.forName("US-ASCII"));
      }
      else if(encoding.toLowerCase().contains("base64")) {
          return Base64.decode(data, Base64.NO_WRAP);

      }
    
      return data.getBytes(Charset.forName("UTF-8"));
  }

  /**
   * read file
   */
  @ReactMethod
  public void readFile(String filepath, String encoding, Promise promise) {
    try {
      InputStream inputStream = getInputStream(filepath);
      byte[] inputData = getInputStreamBytes(inputStream);

      if (encoding.toLowerCase().contains("base64")) {
        promise.resolve(Base64.encodeToString(inputData, Base64.NO_WRAP));
      } else {
        promise.resolve(new String(inputData));
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      reject(promise, filepath, ex);
    }
  }

  @ReactMethod
  public void read(String filepath, int length, int position, Promise promise) {
    try {
      InputStream inputStream = getInputStream(filepath);
      byte[] buffer = new byte[length];
      inputStream.skip(position);
      int bytesRead = inputStream.read(buffer, 0, length);

      promise.resolve(new String(buffer));
    } catch (Exception ex) {
      ex.printStackTrace();
      reject(promise, filepath, ex);
    }
  }

  /**
   * mkdir
   */
  @ReactMethod
  public void mkdir(String filepath, ReadableMap options, Promise promise) {
    try {
      File file = new File(filepath);

      file.mkdirs();

      boolean exists = file.exists();

      if (!exists) throw new Exception("Directory could not be created");

      promise.resolve(null);
    } catch (Exception ex) {
      ex.printStackTrace();
      reject(promise, filepath, ex);
    }
  }

  /**
   * exists
   */
  @ReactMethod
  public void exists(String filepath, Promise promise) {
    try {
      File file = new File(filepath);
      promise.resolve(file.exists());
    } catch (Exception ex) {
      ex.printStackTrace();
      reject(promise, filepath, ex);
    }
  }

  /**
   * write file
   */
  @ReactMethod
  public void writeFile(String filepath, String encoding, String data, Boolean append, ReadableMap options, Promise promise) {
    try {
      byte[] bytes = stringToBytes(data, encoding);

      OutputStream outputStream = getOutputStream(filepath, append);
      outputStream.write(bytes);
      outputStream.close();

      promise.resolve(null);
    } catch (Exception ex) {
      ex.printStackTrace();
      reject(promise, filepath, ex);
    }
  }

  private void reject(Promise promise, String filepath, Exception ex) {
    if (ex instanceof FileNotFoundException) {
      rejectFileNotFound(promise, filepath);
      return;
    }
    if (ex instanceof IORejectionException) {
      IORejectionException ioRejectionException = (IORejectionException) ex;
      promise.reject(ioRejectionException.getCode(), ioRejectionException.getMessage());
      return;
    }

    promise.reject(null, ex.getMessage());
  }

  private void rejectFileNotFound(Promise promise, String filepath) {
    promise.reject("ENOENT", "ENOENT: no such file or directory, open '" + filepath + "'");
  }

  private void rejectFileIsDirectory(Promise promise) {
    promise.reject("EISDIR", "EISDIR: illegal operation on a directory, read");
  }

  @Override
  public Map<String, Object> getConstants() {
    final Map<String, Object> constants = new HashMap<>();

    constants.put(RNFSDocumentDirectory, 0);
    constants.put(RNFSDocumentDirectoryPath, this.getReactApplicationContext().getFilesDir().getAbsolutePath());
    constants.put(RNFSTemporaryDirectoryPath, this.getReactApplicationContext().getCacheDir().getAbsolutePath());
    constants.put(RNFSPicturesDirectoryPath, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath());
    constants.put(RNFSCachesDirectoryPath, this.getReactApplicationContext().getCacheDir().getAbsolutePath());
    constants.put(RNFSFileTypeRegular, 0);
    constants.put(RNFSFileTypeDirectory, 1);

    File externalStorageDirectory = Environment.getExternalStorageDirectory();
    if (externalStorageDirectory != null) {
      constants.put(RNFSExternalStorageDirectoryPath, externalStorageDirectory.getAbsolutePath());
    } else {
      constants.put(RNFSExternalStorageDirectoryPath, null);
    }

    File externalDirectory = this.getReactApplicationContext().getExternalFilesDir(null);
    if (externalDirectory != null) {
      constants.put(RNFSExternalDirectoryPath, externalDirectory.getAbsolutePath());
    } else {
      constants.put(RNFSExternalDirectoryPath, null);
    }

    File externalCachesDirectory = this.getReactApplicationContext().getExternalCacheDir();
    if (externalCachesDirectory != null) {
      constants.put(RNFSExternalCachesDirectoryPath, externalCachesDirectory.getAbsolutePath());
    } else {
      constants.put(RNFSExternalCachesDirectoryPath, null);
    }

    return constants;
  }
}