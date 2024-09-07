package ru.netology;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {

    private final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png",
            "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html",
            "/classic.html", "/events.html", "/events.js");
    private final ExecutorService executorService = Executors.newFixedThreadPool(64);
    private static final int PORT = 9999;
    private static final int LIMIT_CONNECTION = 64;

    public Server() {

    }

    public void start() {
        try (final ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                final var socket = serverSocket.accept();
                executorService.submit(() -> connect(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void connect(Socket socket) {
        try (
                socket;
                final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final var out = new BufferedOutputStream(socket.getOutputStream())) {
            final String[] parts = getResponse(in);
            final var path = parts[1];

            if (parts.length != 3) {
                // just close socket
            } else if (!validPaths.contains(path)) {
                errorResponse(out);
            } else {
                getTrueResponse(out, path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String[] getResponse(BufferedReader in) throws IOException {
        final var requestLine = in.readLine();
        final var parts = requestLine.split(" ");
        return parts;
    }

    public void errorResponse(BufferedOutputStream out) throws IOException {
        String response = "HTTP/1.1 404 Not Found\r\n" +
                "Content-Length: 0\r\n" +
                "Connection: close\r\n" +
                "\r\n";
        out.write(response.getBytes());
        out.flush();
    }

    public String correctResponse(String mimeType, long length) {
        return "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + mimeType + "\r\n" +
                "Content-Length: " + length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";
    }

    public void getTrueResponse(BufferedOutputStream out, String path) throws IOException {
        final var filePath = Path.of(".", "public", path);
        final var mimeType = Files.probeContentType(filePath);

        if (path.equals("/classic.html")) {
            final var template = Files.readString(filePath);
            final var content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            out.write(correctResponse(mimeType, content.length).getBytes());
            out.write(content);
        } else {
            final var length = Files.size(filePath);
            out.write(correctResponse(mimeType, length).getBytes());
            Files.copy(filePath, out);
        }
        out.flush();
    }
}