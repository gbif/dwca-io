package org.gbif.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * A simple download utils that uses the native http capabilities of the URL class and NIO.
 * It does not handle redirects and other advanced http options - please use the gbif-httputil HttpUtils class for
 * those cases which makes use of the heavier but more robust HttpClient.
 */
public class DownloadUtil {

  /**
   * A private Utils class.
   */
  private DownloadUtil() {
  }

  /**
   * Copies the content of a URL to a local file.
   *
   * @param url source url
   * @param out file to write content to
   */
  public static void download(URL url, File out) throws IOException {
    ReadableByteChannel rbc = Channels.newChannel(url.openStream());
    FileOutputStream fos = new FileOutputStream(out);
    fos.getChannel().transferFrom(rbc, 0, 1 << 24);
    fos.close();
    rbc.close();
  }

  /**
   * Copies the content of a URL to a local file.
   *
   * @param url source url
   * @param out file to write content to
   */
  public static void download(String url, File out) throws IOException {
    URL u = new URL(url);
    download(u, out);
  }

}
