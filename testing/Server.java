import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private static final int PORT = 5000;
    private static final int MAX_CLIENTS = 4;
    private static int clientCount = 0;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("伺服器已啟動，等待連接中...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                int userId = clientCount % MAX_CLIENTS; 
                clientCount++;

                System.out.println("使用者 " + userId + " 已連接，來自：" 
                                   + clientSocket.getRemoteSocketAddress());

                // 建立一個執行緒處理該使用者的訊息
                ClientHandler handler = new ClientHandler(clientSocket, userId);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private int userId;

        public ClientHandler(Socket socket, int userId) {
            this.socket = socket;
            this.userId = userId;
        }

        @Override
        public void run() {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    // 過濾只保留 w, a, s, d 和空白鍵
                    // 此範例假設使用者每次只傳一個字元，若傳多字元可自行處理
                    if (line.equals("w") || line.equals("a") || line.equals("s") || line.equals("d") || line.equals(" ")) {
                        System.out.println("使用者 " + userId + " 輸入：" + (line.equals(" ") ? "空白鍵" : line));
                    }
                }
            } catch (IOException e) {
                System.out.println("使用者 " + userId + " 連線中斷。");
            }
        }
    }
}
