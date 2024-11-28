import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

public class Client2 extends JFrame {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    private char[][] gameMap = new char[5][5];
    private PrintWriter out;

    public Client2() {
        setTitle("爆爆王");
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // 初始化畫布
        GamePanel gamePanel = new GamePanel();
        add(gamePanel);

        // 鍵盤控制
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                String command = null;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W -> command = "UP";
                    case KeyEvent.VK_A -> command = "LEFT";
                    case KeyEvent.VK_S -> command = "DOWN";
                    case KeyEvent.VK_D -> command = "RIGHT";
                    case KeyEvent.VK_UP -> command = "UP";
                    case KeyEvent.VK_LEFT -> command = "LEFT";
                    case KeyEvent.VK_DOWN -> command = "DOWN";
                    case KeyEvent.VK_RIGHT -> command = "RIGHT";
                    case KeyEvent.VK_SPACE -> command = "BOMB";
                }
                if (command != null && out != null) {
                    out.println(command);
                }
            }
        });

        // 連線至伺服器
        connectToServer(gamePanel);
    }

    private void connectToServer(GamePanel gamePanel) {
        try {
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // 接收伺服器訊息並更新遊戲地圖
            new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        updateMap(serverMessage);
                        gamePanel.repaint();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "無法連線至伺服器", "錯誤", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void updateMap(String serverMessage) {
        String[] rows = serverMessage.split("\n");
        for (int i = 0; i < gameMap.length; i++) {
            gameMap[i] = rows[i].toCharArray();
        }
    }

    private class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int cellSize = getWidth() / gameMap.length;

            for (int i = 0; i < gameMap.length; i++) {
                for (int j = 0; j < gameMap[i].length; j++) {
                    char cell = gameMap[i][j];
                    switch (cell) {
                        case ' ' -> g.setColor(Color.WHITE); // 空地
                        case '#' -> g.setColor(Color.GRAY); // 牆壁
                        case 'P' -> g.setColor(Color.BLUE); // 玩家
                        case 'B' -> g.setColor(Color.RED); // 炸彈
                    }
                    g.fillRect(j * cellSize, i * cellSize, cellSize, cellSize);
                    g.setColor(Color.BLACK);
                    g.drawRect(j * cellSize, i * cellSize, cellSize, cellSize);
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Client2 client = new Client2();
            client.setVisible(true);
        });
    }
}
