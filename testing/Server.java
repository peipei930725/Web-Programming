import java.io.*;
import java.net.*;
import java.util.*;
import com.google.gson.Gson;

public class Server {
    private static final int PORT = 5000;
    private static final int TICK_RATE = 2; // 每 16ms 更新一次 (約 60fps)

    private static Map<Integer, PlayerState> playerStates = Collections.synchronizedMap(new HashMap<>());
    private static List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private static Gson gson = new Gson();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("伺服器已啟動，等待連線...");

            // 啟動遊戲邏輯更新執行緒
            new Thread(Server::gameLoop).start();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                int userId = clients.size(); // 每個玩家一個唯一 ID
                PlayerState playerState = new PlayerState(userId, 400, 300);
                playerStates.put(userId, playerState);

                ClientHandler handler = new ClientHandler(clientSocket, userId);
                clients.add(handler);
                new Thread(handler).start();

                System.out.println("玩家 " + userId + " 已連接");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 遊戲邏輯循環
// 遊戲畫面大小
private static final int SCREEN_WIDTH = 1280;
private static final int SCREEN_HEIGHT = 680;

private static void gameLoop() {
    while (true) {
        try {
            Thread.sleep(TICK_RATE);

            // 更新邏輯
            synchronized (playerStates) {
                for (PlayerState player : playerStates.values()) {
                    Set<String> keys = player.keysPressed;

                    // 處理移動邏輯
                    if (keys.contains("w")) player.y = Math.max(0, player.y - player.speed); // 上邊界
                    if (keys.contains("a")) player.x = Math.max(0, player.x - player.speed); // 左邊界
                    if (keys.contains("s")) player.y = Math.min(SCREEN_HEIGHT - 40, player.y + player.speed); // 下邊界
                    if (keys.contains("d")) player.x = Math.min(SCREEN_WIDTH - 40, player.x + player.speed); // 右邊界

                    // 處理射擊邏輯
                    if (keys.contains(" ")) {
                        if (player.fireCooldown == 0) {
                            if (player.bullets.size() < 10) { // 限制最大子彈數量
                                player.bullets.add(new Bullet(player.x + 20, player.y + 20));
                            }
                            player.fireCooldown = 200; // 設置冷卻時間 (200ms)
                        }
                    }

                    // 減少射擊冷卻時間
                    if (player.fireCooldown > 0) {
                        player.fireCooldown -= TICK_RATE;
                    }

                    // 更新子彈位置
                    Iterator<Bullet> it = player.bullets.iterator();
                    while (it.hasNext()) {
                        Bullet bullet = it.next();
                        bullet.x += bullet.speed;
                        if (bullet.x > SCREEN_WIDTH || bullet.y < 0 || bullet.y > SCREEN_HEIGHT) {
                            it.remove(); // 子彈出界，刪除
                        }
                    }
                }
            }

            // 廣播遊戲狀態
            broadcastGameState();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}


    // 廣播遊戲狀態
    public static void broadcastGameState() {
        GameState gameState = new GameState(new ArrayList<>(playerStates.values()));
        String json = gson.toJson(gameState);

        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(json);
            }
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private int userId;
        private PrintWriter out;

        public ClientHandler(Socket socket, int userId) {
            this.socket = socket;
            this.userId = userId;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"))) {
                out = new PrintWriter(socket.getOutputStream(), true);

                String command;
                while ((command = in.readLine()) != null) {
                    if (command.startsWith("PRESS ")) {
                        String key = command.substring(6); // 按鍵值
                        playerStates.get(userId).keysPressed.add(key);
                    } else if (command.startsWith("RELEASE ")) {
                        String key = command.substring(8); // 按鍵值
                        playerStates.get(userId).keysPressed.remove(key);
                    }
                }
            } catch (IOException e) {
                System.out.println("玩家 " + userId + " 已斷線");
            } finally {
                playerStates.remove(userId);
                clients.remove(this);
            }
        }

        public void sendMessage(String msg) {
            if (out != null) {
                out.println(msg);
            }
        }
    }

    // 玩家狀態類別
    static class PlayerState {
        int userId;
        int x, y;
        int speed = 1;
        int fireCooldown = 0; // 射擊冷卻時間
        List<Bullet> bullets = new ArrayList<>();
        Set<String> keysPressed = Collections.synchronizedSet(new HashSet<>()); // 當前按下的按鍵集合

        PlayerState(int userId, int x, int y) {
            this.userId = userId;
            this.x = x;
            this.y = y;
        }
    }

    // 子彈類別
    static class Bullet {
        int x, y;
        int speed = 1;

        Bullet(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    // 遊戲狀態類別
    static class GameState {
        List<PlayerState> players;

        GameState(List<PlayerState> players) {
            this.players = players;
        }
    }
}
