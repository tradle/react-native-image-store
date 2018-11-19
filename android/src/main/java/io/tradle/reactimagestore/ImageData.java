package io.tradle.reactimagestore;

public class ImageData {
  public final byte[] bytes;
  public final String mimeType;
  public ImageData(byte[] bytes, String mimeType) {
    this.bytes = bytes;
    this.mimeType = mimeType;
  }
}
