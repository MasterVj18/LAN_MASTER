package mousemove;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import javax.imageio.ImageIO;
import javax.swing.*;

public class RemoteControllerClient extends JFrame {
    private PrintWriter out;
    private JLabel screenLabel;
    private int remoteWidth = 1366;
    private int remoteHeight = 768;
    private Dimension localScreen;

    public RemoteControllerClient(String serverIP) throws Exception {
        setTitle("Remote Control Viewer");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        screenLabel = new JLabel();
        add(screenLabel);
        setVisible(true);

        Socket controlSocket = new Socket(serverIP, 5000);
        out = new PrintWriter(controlSocket.getOutputStream(), true);
        localScreen = Toolkit.getDefaultToolkit().getScreenSize();

        screenLabel.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                int sx = (int) ((e.getX() / (double) screenLabel.getWidth()) * remoteWidth);
                int sy = (int) ((e.getY() / (double) screenLabel.getHeight()) * remoteHeight);
                out.println("MOVE:" + sx + "," + sy);
            }
        });

        screenLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int button = e.getButton() == MouseEvent.BUTTON1 ? InputEvent.BUTTON1_DOWN_MASK :
                        e.getButton() == MouseEvent.BUTTON3 ? InputEvent.BUTTON3_DOWN_MASK : 0;
                out.println("CLICK:" + button);
            }
        });

        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                out.println("KEY:" + e.getKeyCode() + ",true");
            }

            public void keyReleased(KeyEvent e) {
                out.println("KEY:" + e.getKeyCode() + ",false");
            }
        });

        screenLabel.setFocusable(true);
        screenLabel.requestFocusInWindow();

        // Screen Receiving
        new Thread(() -> {
            while (true) {
                try {
                    Socket screenSocket = new Socket(serverIP, 6000);
                    BufferedImage img = ImageIO.read(screenSocket.getInputStream());
                    if (img != null) {
                        remoteWidth = img.getWidth();
                        remoteHeight = img.getHeight();
                        screenLabel.setIcon(new ImageIcon(img));
                    }
                    screenSocket.close();
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void main(String[] args) throws Exception {
        new RemoteControllerClient("10.1.50.150"); // Replace with server IP
    }
}

