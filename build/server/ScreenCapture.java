import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;

/** Function 3: Chup man hinh */
public class ScreenCapture {

    public static void capture(DataOutputStream out) throws IOException {
        try {
            Robot robot = new Robot();
            Rectangle screen = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage img = robot.createScreenCapture(screen);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", baos);
            byte[] bytes = baos.toByteArray();

            // Binary protocol: marker + size + data
            out.writeUTF("BINARY");
            out.writeInt(bytes.length);
            out.write(bytes);
            out.flush();
        } catch (AWTException e) {
            out.writeUTF(JsonUtil.textResponse("ERROR", "Khong the chup man hinh: " + e.getMessage()));
            out.flush();
        }
    }
}
