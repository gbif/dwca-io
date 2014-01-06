package org.gbif.file;

import java.io.File;

import org.junit.Test;

public class DownloadUtilTest {

  @Test
  public void testDownload() throws Exception {
    File t = File.createTempFile("google", "");
    t.deleteOnExit();
    DownloadUtil.download("http://www.google.com", t);
  }
}
