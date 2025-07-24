package client;

import org.bytedeco.javacv.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import java.net.Socket;

public class WebcamHandler {
    public static void captureImage(Socket socket) {
        try {
            OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
            grabber.start();
            Frame frame = grabber.grab();
            Java2DFrameConverter converter = new Java2DFrameConverter();
            BufferedImage img = converter.convert(frame);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", baos);
            OutputStream out = socket.getOutputStream();
            out.write(baos.toByteArray());
            grabber.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
