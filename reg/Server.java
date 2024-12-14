import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 12345;
    private static final int MAP_SIZE = 5; // 簡化地圖大小
    private static char[][] gameMap = new char[MAP_SIZE][MAP_SIZE]; // ' ' = 空地, '#' = 牆壁, 'P' = 玩家, 'B' = 炸彈
    private static ConcurrentHashMap<String, Socket> clients = new ConcurrentHashMap<>();
    
    public static void main(String[] args) throws IOException {
        // 初始化地圖
        initializeMap();

        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("伺服器已啟動，等待玩家連線...");

        ExecutorService pool = Executors.newFixedThreadPool(2); // 兩個玩家

        while (clients.size() < 2) {
            Socket clientSocket = serverSocket.accept();
            String playerName = "Player" + (clients.size() + 1);
            clients.put(playerName, clientSocket);
            pool.execute(() -> handleClient(clientSocket, playerName));
            System.out.println(playerName + " 已連線！");
        }

        pool.shutdown();
        System.out.println("遊戲開始！");
    }

    private static void initializeMap() {
        for (int i = 0; i < MAP_SIZE; i++) {
            for (int j = 0; j < MAP_SIZE; j++) {
                gameMap[i][j] = (Math.random() > 0.8) ? '#' : ' ';
            }
        }
        gameMap[0][0] = 'P'; // 玩家1初始位置
        gameMap[MAP_SIZE - 1][MAP_SIZE - 1] = 'P'; // 玩家2初始位置
    }

    private static void handleClient(Socket clientSocket, String playerName) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        ) {
            out.println("歡迎來到爆爆王！你的代號是：" + playerName);
            out.println(renderMap());

            String input;
            while ((input = in.readLine()) != null) {
                if (input.equalsIgnoreCase("quit")) {
                    clients.remove(playerName);
                    clientSocket.close();
                    break;
                }
                processCommand(input, playerName);
                broadcast(renderMap());
            }
        } catch (IOException e) {
            System.out.println("玩家 " + playerName + " 離線。");
        }
    }

    private static synchronized void processCommand(String command, String playerName) {
        // 簡單解析命令，例如 "UP", "DOWN", "BOMB"
        System.out.println(playerName + " 的指令: " + command);
        // TODO: 更新遊戲邏輯，處理移動或放置炸彈
    }

    private static synchronized void broadcast(String message) throws IOException {
        for (Socket socket : clients.values()) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message);
        }
    }

    private static String renderMap() {
        StringBuilder sb = new StringBuilder();
        for (char[] row : gameMap) {
            for (char cell : row) {
                sb.append(cell).append(' ');
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
