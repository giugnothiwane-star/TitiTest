import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.Base64;

/**
 * Affiche les captures d'écran de la CI dans le journal, réduites en JPEG et encodées en base64
 * (une ligne par image, entre des balises) : elles peuvent ainsi être relues sans télécharger d'artefact.
 */
public class CaptureB64 {
    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : "run/screenshots");
        int width = args.length > 1 ? Integer.parseInt(args[1]) : 960;
        File[] files = dir.listFiles((d, n) -> n.endsWith(".png"));
        if (files == null || files.length == 0) {
            System.out.println("Aucune capture dans " + dir);
            return;
        }
        Arrays.sort(files);
        for (File f : files) {
            BufferedImage src = ImageIO.read(f);
            int h = src.getHeight() * width / src.getWidth();
            BufferedImage dst = new BufferedImage(width, h, BufferedImage.TYPE_INT_RGB);
            var g = dst.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, width, h, null);
            g.dispose();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageWriter w = ImageIO.getImageWritersByFormatName("jpg").next();
            ImageWriteParam p = w.getDefaultWriteParam();
            p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            p.setCompressionQuality(0.72f);
            try (MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(out)) {
                w.setOutput(ios);
                w.write(null, new IIOImage(dst, null, null), p);
            }
            w.dispose();
            System.out.println("===CAPTURE " + f.getName() + " " + out.size() + "===");
            System.out.println(Base64.getEncoder().encodeToString(out.toByteArray()));
            System.out.println("===FIN===");
        }
    }
}
