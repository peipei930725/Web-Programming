import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.*;

public class SimpleGameClientWithConnection extends JFrame {

    private GamePanel gamePanel;
    private Socket socket;
    private PrintWriter out;
    
    // 請根據實際情況修改伺服器位址與埠號
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 5000;

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
            // 若無法連接，這裡可考慮直接關閉程式，或繼續離線模式
        }

        gamePanel = new GamePanel();
        add(gamePanel);

        setFocusable(true);
        requestFocusInWindow();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                gamePanel.handleKeyPress(e);
            }
        });

        // 遊戲更新計時器 (約 16ms 一次，相當於 60fps)
        Timer timer = new Timer(16, e -> gamePanel.updateGame());
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
        private List<Bullet> bullets;

        public GamePanel() {
            bullets = new ArrayList<>();
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentShown(ComponentEvent e) {
                    playerX = getWidth()/2 - playerSize/2;
                    playerY = getHeight()/2 - playerSize/2;
                }
            });
        }

        public void handleKeyPress(KeyEvent e) {
            int key = e.getKeyCode();
            if (key == KeyEvent.VK_W) {
                playerY -= playerSpeed;
                sendCommandToServer("w");
            } else if (key == KeyEvent.VK_S) {
                playerY += playerSpeed;
                sendCommandToServer("s");
            } else if (key == KeyEvent.VK_A) {
                playerX -= playerSpeed;
                sendCommandToServer("a");
            } else if (key == KeyEvent.VK_D) {
                playerX += playerSpeed;
                sendCommandToServer("d");
            } else if (key == KeyEvent.VK_SPACE) {
                bullets.add(new Bullet(playerX + playerSize/2, playerY + playerSize/2));
                sendCommandToServer(" ");
            }
        }

        public void updateGame() {
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
