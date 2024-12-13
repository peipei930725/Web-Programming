// Server Code (Java with Socket Programming)
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

class GameObject {
    int x;
    int y;
    String type;

    public GameObject(int x, int y, String type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }
}

class Player {
    int x = 250;
    int score = 0;
    int lives = 3;
}

public class GameServer {
    private static final int PORT = 3000;
    private static Map<String, Player> players = new ConcurrentHashMap<>();
    private static List<GameObject> objects = Collections.synchronizedList(new ArrayList<>());
    private static String winner = null;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server is running on port " + PORT);

        // Spawn objects periodically
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
        executor.scheduleAtFixedRate(() -> {
            if (winner == null) {
                generateObject();
            }
        }, 0, 1, TimeUnit.SECONDS);

        executor.scheduleAtFixedRate(() -> {
            if (winner == null) {
                moveObjects();
            }
        }, 0, 50, TimeUnit.MILLISECONDS);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(new ClientHandler(clientSocket)).start();
        }
    }

    private static void generateObject() {
        String type = Math.random() > 0.7 ? "bomb" : "coin";
        GameObject object = new GameObject(new Random().nextInt(500), 0, type);
        objects.add(object);
    }

    private static void moveObjects() {
        synchronized (objects) {
            for (Iterator<GameObject> iterator = objects.iterator(); iterator.hasNext();) {
                GameObject obj = iterator.next();
                obj.y += 5;
                if (obj.y > 500) {
                    iterator.remove();
                }
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private String playerId;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                playerId = UUID.randomUUID().toString();
                players.put(playerId, new Player());
                System.out.println("Player connected: " + playerId);

                out.println("CONNECTED:" + playerId);
                while (winner == null) {
                    String input = in.readLine();
                    if (input == null) continue;

                    if (input.startsWith("MOVE:")) {
                        handleMove(input.split(":")[1]);
                    } else if (input.startsWith("CATCH:")) {
                        handleCatch(Integer.parseInt(input.split(":")[1]));
                    }

                    out.println(serializeGameState());
                }

                if (playerId.equals(winner)) {
                    out.println("WINNER");
                } else {
                    out.println("LOSER");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                players.remove(playerId);
                System.out.println("Player disconnected: " + playerId);
            }
        }

        private void handleMove(String direction) {
            Player player = players.get(playerId);
            if (direction.equals("LEFT")) {
                player.x = Math.max(player.x - 20, 0);
            } else if (direction.equals("RIGHT")) {
                player.x = Math.min(player.x + 20, 500);
            }
        }

        private void handleCatch(int objectId) {
            synchronized (objects) {
                if (objectId >= 0 && objectId < objects.size()) {
                    GameObject obj = objects.get(objectId);
                    Player player = players.get(playerId);

                    if (obj.type.equals("coin")) {
                        player.score++;
                        if (player.score >= 25) {
                            winner = playerId;
                        }
                    } else if (obj.type.equals("bomb")) {
                        player.lives--;
                        if (player.lives <= 0) {
                            winner = players.keySet().stream().filter(id -> !id.equals(playerId)).findFirst().orElse(null);
                        }
                    }

                    objects.remove(objectId);
                }
            }
        }

        private String serializeGameState() {
            StringBuilder sb = new StringBuilder();
            sb.append("PLAYERS:");
            players.forEach((id, player) -> {
                sb.append(id).append(",").append(player.x).append(",").append(player.score).append(",").append(player.lives).append(";");
            });

            sb.append("OBJECTS:");
            synchronized (objects) {
                for (int i = 0; i < objects.size(); i++) {
                    GameObject obj = objects.get(i);
                    sb.append(i).append(",").append(obj.x).append(",").append(obj.y).append(",").append(obj.type).append(";");
                }
            }

            return sb.toString();
        }
    }
}