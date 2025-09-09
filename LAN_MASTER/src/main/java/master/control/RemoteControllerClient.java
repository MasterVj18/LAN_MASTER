package master.control;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.net.Socket;

public class RemoteControllerClient extends JFrame {
    private PrintWriter out;
    private JLabel screenLabel;
    private int remoteWidth = 1366;
    private int remoteHeight = 768;
    private Dimension localScreen;
    private volatile boolean running = true; // For Stop function

    public RemoteControllerClient(String serverIP) throws Exception {
        setTitle("Remote Control Viewer");
        setSize(800, 600);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); // So we can close without killing master
        screenLabel = new JLabel();
        add(new JScrollPane(screenLabel), BorderLayout.CENTER);
        setVisible(true);

        Socket controlSocket = new Socket(serverIP, 5000);
        out = new PrintWriter(controlSocket.getOutputStream(), true);
        localScreen = Toolkit.getDefaultToolkit().getScreenSize();

        // Mouse move
        screenLabel.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                int sx = (int) ((e.getX() / (double) screenLabel.getWidth()) * remoteWidth);
                int sy = (int) ((e.getY() / (double) screenLabel.getHeight()) * remoteHeight);
                out.println("MOVE:" + sx + "," + sy);
            }
        });

        // Mouse click
        screenLabel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                int button = e.getButton() == MouseEvent.BUTTON1 ? InputEvent.BUTTON1_DOWN_MASK :
                        e.getButton() == MouseEvent.BUTTON3 ? InputEvent.BUTTON3_DOWN_MASK : 0;
                out.println("CLICK:" + button);
            }
        });

        // Keyboard events on the JFrame
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                out.println("KEY:" + e.getKeyCode() + ",true");
            }

            public void keyReleased(KeyEvent e) {
                out.println("KEY:" + e.getKeyCode() + ",false");
            }
        });

        // Always grab focus for keyboard
        setFocusable(true);
        requestFocusInWindow();

        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                requestFocusInWindow();
            }
        });

        // Screen receiving thread
        new Thread(() -> {
            while (running) {
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
                    // Stop when running == false
                    if (running) e.printStackTrace();
                }
            }
        }).start();
    }

    // Call this to stop remote control cleanly
    public void stopRemoteControl() {
        running = false;
        dispose();
    }

    public static void main(String[] args) throws Exception {
        RemoteControllerClient rcc = new RemoteControllerClient("10.1.50.150");
    }
}
