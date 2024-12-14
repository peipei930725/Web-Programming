import java.io.*;
import java.net.*;
import java.util.*;
import com.google.gson.Gson;
import java.awt.Color;

public class Server {
    private static final int PORT = 5000;
    private static final int TICK_RATE = 2; // 每 16ms 更新一次 (約 60fps)

    private static Map<Integer, PlayerState> playerStates = Collections.synchronizedMap(new HashMap<>());
    private static List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private static Gson gson = new Gson();

    // 遊戲畫面大小
    private static final int SCREEN_WIDTH = 1280;
    private static final int SCREEN_HEIGHT = 680;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("伺服器已啟動，等待連線...");
    
            // 啟動遊戲邏輯更新執行緒
            new Thread(Server::gameLoop).start();
    
            while (true) {
                Socket clientSocket = serverSocket.accept();
                int userId = clients.size(); // 每個玩家一個唯一 ID
    
                // 根據 userId 設定初始位置與顏色
                int startX, startY;
                int playerColor, bulletColor;
                if (userId == 0) { // 玩家 1
                    startX = 50;
                    startY = SCREEN_HEIGHT / 2;
                    playerColor = Color.YELLOW.getRGB();
                    bulletColor = Color.BLUE.getRGB();
                } else if (userId == 1) { // 玩家 2
                    startX = SCREEN_WIDTH - 90;
                    startY = SCREEN_HEIGHT / 2;
                    playerColor = Color.GREEN.getRGB();
                    bulletColor = Color.RED.getRGB();
                } else { // 其他玩家默認
                    startX = SCREEN_WIDTH / 2;
                    startY = SCREEN_HEIGHT / 2;
                    playerColor = Color.GRAY.getRGB();
                    bulletColor = Color.BLACK.getRGB();
                }
    
                // 初始化玩家狀態
                PlayerState playerState = new PlayerState(userId, startX, startY, playerColor, bulletColor);
                playerStates.put(userId, playerState);
    
                ClientHandler handler = new ClientHandler(clientSocket, userId);
                clients.add(handler);
                new Thread(handler).start();
    
                System.out.println("玩家 " + userId + " 已連接，顏色：" + playerColor);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
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
                                int direction = player.userId == 0 ? 1 : -1; // 玩家 1 向右，玩家 2 向左
                                if (player.bullets.size() < 10) { // 限制子彈數量
                                    player.bullets.add(new Bullet(player.x + 20, player.y + 20, direction, player.bulletColor));
                                }
                                player.fireCooldown = 200; // 設置冷卻時間
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
                            bullet.move(); // 子彈移動
                            if (bullet.x > SCREEN_WIDTH || bullet.x < 0) { // 子彈出界
                                it.remove();
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
        int speed = 2;
        int fireCooldown = 0;
        List<Bullet> bullets = new ArrayList<>();
        Set<String> keysPressed = Collections.synchronizedSet(new HashSet<>());
        int playerColor; // 玩家顏色（RGB 值）
        int bulletColor; // 子彈顏色（RGB 值）
    
        PlayerState(int userId, int x, int y, int playerColor, int bulletColor) {
            this.userId = userId;
            this.x = x;
            this.y = y;
            this.playerColor = playerColor;
            this.bulletColor = bulletColor;
        }
    }
    
    // 子彈類別
    static class Bullet {
        int x, y;
        int speed = 4;
        int direction; // 子彈方向：1 表右，-1 表左
        int color; // 子彈顏色（RGB 值）
    
        Bullet(int x, int y, int direction, int color) {
            this.x = x;
            this.y = y;
            this.direction = direction;
            this.color = color;
        }
    
        void move() {
            x += speed * direction; // 根據方向更新位置
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
