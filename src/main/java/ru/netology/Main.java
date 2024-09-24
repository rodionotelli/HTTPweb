package ru.netology;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {
        final var server = new Server();

        server.addHandler("GET", "/classic.html", new Handler() {
                    public void handle(Request request, BufferedOutputStream out) throws IOException {
                        try {
                            final var filePath = Path.of(".", "public", request.getPath());
                            final var mimeType = Files.probeContentType(filePath);
                            System.out.println(request.getPath());
                            if (request.getQueryParams() != null) {
                                request.getQueryParams().forEach((key, value) -> System.out.println(key + ":" + value));
                            }
                            // special case for classic
                            final var template = Files.readString(filePath);
                            final var content = template.replace(
                                    "{time}",
                                    LocalDateTime.now().toString()
                            ).getBytes();
                            out.write((
                                    "HTTP/1.1 200 OK\r\n" +
                                            "Content-Type: " + mimeType + "\r\n" +
                                            "Content-Length: " + content.length + "\r\n" +
                                            "Connection: close\r\n" +
                                            "\r\n"
                            ).getBytes());
                            out.write(content);
                            out.flush();

                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    }
                }
        );
        server.listen(9999);
    }
}


