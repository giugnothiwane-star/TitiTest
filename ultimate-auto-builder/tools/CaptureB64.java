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
    static ByteArrayOutputStream jpeg(BufferedImage src, int width, float quality) throws java.io.IOException {
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
        p.setCompressionQuality(quality);
        try (MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(out)) {
            w.setOutput(ios);
            w.write(null, new IIOImage(dst, null, null), p);
        }
        w.dispose();
        return out;
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : "run/screenshots");
        int width = args.length > 1 ? Integer.parseInt(args[1]) : 960;
        String json = args.length > 2 ? args[2] : null;
        String sha = args.length > 3 ? args[3] : "";
        StringBuilder ann = new StringBuilder();
        File[] files = dir.listFiles((d, n) -> n.endsWith(".png"));
        if (files == null || files.length == 0) {
            System.out.println("Aucune capture dans " + dir);
            return;
        }
        Arrays.sort(files);
        if (files.length > 50) {
            files = Arrays.copyOf(files, 50);
        }
        for (File f : files) {
            BufferedImage src = ImageIO.read(f);
            // Qualité puis largeur réduites jusqu'à tenir dans une annotation (64 Ko de texte).
            ByteArrayOutputStream out = null;
            int wTry = width;
            float q = 0.8f;
            while (true) {
                out = jpeg(src, wTry, q);
                if (out.size() * 4 / 3 + 4 <= 63_000 || wTry <= 320) {
                    break;
                }
                if (q > 0.45f) {
                    q -= 0.1f;
                } else {
                    wTry = wTry * 85 / 100;
                }
            }
            String b64 = Base64.getEncoder().encodeToString(out.toByteArray());
            System.out.println("Capture " + f.getName() + " : " + out.size() + " octets en JPEG");
            if (b64.length() > 64_000) {
                System.out.println("  trop grande pour une annotation, ignorée");
                continue;
            }
            // Une annotation par image : relisible via l'API publique des « check runs ».
            ann.append(ann.isEmpty() ? "" : ",").append("{\"path\":\".github/workflows/ultimate-auto-builder.yml\",\"start_line\":1,")
                    .append("\"end_line\":1,\"annotation_level\":\"notice\",\"title\":\"").append(f.getName())
                    .append("\",\"message\":\"").append(b64).append("\"}");
        }
        if (json != null) {
            String body = "{\"name\":\"Captures en jeu\",\"head_sha\":\"" + sha + "\",\"status\":\"completed\",\"conclusion\":\"neutral\","
                    + "\"output\":{\"title\":\"Captures d'écran du client\",\"summary\":\"Captures JPEG encodées en base64 dans les annotations "
                    + "(vérification visuelle automatique).\",\"annotations\":[" + ann + "]}}";
            java.nio.file.Files.writeString(java.nio.file.Path.of(json), body);
        }
    }
}
