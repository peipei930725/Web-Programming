import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.*;
import com.google.gson.Gson;

public class Client extends JFrame {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 5000;

    private Socket socket;
    private PrintWriter out;
    private List<PlayerState> players = new ArrayList<>();
    private GamePanel gamePanel;
    private boolean gameOver = false; // 遊戲結束標誌
    private int winnerId = -1; // 獲勝者ID

    public Client() {
        setTitle("Multiplayer Game Client");
        setSize(1280, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        try {
            socket = new Socket(HOST, PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "無法連接到伺服器", "錯誤", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        gamePanel = new GamePanel();
        add(gamePanel);

        // 處理按鍵事件
        setFocusable(true);
        requestFocusInWindow();
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!gameOver) {
                    sendCommandToServer("PRESS " + e.getKeyChar());
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (!gameOver) {
                    sendCommandToServer("RELEASE " + e.getKeyChar());
                }
            }
        });

        // 啟動接收伺服器廣播的執行緒
        new Thread(new GameStateReceiver()).start();
    }

    private void sendCommandToServer(String cmd) {
        if (out != null) {
            out.println(cmd);
        }
    }

    private class GamePanel extends JPanel {
        private JButton restartButton;
        private JButton exitButton;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // 繪製背景
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());

            // 檢查是否有玩家死亡
            for (PlayerState player : players) {
                if (player.health <= 0) {
                    gameOver = true;
                    winnerId = (player.userId == 0) ? 1 : 0; // 設定獲勝者ID
                }
            }

            // 繪製玩家和子彈
            if (!gameOver) {
                for (PlayerState player : players) {
                    // 玩家
                    g.setColor(new Color(player.playerColor));
                    g.fillOval(player.x, player.y, 60, 60); // 玩家變大

                    // 子彈
                    g.setColor(new Color(player.bulletColor));
                    for (Bullet bullet : player.bullets) {
                        g.fillOval(bullet.x, bullet.y, 20, 20); // 子彈變大
                    }
                }

                // 繪製血條
                drawHealthBars(g);
            } else {
                // 顯示遊戲結束畫面
                drawGameOverScreen(g);
            }
        }

        private void drawHealthBars(Graphics g) {
            for (PlayerState player : players) {
                int healthBarWidth = 200;
                int healthBarHeight = 20;

                int healthBarX, healthBarY;
                if (player.userId == 0) { // 玩家 1 血條在左上角
                    healthBarX = 50;
                    healthBarY = 20;
                } else { // 玩家 2 血條在右上角
                    healthBarX = getWidth() - 250;
                    healthBarY = 20;
                }

                int healthBarCurrentWidth = (int) ((player.health / 100.0) * healthBarWidth);

                g.setColor(Color.GRAY);
                g.fillRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);

                g.setColor(Color.RED);
                g.fillRect(healthBarX, healthBarY, healthBarCurrentWidth, healthBarHeight);

                g.setColor(Color.BLACK);
                g.drawRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);
                g.drawString("Player " + (player.userId + 1) + ": " + player.health + " HP", healthBarX, healthBarY - 5);

                if (player.health <= 0) {
                    g.drawString("Player " + (player.userId + 1) + " Dead!", healthBarX, healthBarY + 40);
                }
            }
        }

        private void drawGameOverScreen(Graphics g) {
            setLayout(null);

            // 顯示獲勝者
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 36));
            g.drawString("Player " + (winnerId + 1) + " Win!", getWidth() / 2 - 100, getHeight() / 2 - 100);

            // 再來一局按鈕
            if (restartButton == null) {
                restartButton = new JButton("再來一局");
                restartButton.setBounds(getWidth() / 2 - 150, getHeight() / 2 - 25, 150, 50);
                restartButton.addActionListener(e -> restartGame());
                add(restartButton);
            }

            // 結束遊戲按鈕
            if (exitButton == null) {
                exitButton = new JButton("結束遊戲");
                exitButton.setBounds(getWidth() / 2 + 10, getHeight() / 2 - 25, 150, 50);
                exitButton.addActionListener(e -> System.exit(0));
                add(exitButton);
            }

            revalidate();
            repaint();
        }
    }

    private void restartGame() {
        gameOver = false;
        winnerId = -1; // 重置獲勝者ID

        // 重置遊戲狀態
        for (PlayerState player : players) {
            player.health = 100; // 重置血量
            player.bullets.clear(); // 清除子彈
        }

        // 移除按鈕
        gamePanel.remove(gamePanel.restartButton);
        gamePanel.remove(gamePanel.exitButton);
        gamePanel.restartButton = null;
        gamePanel.exitButton = null;

        // 重繪遊戲畫面
        gamePanel.revalidate();
        gamePanel.repaint();

        // 通知伺服器重置遊戲
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
                    if (json.equals("RESET")) {
                        resetGame();
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

    private void resetGame() {
        gameOver = false;
        winnerId = -1; // 重置獲勝者ID

        // 重置遊戲狀態
        for (PlayerState player : players) {
            player.health = 100; // 重置血量
            player.bullets.clear(); // 清除子彈
        }

        // 重繪遊戲畫面
        gamePanel.revalidate();
        gamePanel.repaint();
    }

    static class PlayerState {
        int userId;
        int x, y;
        int health;
        int playerColor;
        int bulletColor;
        List<Bullet> bullets;
    }

    static class Bullet {
        int x, y;
    }

    static class GameState {
        List<PlayerState> players;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Client client = new Client();
            client.setVisible(true);
        });
    }
}