package Client.src.main;

import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class GanePanel extends JPanel implements Runnable {

    // Screen settings
    final int originalTileSize = 16;
    final int scale = 3;
    final int tileSize = originalTileSize * scale;

    final int maxScreeRow = 15;
    final int maxScreenCol = 20;
    final int screenWidth = maxScreenCol * tileSize;
    final int screenHeight = maxScreeRow * tileSize; 

    KeyHander keyHander = new KeyHander();
    Thread gameThread;

    int playerX = 100;
    int playerY = 100;
    int playerSpeed = 4;
    int Fps = 60;

    public GanePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
        this.addKeyListener(keyHander);
        this.setFocusable(true);
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() { // Game loop
        
        double drawInterval = 1000000000 / Fps;
        double nextDrawTime = System.nanoTime() + drawInterval;

        while(gameThread != null) {
            // System.out.println("Game is running...");
            // update information
            // draw the screen

            update();
            repaint();
            
            try{
                double remainingTime = nextDrawTime - System.nanoTime();
                remainingTime = remainingTime / 1000000;

                if (remainingTime < 0) {
                    remainingTime = 0 ;
                }
                Thread.sleep((long) remainingTime);
                nextDrawTime += drawInterval;
            }catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void update() {
        // update information
        if(keyHander.up && playerY - playerSpeed >= 0) {
            playerY -= playerSpeed;
        }
        if(keyHander.down && playerY + playerSpeed + tileSize <= screenHeight) {
            playerY += playerSpeed;
        }
        if(keyHander.left && playerX - playerSpeed >= 0) {
            playerX -= playerSpeed;
        }
        if(keyHander.right && playerX + playerSpeed + tileSize <= screenWidth) {
            playerX += playerSpeed;
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
    
        g2.setColor(Color.WHITE);
        g2.fillRect(playerX, playerY, tileSize, tileSize);
        g2.dispose();
    }

}
