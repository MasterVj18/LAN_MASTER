package client.control;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class RemoteControlServer {
    private volatile boolean running = true;

    public void start() throws Exception {
        Robot robot = new Robot();
        ServerSocket controlSocket = new ServerSocket(5000);
        ServerSocket screenSocket = new ServerSocket(6000);

        System.out.println("Waiting for master control connection...");

        Socket controlClient = controlSocket.accept();
        BufferedReader in = new BufferedReader(new InputStreamReader(controlClient.getInputStream()));
        System.out.println("Master connected.");

        // Command handling
        new Thread(() -> {
            try {
                String line;
                while (running && (line = in.readLine()) != null) {
                    if (line.startsWith("MOVE:")) {
                        String[] parts = line.substring(5).split(",");
                        int x = Integer.parseInt(parts[0]);
                        int y = Integer.parseInt(parts[1]);
                        robot.mouseMove(x, y);
                    } else if (line.startsWith("CLICK:")) {
                        String[] parts = line.split(":")[1].split(",");
                        int btn = Integer.parseInt(parts[0]);
                        robot.mousePress(btn);
                        robot.mouseRelease(btn);
                    } else if (line.startsWith("KEY:")) {
                        String[] parts = line.split(":")[1].split(",");
                        int key = Integer.parseInt(parts[0]);
                        boolean press = Boolean.parseBoolean(parts[1]);
                        if (press) robot.keyPress(key);
                        else robot.keyRelease(key);
                    } else if (line.equals("STOP_REMOTE_CONTROL")) {
                        System.out.println("Stopping remote control...");
                        running = false;
                        break;
                    }
                }
            } catch (Exception e) {
                if (running) e.printStackTrace();
            }
        }).start();

        // Screen sending loop
        while (running) {
            Socket screenClient = screenSocket.accept();
            OutputStream out = screenClient.getOutputStream();
            BufferedImage img = robot.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
            ImageIO.write(img, "jpg", out);
            out.close();
            screenClient.close();
            Thread.sleep(100);
        }

        controlSocket.close();
        screenSocket.close();
    }

    public static void main(String[] args) throws Exception {
        new RemoteControlServer().start();
    }
}
