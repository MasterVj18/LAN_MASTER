package mousemove;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import javax.imageio.ImageIO;

public class RemoteControlServer {
    public static void main(String[] args) throws Exception {
        Robot robot = new Robot();
        ServerSocket controlSocket = new ServerSocket(5000); // For commands
        ServerSocket screenSocket = new ServerSocket(6000);  // For screen sharing

        System.out.println("Waiting for client...");

        Socket controlClient = controlSocket.accept();
        BufferedReader in = new BufferedReader(new InputStreamReader(controlClient.getInputStream()));
        System.out.println("Client connected.");

        // Command Handling
        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
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
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // Screen Sending Loop
        while (true) {
            Socket screenClient = screenSocket.accept();
            OutputStream out = screenClient.getOutputStream();
            BufferedImage img = robot.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
            ImageIO.write(img, "jpg", out);
            out.close();
            screenClient.close();
            Thread.sleep(100);
        }
    }
}
