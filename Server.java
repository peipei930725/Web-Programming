import java.io.*;
import java.net.*;
import java.util.*;
import com.google.gson.Gson;
import java.awt.Color;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.*;

public class Server {
    private static final int PORT = 5000;
    private static final int TICK_RATE = 2;

    private static final int MAX_PLAYERS = 2; // 限制遊戲人數為兩人
    private static final int PLAYER_HEALTH = 100; // 初始血量
    private static final int HEALTH_PACK_RESPAWN_TIME = 10; // 補包重生時間（秒）
    private static final int HEALTH_PACK_HEAL_AMOUNT = 10; // 補包恢復血量

    private static Map<Integer, PlayerState> playerStates = Collections.synchronizedMap(new HashMap<>());
    private static List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private static Gson gson = new Gson();
    private static HealthPack healthPack;
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static final int SCREEN_WIDTH = 1280;
    private static final int SCREEN_HEIGHT = 680;

    public static void main(String[] args) {
        boolean flag = true;
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("伺服器已啟動，等待連線...");

            // 啟動遊戲邏輯更新執行緒
            new Thread(Server::gameLoop).start();
            scheduler.schedule(Server::spawnHealthPack, 5, TimeUnit.SECONDS); // 5秒後生成補包

            while (true) {
                if (clients.size() >= MAX_PLAYERS && flag) {
                    checkPlayersAndSpawnHealthPack();
                    System.out.println("玩家數量已達到上限！");
                    flag = false;
                    continue;
                }

                Socket clientSocket = serverSocket.accept();
                int userId = clients.size(); // 每個玩家一個唯一 ID

                // 設定玩家初始位置與顏色
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
                } else {
                    continue; // 不允許更多玩家加入
                }

                // 初始化玩家狀態
                PlayerState playerState = new PlayerState(userId, startX, startY, playerColor, bulletColor, PLAYER_HEALTH);
                playerStates.put(userId, playerState);

                ClientHandler handler = new ClientHandler(clientSocket, userId);
                clients.add(handler);
                new Thread(handler).start();

                System.out.println("玩家 " + userId + " 已連接！");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void spawnHealthPack() {
        // 只有當玩家數量達到最大值時，才生成補包
        if (clients.size() == 2) {
            int padding = 40; // 確保補包完全在畫面內
            int x = new Random().nextInt(SCREEN_WIDTH - padding * 2) + padding;
            int y = new Random().nextInt(SCREEN_HEIGHT - padding * 2) + padding;
            healthPack = new HealthPack(x, y);
            broadcastHealthPack();
        }
    }
    
    private static void broadcastHealthPack() {
        if (healthPack != null) {
            String json = gson.toJson(healthPack);
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    client.sendMessage("HEALTH_PACK " + json);
                }
            }
        }else {
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    client.sendMessage("HEALTH_PACK " + "{\"x\":0,\"y\":0}");
                }
            }
        }
    }

    
    
    private static void checkPlayersAndSpawnHealthPack() {
        // 當兩位玩家都連接時，開始生成補包
        if (clients.size() == 2 && healthPack == null) {
            scheduler.schedule(Server::spawnHealthPack, 5, TimeUnit.SECONDS);
        }
    }
    
    

    private static void gameLoop() {
        while (true) {
            try {
                Thread.sleep(TICK_RATE);

                synchronized (playerStates) {
                    for (PlayerState player : playerStates.values()) {
                        Set<String> keys = player.keysPressed;

                        // 處理移動邏輯
                        if (keys.contains("w")) player.y = Math.max(0, player.y - player.speed);
                        if (keys.contains("a")) player.x = Math.max(0, player.x - player.speed);
                        if (keys.contains("s")) player.y = Math.min(SCREEN_HEIGHT - 40, player.y + player.speed);
                        if (keys.contains("d")) player.x = Math.min(SCREEN_WIDTH - 40, player.x + player.speed);

                        // 處理射擊邏輯
                        if (keys.contains(" ")) {
                            if (player.fireCooldown == 0) {
                                int direction = player.userId == 0 ? 1 : -1; // 玩家 1 向右，玩家 2 向左
                                if (player.bullets.size() < 10) {
                                    player.bullets.add(new Bullet(player.x + 20, player.y + 20, direction, player.bulletColor));
                                }
                                player.fireCooldown = 100;
                            }
                        }

                        // 減少射擊冷卻時間
                        if (player.fireCooldown > 0) {
                            player.fireCooldown -= TICK_RATE;
                        }
                    }

                    // 更新子彈並檢測碰撞
                    for (PlayerState player : playerStates.values()) {
                        Iterator<Bullet> it = player.bullets.iterator();
                        while (it.hasNext()) {
                            Bullet bullet = it.next();
                            bullet.move();

                            // 檢測子彈是否擊中對方玩家
                            for (PlayerState target : playerStates.values()) {
                                if (target.userId != player.userId) { // 不能擊中自己
                                    if (bullet.x >= target.x && bullet.x <= target.x + 40 &&
                                        bullet.y >= target.y && bullet.y <= target.y + 40) {
                                        target.health -= 10; // 擊中時扣血
                                        it.remove(); // 移除子彈
                                        System.out.println("玩家 " + target.userId + " 被擊中！剩餘血量：" + target.health);
                                        if (target.health <= 0) {
                                            broadcastGameOver(player.userId); // 廣播勝利者
                                            resetGame();
                                            return; // 結束遊戲迴圈
                                        }
                                        break;
                                    }
                                }
                            }

                            // 檢測子彈是否出界
                            if (bullet.x > SCREEN_WIDTH || bullet.x < 0) {
                                it.remove();
                            }
                        }
                    }

                    // 檢測玩家是否碰到補包
                    // 檢測玩家是否碰到補包
                    if (healthPack != null) {
                        for (PlayerState player : playerStates.values()) {
                            if (player.x < healthPack.x + 40 && player.x + 40 > healthPack.x &&
                                player.y < healthPack.y + 40 && player.y + 40 > healthPack.y) {
                                player.health = Math.min(PLAYER_HEALTH, player.health + HEALTH_PACK_HEAL_AMOUNT);
                                System.out.println("玩家 " + player.userId + " 撿取補包，恢復血量至: " + player.health);
                                healthPack = null; // 移除補包
                                broadcastHealthPack(); // 同步到所有客戶端
                                scheduler.schedule(Server::spawnHealthPack, HEALTH_PACK_RESPAWN_TIME, TimeUnit.SECONDS);
                                break;
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
    private static void broadcastGameOver(int winnerId) {
        String message = "GAME_OVER " + winnerId;
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(message);
            }
        }
        System.out.println("遊戲結束！玩家 " + winnerId + " 獲勝！");
        System.exit(0);
    }
    private static void broadcastGameState() {
        GameState gameState = new GameState(new ArrayList<>(playerStates.values()));
        String json = gson.toJson(gameState);

        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(json);
            }
        }
    }

    private static void resetGame() {
        playerStates.clear();
        for (ClientHandler client : clients) {
            client.sendMessage("RESET");
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
                        String key = command.substring(6);
                        playerStates.get(userId).keysPressed.add(key);
                    } else if (command.startsWith("RELEASE ")) {
                        String key = command.substring(8);
                        playerStates.get(userId).keysPressed.remove(key);
                    } else if (command.equals("RESTART")) {
                        resetGame();
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

    static class PlayerState {
        int userId;
        int x, y;
        int speed = 2;
        int fireCooldown = 0;
        int health; // 玩家血量
        List<Bullet> bullets = new ArrayList<>();
        Set<String> keysPressed = Collections.synchronizedSet(new HashSet<>());
        int playerColor;
        int bulletColor;

        PlayerState(int userId, int x, int y, int playerColor, int bulletColor, int health) {
            this.userId = userId;
            this.x = x;
            this.y = y;
            this.playerColor = playerColor;
            this.bulletColor = bulletColor;
            this.health = health;
        }
    }

    static class Bullet {
        int x, y;
        int speed = 4;
        int direction;
        int color;

        Bullet(int x, int y, int direction, int color) {
            this.x = x;
            this.y = y;
            this.direction = direction;
            this.color = color;
        }

        void move() {
            x += speed * direction;
        }
    }

    static class GameState {
        List<PlayerState> players;

        GameState(List<PlayerState> players) {
            this.players = players;
        }
    }

    static class HealthPack {
        int x, y;

        HealthPack(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}