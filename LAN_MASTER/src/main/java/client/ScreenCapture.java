package client;
import java.awt.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
import java.io.*;
import java.net.*;

public class ScreenCapture {
    static void capture(Socket socket) {
        try {
            Robot robot = new Robot();
            Rectangle screen = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage image = robot.createScreenCapture(screen);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            byte[] imageBytes = baos.toByteArray();

            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            dos.writeInt(imageBytes.length); // send length first
            dos.write(imageBytes);           // then the image
            dos.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    static volatile boolean streaming = false;

    static void startScreenStreaming() {
        streaming = true;
        new Thread(() -> {
            try {
                while (streaming) {
                    Socket imageSocket = new Socket("localhost", 9091);
                    DataOutputStream dos = new DataOutputStream(imageSocket.getOutputStream());

                    Robot robot = new Robot();
                    Rectangle screen = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
                    BufferedImage image = robot.createScreenCapture(screen);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(image, "jpg", baos);
                    byte[] imageBytes = baos.toByteArray();

                    dos.writeInt(imageBytes.length);
                    dos.write(imageBytes);
                    dos.flush();

                    dos.close();
                    imageSocket.close();

                    Thread.sleep(200); // adjust frame rate (e.g., 5 fps)
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    static void stopScreenStreaming() {
        streaming = false;
    }
}
