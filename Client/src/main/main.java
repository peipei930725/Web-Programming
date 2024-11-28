package Client.src.main;
import javax.swing.JFrame;

public class main {
    public static void main(String[] args) {
        JFrame window = new JFrame("爆爆王");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setSize(1600, 1200);
        window.setLocationRelativeTo(null);

        GanePanel gamePanel = new GanePanel();
        window.add(gamePanel);

        window.pack();

        window.setResizable(false);
        window.setVisible(true);

        gamePanel.startGameThread();
    }
}
