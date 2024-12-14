import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import javax.swing.*;

public class SimpleGameClientWithConnection extends JFrame {

    private GamePanel gamePanel;
    private Socket socket;
    private PrintWriter out;

    // 請根據實際情況修改伺服器位址與埠號
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 5000;

    // 用於追蹤按鍵狀態的 Set
    private Set<Integer> keysPressed = new HashSet<>();

    public SimpleGameClientWithConnection() {
        setTitle("Simple Game Client with Connection");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 嘗試連線到伺服器
        try {
            socket = new Socket(HOST, PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("已連上伺服器！");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "無法連接到伺服器，請確認伺服器已啟動並正確埠號", "連接失敗", JOptionPane.ERROR_MESSAGE);
        }

        gamePanel = new GamePanel();
        add(gamePanel);

        setFocusable(true);
        requestFocusInWindow();

        // 處理按鍵按下事件
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                keysPressed.add(e.getKeyCode()); // 將按下的按鍵加入 Set
            }

            @Override
            public void keyReleased(KeyEvent e) {
                keysPressed.remove(e.getKeyCode()); // 將放開的按鍵移出 Set
            }
        });

        // 遊戲更新計時器 (約 16ms 一次，相當於 60fps)
        javax.swing.Timer timer = new javax.swing.Timer(16, e -> gamePanel.updateGame());
        timer.start();
    }

    // 封裝發送指令到伺服器的功能
    private void sendCommandToServer(String cmd) {
        if (out != null) {
            out.println(cmd);
            out.flush();
        }
    }

    class GamePanel extends JPanel {
        private int playerX, playerY;
        private int playerSize = 40;
        private int playerSpeed = 5;
        private final java.util.List<Bullet> bullets;

        private int fireCooldown = 0; // 射擊冷卻時間 (用來限制射速)

        public GamePanel() {
            bullets = new ArrayList<>();
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentShown(ComponentEvent e) {
                    playerX = getWidth() / 2 - playerSize / 2;
                    playerY = getHeight() / 2 - playerSize / 2;
                }
            });
        }

        public void updateGame() {
            // 移動邏輯 (根據按鍵狀態進行操作)
            if (keysPressed.contains(KeyEvent.VK_W)) {
                playerY -= playerSpeed;
                sendCommandToServer("w");
            }
            if (keysPressed.contains(KeyEvent.VK_S)) {
                playerY += playerSpeed;
                sendCommandToServer("s");
            }
            if (keysPressed.contains(KeyEvent.VK_A)) {
                playerX -= playerSpeed;
                sendCommandToServer("a");
            }
            if (keysPressed.contains(KeyEvent.VK_D)) {
                playerX += playerSpeed;
                sendCommandToServer("d");
            }

            // 射擊邏輯 (按住空白鍵射擊)
            if (keysPressed.contains(KeyEvent.VK_SPACE)) {
                if (fireCooldown == 0) { // 射擊冷卻結束後才能再次射擊
                    bullets.add(new Bullet(playerX + playerSize / 2, playerY + playerSize / 2));
                    sendCommandToServer(" ");
                    fireCooldown = 10; // 射擊冷卻時間 (約 10*16ms = 160ms)
                }
            }

            // 減少射擊冷卻時間
            if (fireCooldown > 0) {
                fireCooldown--;
            }

            // 更新子彈位置
            Iterator<Bullet> it = bullets.iterator();
            while (it.hasNext()) {
                Bullet b = it.next();
                b.x += b.speed;
                if (b.x > getWidth()) {
                    it.remove();
                }
            }

            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // 背景
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());

            // 繪製玩家 (橘色圓形)
            g.setColor(Color.ORANGE);
            g.fillOval(playerX, playerY, playerSize, playerSize);

            // 繪製子彈 (藍色小圓)
            g.setColor(Color.BLUE);
            for (Bullet b : bullets) {
                g.fillOval(b.x, b.y, b.size, b.size);
            }
        }
    }

    class Bullet {
        int x, y;
        int size = 10;
        int speed = 10;

        public Bullet(int startX, int startY) {
            this.x = startX;
            this.y = startY;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SimpleGameClientWithConnection frame = new SimpleGameClientWithConnection();
            frame.setVisible(true);
        });
    }
}
