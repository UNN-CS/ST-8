package ru.unn.cs.st8;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Downloads the printable PDF via the site's GET CGI (same as the HTML form).
 *
 * <p>The site's HTTPS certificate may be expired (see course README). We always prefer {@code http}
 * for the CGI URL, and configure a permissive TLS context on the {@linkplain HttpClient} so that if
 * the server redirects to {@code https}, downloads still succeed in the lab assignment context.
 *
 * <p><strong>Security:</strong> do not reuse this TLS setup for arbitrary hosts outside this
 * exercise.
 */
public final class PapercasePdfDownloader {

  public static final String OUTPUT_FILENAME = "cd.pdf";

  private PapercasePdfDownloader() {}

  /**
   * @param landingPage Uri of whatever page resolved after opening the site (scheme/host/port only).
   */
  public static Path downloadPdf(URI landingPage, CoverData cover, Path outputPath)
      throws IOException, InterruptedException {
    Objects.requireNonNull(landingPage, "landingPage");
    Objects.requireNonNull(cover, "cover");
    Objects.requireNonNull(outputPath, "outputPath");

    URI baseHttp = stripToHttp(landingPage);
    URI endpoint = baseHttp.resolve("/papercdcase.cgi/papercdcase.pdf").normalize();
    String rawQuery = encodeForm(cover);
    URI pdfUri = URI.create(endpoint + "?" + rawQuery);

    HttpClient client;
    try {
      client =
          HttpClient.newBuilder()
              .sslContext(relaxedTls())
              .connectTimeout(Duration.ofSeconds(45))
              .followRedirects(HttpClient.Redirect.ALWAYS)
              .build();
    } catch (GeneralSecurityException e) {
      throw new IOException("Unable to initialise HTTP client TLS", e);
    }

    HttpRequest req =
        HttpRequest.newBuilder(pdfUri)
            .timeout(Duration.ofMinutes(3))
            .header(
                "User-Agent",
                "Mozilla/5.0 (compatible; ST-8 coursework; papercdcase CGI client)")
            .GET()
            .build();

    HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
    int code = resp.statusCode();
    if (code >= 400) {
      throw new IOException("Unexpected HTTP status " + code + " for initial " + pdfUri);
    }

    byte[] body = validatePdf(resp.body(), pdfUri.toString());

    Path parent = outputPath.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.write(
        outputPath, body, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    return outputPath.normalize();
  }

  /** Papercdcase warns about HTTPS/TLS problems; CGI works over plain HTTP — avoid PKIX on {@code https} when possible. */
  private static URI stripToHttp(URI landing) {
    if (landing != null && "https".equalsIgnoreCase(landing.getScheme())) {
      String ascii = landing.toASCIIString();
      return URI.create(ascii.replaceFirst("(?i)^https:", "http:"));
    }
    return landing;
  }

  /** Accept-all trust store for localhost-style lab tooling only — allows expired certs on redirects. */
  private static SSLContext relaxedTls() throws GeneralSecurityException {
    TrustManager[] trustManagers =
        new TrustManager[] {
          new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {}

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {}

            @Override
            public X509Certificate[] getAcceptedIssuers() {
              return new X509Certificate[0];
            }
          }
        };
    SSLContext ctx = SSLContext.getInstance("TLS");
    ctx.init(null, trustManagers, new SecureRandom());
    return ctx;
  }

  private static byte[] validatePdf(byte[] body, String requestLabel) throws IOException {
    if (body == null
        || body.length < 512
        || body[0] != (byte) '%'
        || body[1] != (byte) 'P'
        || body[2] != (byte) 'D'
        || body[3] != (byte) 'F') {
      throw new IOException("Response body is not a PDF (length=" + (body != null ? body.length : -1) + ") for " + requestLabel);
    }
    return body;
  }

  /** Mirrors the CGI parameters produced by PapercasePage selections (jewel case, A4, force save-as). */
  static String encodeForm(CoverData cover) {
    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("sql_serial", "");
    fields.put("sql_genre", "");
    fields.put("artist", cover.artist());
    fields.put("title", cover.title());
    for (int i = 1; i <= CoverData.MAX_TRACKS_FIELDS; i++) {
      String track = i <= cover.tracks().size() ? cover.tracks().get(i - 1) : "";
      fields.put("track" + i, track);
    }
    fields.put("template", "jewel");
    fields.put("size", "a4");
    fields.put("lang", "west");
    fields.put("force_saveas", "yes");
    fields.put("submit.x", "1");
    fields.put("submit.y", "1");

    List<String> pairs = new ArrayList<>();
    for (Map.Entry<String, String> e : fields.entrySet()) {
      String k = URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8);
      String v = URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8);
      pairs.add(k + "=" + v);
    }
    return String.join("&", pairs);
  }
}
