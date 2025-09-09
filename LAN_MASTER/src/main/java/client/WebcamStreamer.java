package client;

import java.io.*;
import java.net.*;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class WebcamStreamer {
    private static boolean streaming = false;
    private static Thread streamThread;

    public static void startStreaming(String serverIP, int port) {
        if (streaming) return;
        streaming = true;

        streamThread = new Thread(() -> {
            try (Socket socket = new Socket(serverIP, port);
                 OutputStream out = socket.getOutputStream();
                 DataOutputStream dos = new DataOutputStream(out)) {

                Webcam webcam = Webcam.getDefault();
                webcam.setViewSize(WebcamResolution.QVGA.getSize());
                webcam.open();

                while (streaming) {
                    BufferedImage image = webcam.getImage();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(image, "jpg", baos);
                    byte[] bytes = baos.toByteArray();

                    dos.writeInt(bytes.length);
                    dos.write(bytes);
                    dos.flush();

                    Thread.sleep(100); // ~10 fps
                }

                webcam.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        streamThread.start();
    }

    public static void stopStreaming() {
        streaming = false;
        if (streamThread != null) {
            try {
                streamThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
