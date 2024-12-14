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
                sendCommandToServer("PRESS " + e.getKeyChar());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                sendCommandToServer("RELEASE " + e.getKeyChar());
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
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // 繪製背景
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());

            // 繪製玩家和子彈
            for (PlayerState player : players) {
                // 玩家
                g.setColor(new Color(player.playerColor)); // 使用伺服器指定的顏色
                g.fillOval(player.x, player.y, 40, 40);

                // 子彈
                g.setColor(new Color(player.bulletColor)); // 使用伺服器指定的子彈顏色
                for (Bullet bullet : player.bullets) {
                    g.fillOval(bullet.x, bullet.y, 10, 10);
                }
            }
        }
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
                    GameState gameState = gson.fromJson(json, GameState.class);
                    players = gameState.players; // 更新玩家狀態
                    gamePanel.repaint();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 與伺服器相同的類別結構
    static class PlayerState {
        int userId;
        int x, y;
        int playerColor; // RGB 顏色
        int bulletColor; // RGB 顏色
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
