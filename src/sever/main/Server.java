package src.sever.main;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 12345;
    private static Set<PrintWriter> clientWriters = new HashSet<>();
    private static List<String> tokens = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("伺服器啟動中...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String token;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                synchronized (clientWriters) {
                    clientWriters.add(out);
                }

                // 接收並存儲 token
                token = in.readLine();
                synchronized (tokens) {
                    if (tokens.size() >= 2) {
                        out.println("Er0 full");
                    } else {
                        tokens.add(token);
                        System.out.println("收到 token: " + token);
                    }
                }

                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("收到訊息: " + message);
                    if (message.endsWith("close")) {
                        synchronized (tokens) {
                            tokens.remove(token);
                            System.out.println("移除 token: " + token);
                        }
                        break;
                    }
                    synchronized (clientWriters) {
                        for (PrintWriter writer : clientWriters) {
                            writer.println(message);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                synchronized (clientWriters) {
                    clientWriters.remove(out);
                }
            }
        }
    }
}