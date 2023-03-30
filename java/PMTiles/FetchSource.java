package PMTiles;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class FetchSource implements Source {

  private final String url;

  public FetchSource(String url) {
    this.url = url;
  }

  public String getKey() {
    return this.url;
  }

  public CompletableFuture<RangeResponse> getBytes(long offset, long length) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        URL urlObj = new URL(this.url);
        URLConnection connection = urlObj.openConnection();
        connection.setConnectTimeout(10 * 1000);
        connection.setReadTimeout(10 * 1000);
        String rangeHeader = "bytes=" + offset + "-" + (offset + length - 1);
        connection.setRequestProperty("Range", rangeHeader);

        int responseCode = ((HttpURLConnection) connection).getResponseCode();
        if (responseCode >= 300) {
          throw new RuntimeException("Bad response code: " + responseCode);
        }

        try (InputStream in = connection.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
          byte[] buffer = new byte[1024];
          int bytesRead;

          while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
          }

          byte[] responseBody = out.toByteArray();
          ByteBuffer data = ByteBuffer.wrap(responseBody);

          String etag = connection.getHeaderField("ETag");
          String cacheControl = connection.getHeaderField("Cache-Control");
          String expires = connection.getHeaderField("Expires");

          return new RangeResponse(data, etag, cacheControl, expires);
        }
      } catch (IOException e) {
        System.out.println("Error: " + e.getMessage());
        return null;
      }
    });
  }
}
