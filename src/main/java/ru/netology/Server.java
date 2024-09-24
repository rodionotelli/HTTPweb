package ru.netology;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {

    private final ExecutorService executorService = Executors.newFixedThreadPool(64);
    private Map<String, Map<String, Handler>> handlers = new ConcurrentHashMap<>();


    public Server() {

    }

    public void listen(int port) {
        try (final ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                final var socket = serverSocket.accept();
                executorService.submit(() -> {
                    try {
                        connect(socket);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private void connect(Socket socket) throws IOException, URISyntaxException {
        try (
                socket;
                final var in = socket.getInputStream();
                final var out = new BufferedOutputStream(socket.getOutputStream());
        ) {
            var request = Request.getRequest(in, out);
            var handlerMap = handlers.get(request.getMethod());
            if (handlerMap == null) {
                notFoundError(out);
                return;
            }
            var handler = handlerMap.get(request.getPath());
            if (handler == null) {
                notFoundError(out);
                return;
            }
            handler.handle(request, out);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void notFoundError(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    public void addHandler(String method, String path, Handler handler) {
        if (handlers.get(method) == null) {
            handlers.put(method, new ConcurrentHashMap<>());
        }
        handlers.get(method).put(path, handler);
    }
}