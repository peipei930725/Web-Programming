import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.Socket;

public class ClientGUI extends JFrame {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 5000;

    private Socket socket;
    private PrintWriter out;

    public ClientGUI() {
        super("Client GUI");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // 視窗置中

        // 建立連線
        try {
            socket = new Socket(HOST, PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("已連上伺服器！");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "無法連接伺服器", "連接錯誤", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // 提示訊息 Label
        JLabel label = new JLabel("請按 w, a, s, d 或空白鍵 (space) 發送訊號至伺服器", SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);

        // 將整個視窗的事件焦點放在 frame 上，方便直接偵測鍵盤輸入
        setFocusable(true);
        requestFocusInWindow();

        // Key Listener
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                char c = e.getKeyChar();
                // 檢查是否是 w, a, s, d 或空白鍵
                if (c == 'w' || c == 'a' || c == 's' || c == 'd') {
                    sendToServer(String.valueOf(c));
                } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    sendToServer(" ");
                }
            }
        });
    }

    private void sendToServer(String msg) {
        if (out != null) {
            out.println(msg);
            out.flush();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientGUI client = new ClientGUI();
            client.setVisible(true);
        });
    }
}
