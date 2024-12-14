package src.server.main;

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
                    // if (tokens.size() >= 2) {
                    //     out.println("Er0 full");
                    // } else {
                        tokens.add(token);
                        System.out.println("收到 token: " + token);
                    // }
                }
                while (true) {
                    String message = in.readLine();
                    if (message == null) {
                        break;
                    }
                    System.out.println("收到訊息: " + message);
                    synchronized (clientWriters) {
                        for (PrintWriter writer : clientWriters) {
                            writer.println(message);
                        }
                    }
                }
            } catch (SocketException e) {
                if (e.getMessage().equals("Connection reset")) {
                    System.out.println("Connection reset by peer: " + socket);
                    synchronized (clientWriters) {
                        for (PrintWriter writer : clientWriters) {
                            writer.println("check");
                        }
                    }
                } else {
                    e.printStackTrace();
                }
            }catch (IOException e) {
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