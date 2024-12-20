import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.*;
import com.google.gson.Gson;;


public class Client extends JFrame {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 5000;

    private Socket socket;
    private PrintWriter out;
    private List<PlayerState> players = new ArrayList<>();
    private GamePanel gamePanel;
    private boolean gameOver = false; // 遊戲結束標誌
    private int winnerId = -1; // 獲勝者ID
    private HealthPack healthPack;

    public Client() {
        setTitle("Multiplayer Game Client");
        setSize(1280, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 建立連線
        try {
            socket = new Socket(HOST, PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "無法連接到伺服器", "錯誤", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // 初始化遊戲面板
        gamePanel = new GamePanel();
        BackgroundMusic.play();
        add(gamePanel);

        // 處理按鍵事件
        setFocusable(true);
        requestFocusInWindow();
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!gameOver) sendCommandToServer("PRESS " + e.getKeyChar());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (!gameOver) sendCommandToServer("RELEASE " + e.getKeyChar());
            }
        });

        // 啟動接收伺服器狀態的執行緒
        new Thread(new GameStateReceiver()).start();
    }

    private void sendCommandToServer(String cmd) {
        if (out != null) out.println(cmd);
    }

    private class GamePanel extends JPanel {
        private Image backgroundImage;
        private Image healthPackImage;

        public GamePanel() {
            try {
                // 載入背景與補包圖片
                backgroundImage = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/img/bg.png"));
                healthPackImage = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/img/hp.jpg"));
                if (healthPackImage == null) {
                    System.out.println("補包圖片載入失敗！");
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("圖片載入失敗！");
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // 繪製背景圖片
            if (backgroundImage != null) {
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            } else {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            


            // 繪製玩家和子彈繪製補包
            if (!gameOver) {
                for (PlayerState player : players) {
                    g.setColor(new Color(player.playerColor));
                    g.fillOval(player.x, player.y, 60, 60);

                    g.setColor(new Color(player.bulletColor));
                    for (Bullet bullet : player.bullets) {
                        g.fillOval(bullet.x, bullet.y, 20, 20);
                    }
                    if (healthPack != null) {
                        int x = healthPack.x;
                        int y = healthPack.y;
                        g.drawImage(healthPackImage, x, y, 40, 40, this);
                    }
                }
                drawHealthBars(g);
            } else {
                drawGameOverScreen(g);
            }
        }

        private void drawHealthBars(Graphics g) {
            for (PlayerState player : players) {
                int barWidth = 200, barHeight = 20;
                int barX = player.userId == 0 ? 50 : getWidth() - 250;
                int barY = 20;

                int currentWidth = (int) ((player.health / 100.0) * barWidth);

                g.setColor(Color.GRAY);
                g.fillRect(barX, barY, barWidth, barHeight);

                g.setColor(Color.RED);
                g.fillRect(barX, barY, currentWidth, barHeight);

                g.setColor(Color.WHITE);
                g.drawRect(barX, barY, barWidth, barHeight);
                g.drawString("Player " + (player.userId + 1) + ": " + player.health + " HP", barX, barY - 5);
            }
        }

        private void drawGameOverScreen(Graphics g) {
            setLayout(null);

            // 繪製遊戲結束畫面
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 36));
            g.drawString("Player " + (winnerId + 1) + " Win!", getWidth() / 2 - 100, getHeight() / 2 - 100);

            // JButton restartButton = new JButton("再來一局");
            // restartButton.setBounds(getWidth() / 2 - 150, getHeight() / 2 - 25, 150, 50);
            // restartButton.addActionListener(e -> restartGame());
            // add(restartButton);

            JButton exitButton = new JButton("結束遊戲");
            exitButton.setBounds(getWidth() / 2 - 100 , getHeight() / 2 - 25, 150, 50);
            exitButton.addActionListener(e -> System.exit(0));
            add(exitButton);

            revalidate();
            repaint();
        }
    }

    private void restartGame() {
        gameOver = false;
        winnerId = -1;
        sendCommandToServer("RESTART");
    }

    private class GameStateReceiver implements Runnable {
        private BufferedReader in;
        private Gson gson = new Gson();

        public GameStateReceiver() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                String json;
                while ((json = in.readLine()) != null) {
                    if (json.startsWith("HEALTH_PACK ")) {
                        System.out.println(json);
                        HealthPack receivedHealthPack = gson.fromJson(json.substring(12), HealthPack.class);
                        if (receivedHealthPack.x == 0 && receivedHealthPack.y == 0) {
                            healthPack = null; // 伺服器同步補包已刪除
                        } else {
                            healthPack = receivedHealthPack; // 更新補包狀態
                        }
                        gamePanel.repaint();
                    } else if (json.startsWith("GAME_OVER ")) {
                        int winnerId = Integer.parseInt(json.split(" ")[1]);
                        gameOver = true;
                        Client.this.winnerId = winnerId; // 設置獲勝者 ID
                        gamePanel.repaint();
                    } else {
                        GameState gameState = gson.fromJson(json, GameState.class);
                        players = gameState.players;
                        gamePanel.repaint();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static class PlayerState {
        int userId, x, y, health, playerColor, bulletColor;
        List<Bullet> bullets;
    }

    static class Bullet {
        int x, y;
    }

    static class GameState {
        List<PlayerState> players;
    }

    static class HealthPack {
        int x, y;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Client client = new Client();
            client.setVisible(true);
        });
    }
}
