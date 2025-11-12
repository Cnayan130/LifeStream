package top.principlecreativity.lifestream.util;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageResizeUtil {

    // 将图片裁剪并缩放为正方形
    public static byte[] resizeImage(InputStream inputStream, int outputSize, String formatName) throws IOException {
        BufferedImage originalImage = ImageIO.read(inputStream);
        if (originalImage == null) {
            throw new IOException("无法读取图像数据");
        }

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // 计算裁剪区域（居中裁剪正方形）
        int size = Math.min(originalWidth, originalHeight);
        int x = (originalWidth - size) / 2;
        int y = (originalHeight - size) / 2;

        BufferedImage croppedImage = originalImage.getSubimage(x, y, size, size);

        // 缩放到目标尺寸 (例如 256x256)
        Image resultingImage = croppedImage.getScaledInstance(outputSize, outputSize, Image.SCALE_SMOOTH);
        BufferedImage outputImage = new BufferedImage(outputSize, outputSize, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(resultingImage, 0, 0, null);
        g2d.dispose();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(outputImage, formatName, os);
        return os.toByteArray();
    }
}