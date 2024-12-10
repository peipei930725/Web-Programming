package src.client.main;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.util.UUID;
import javax.swing.JFrame;

public class Client {
    public static void main(String[] args) {
        JFrame window = new JFrame("爆爆王");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setSize(1600, 1200);
        window.setLocationRelativeTo(null);

        GamePanel gamePanel = new GamePanel();
        window.add(gamePanel);

        window.pack();

        window.setResizable(false);
        window.setVisible(true);

        gamePanel.startGameThread();

        try {
            Socket socket = new Socket("localhost", 12345);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // 檢查命令列參數，若提供了 token，則使用該 token；否則生成一個隨機的 token
            String token;
            if (args.length > 0) {
                token = args[0];
            } else {
                token = UUID.randomUUID().toString();
            }
            out.println(token);

            // 檢查伺服器回應
            String response = in.readLine();

            if (response != null) {
                String[] parts = response.split(" ");
                if ("ok".equals(parts[0])) {} 
                else if (parts.length < 1) {
                    System.out.println("Syntax error: " + response);
                    System.out.println("it should be :<states> <message>");
                    socket.close();
                    return;
                }
                if ("Er0".equals(parts[0])) {
                    System.out.println("Error: " + response);
                    window.dispose();
                    socket.close();
                    System.exit(0);
                    return;
                }
                // 你可以在這裡處理其他回應訊息
            }

            gamePanel.setSocket(socket, out, in);
            window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    out.println(token + " close");
                    try {
                        socket.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });
        } catch (ConnectException e) {
            System.out.println("伺服器未回應");
            window.dispose();
            System.exit(0);;
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("伺服器未回應");
            e.printStackTrace();
        }
    }
}