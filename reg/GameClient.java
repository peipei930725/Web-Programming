import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import javax.swing.*;

public class GameClient {
    private static final int WIDTH = 500;
    private static final int HEIGHT = 500;
    private String playerId;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private JPanel panel;
    private int playerX = 250;
    private int score = 0;
    private int lives = 3;
    private java.util.List<GameObject> objects = new ArrayList<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new GameClient().start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void start() throws IOException {
        socket = new Socket("localhost", 3000);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        JFrame frame = new JFrame("Catch Game");
        panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Color.BLUE);
                g.fillRect(playerX, HEIGHT - 50, 50, 10);

                synchronized (objects) {
                    for (GameObject obj : objects) {
                        g.setColor(obj.type.equals("coin") ? Color.YELLOW : Color.RED);
                        g.fillOval(obj.x, obj.y, 10, 10);
                    }
                }

                g.setColor(Color.BLACK);
                g.drawString("Score: " + score, 10, 10);
                g.drawString("Lives: " + lives, 10, 25);
            }
        };

        panel.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        frame.add(panel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    out.println("MOVE:LEFT");
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    out.println("MOVE:RIGHT");
                } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    catchObject();
                }
            }
        });
        panel.setFocusable(true);

        new Thread(this::listenToServer).start();
    }

    private void listenToServer() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("CONNECTED:")) {
                    playerId = line.split(":")[1];
                } else if (line.startsWith("PLAYERS:")) {
                    parseGameState(line);
                } else if (line.equals("WINNER")) {
                    JOptionPane.showMessageDialog(panel, "You Win!");
                    System.exit(0);
                } else if (line.equals("LOSER")) {
                    JOptionPane.showMessageDialog(panel, "You Lose!");
                    System.exit(0);
                }
                panel.repaint();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseGameState(String line) {
        objects.clear();
        String[] parts = line.split("OBJECTS:");
        if (parts.length > 1) {
            String[] objectData = parts[1].split(";");
            for (String obj : objectData) {
                if (!obj.isEmpty()) {
                    String[] fields = obj.split(",");
                    objects.add(new GameObject(Integer.parseInt(fields[1]), Integer.parseInt(fields[2]), fields[3]));
                }
            }
        }

        String[] playerData = parts[0].replace("PLAYERS:", "").split(";");
        for (String player : playerData) {
            if (!player.isEmpty() && player.startsWith(playerId)) {
                String[] fields = player.split(",");
                playerX = Integer.parseInt(fields[1]);
                score = Integer.parseInt(fields[2]);
                lives = Integer.parseInt(fields[3]);
            }
        }
    }

    private void catchObject() {
        synchronized (objects) {
            for (int i = 0; i < objects.size(); i++) {
                GameObject obj = objects.get(i);
                if (Math.abs(obj.x - playerX) < 50 && obj.y > HEIGHT - 60) {
                    out.println("CATCH:" + i);
                    break;
                }
            }
        }
    }

    private static class GameObject {
        int x, y;
        String type;

        public GameObject(int x, int y, String type) {
            this.x = x;
            this.y = y;
            this.type = type;
        }
    }
}
