package ru.netology;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.*;

public class Request {
    private String method;
    private String path;
    private Map<String, List<String>> queryParams;
    private List<String> headers;
    private byte[] body;
    private final InputStream in;
    private final static String GET = "GET";
    private final static String POST = "POST";

    public Request(String method, String path, List<String> headers, Map<String,
            List<String>> queryParams, InputStream in) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.queryParams = queryParams;
        this.in = in;
    }

    public static Request getRequest(InputStream inputStream, BufferedOutputStream out) throws IOException, NumberFormatException, URISyntaxException {
        final var allowedMethods = List.of(GET, POST);
        final var in = new BufferedInputStream(inputStream);
        final var limit = 4096;

        in.mark(limit);
        final var buffer = new byte[limit];
        final var read = in.read(buffer);

        // ищем request line
        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) {
            badRequest(out);
            return null;

        }

        // читаем request line
        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
            badRequest(out);
            return null;
        }

        final var method = requestLine[0];
        if (!allowedMethods.contains(method)) {
            badRequest(out);
            return null;
        }

        final var pathWithQuery = requestLine[1];
        if (!pathWithQuery.startsWith("/")) {
            badRequest(out);
            return null;
        }

        final String path;
        final Map<String, List<String>> query;

        if (pathWithQuery.contains("?")) {
            String[] value = pathWithQuery.split("\\?");
            path = value[0];
            String queryLine = value[1];
            query = parseUrlToQuery(queryLine);
        } else {
            path = pathWithQuery;
            query = null;
        }

        // ищем заголовки
        final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            badRequest(out);
            return null;
        }

        // отматываем на начало буфера
        in.reset();
        // пропускаем requestLine
        in.skip(headersStart);

        final var headersBytes = in.readNBytes(headersEnd - headersStart);
        final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));

        // для GET тела нет
        if (!method.equals(GET)) {
            in.skip(headersDelimiter.length);
            // вычитываем Content-Length, чтобы прочитать body
            final var contentLength = extractHeader(headers, "Content-Length");
            if (contentLength.isPresent()) {
                final var length = Integer.parseInt(contentLength.get());
                final var bodyBytes = in.readNBytes(length);
                final var body = new String(bodyBytes);
                System.out.println(body);
            }
        }
        return new Request(method, path, headers, query, inputStream);
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    private static Map<String, List<String>> parseUrlToQuery(String queryLine) throws URISyntaxException {
        HashMap<String, List<String>> map = new HashMap<>();
        var nameValuePairs = URLEncodedUtils.parse(queryLine, Charset.defaultCharset(), '&');
        for (NameValuePair nameValuePair : nameValuePairs) {
            if (map.get(nameValuePair.getName()) == null) {
                map.put(nameValuePair.getName(), new ArrayList<>());
                map.get(nameValuePair.getName()).add(nameValuePair.getValue());
            } else {
                map.get(nameValuePair.getName()).add(nameValuePair.getValue());
            }
        }
        return map;
    }

    public List<String> getQueryParam(String name) {
        return queryParams.get(name);
    }

    // from google guava with modifications
    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }


    public List<String> getHeaders() {
        return headers;
    }


    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public Map<String, List<String>> getQueryParams() {
        return queryParams;
    }


    @Override
    public String toString() {
        return "Request{" +
                "method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", queryParams=" + queryParams +
                ", headers=" + headers +
                ", body=" + Arrays.toString(body) +
                '}';
    }
}
