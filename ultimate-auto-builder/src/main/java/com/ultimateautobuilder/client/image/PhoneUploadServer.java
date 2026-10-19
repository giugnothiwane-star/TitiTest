package com.ultimateautobuilder.client.image;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.core.image.ImageLoader;
import com.ultimateautobuilder.core.importer.ImportException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Réception d'images depuis un téléphone ou une tablette du même réseau local, sans service
 * extérieur : le jeu ouvre temporairement une petite page web protégée par un jeton aléatoire.
 * Démarrage uniquement à la demande du joueur, arrêt automatique après 10 minutes, une connexion
 * à la fois, taille bornée, contenu vérifié comme image réelle avant d'être enregistré.
 */
public final class PhoneUploadServer {
    private static final long LIFETIME_MS = 10 * 60 * 1000L;
    private static final int MAX_HEADER = 8192;

    private static PhoneUploadServer instance;

    private final ServerSocket socket;
    private final String token;
    private final String url;
    private final long expiresAt;
    private final Consumer<Path> onReceived;
    private volatile boolean running = true;

    private PhoneUploadServer(Consumer<Path> onReceived) throws IOException {
        this.socket = new ServerSocket(0, 4);
        this.socket.setSoTimeout(1000);
        byte[] t = new byte[16];
        new SecureRandom().nextBytes(t);
        this.token = HexFormat.of().formatHex(t);
        String host = lanAddress();
        if (host == null) {
            socket.close();
            throw new IOException("Aucun réseau local détecté (Wi-Fi ou Ethernet)");
        }
        this.url = "http://" + host + ":" + socket.getLocalPort() + "/?t=" + token;
        this.expiresAt = System.currentTimeMillis() + LIFETIME_MS;
        this.onReceived = onReceived;
        Thread th = new Thread(this::loop, "UAB-PhoneUpload");
        th.setDaemon(true);
        th.start();
    }

    /** Démarre (ou renvoie) le serveur ; le rappel est exécuté sur le thread du jeu. */
    public static synchronized PhoneUploadServer start(Consumer<Path> onReceived) throws IOException {
        if (instance != null && instance.running) {
            return instance;
        }
        instance = new PhoneUploadServer(onReceived);
        return instance;
    }

    public static synchronized PhoneUploadServer current() {
        return instance != null && instance.running ? instance : null;
    }

    public static synchronized void stopAll() {
        if (instance != null) {
            instance.stop();
            instance = null;
        }
    }

    public String url() {
        return url;
    }

    public long secondsLeft() {
        return Math.max(0, (expiresAt - System.currentTimeMillis()) / 1000);
    }

    public void stop() {
        running = false;
        try {
            socket.close();
        } catch (IOException ignored) {
            // Déjà fermé.
        }
    }

    private void loop() {
        while (running && System.currentTimeMillis() < expiresAt) {
            try (Socket s = socket.accept()) {
                s.setSoTimeout(20_000);
                handle(s);
            } catch (java.net.SocketTimeoutException ignored) {
                // Vérification périodique de l'expiration.
            } catch (IOException e) {
                if (running) {
                    UltimateAutoBuilderMod.LOGGER.debug("Connexion téléphone interrompue : {}", e.getMessage());
                }
            }
        }
        stop();
    }

    private void handle(Socket s) throws IOException {
        InputStream in = s.getInputStream();
        String head = readHead(in);
        if (head == null) {
            respond(s.getOutputStream(), 400, "text/plain", "Requête invalide");
            return;
        }
        String[] lines = head.split("\r\n");
        String[] req = lines[0].split(" ");
        if (req.length < 2) {
            respond(s.getOutputStream(), 400, "text/plain", "Requête invalide");
            return;
        }
        String method = req[0], target = req[1];
        String path = target.contains("?") ? target.substring(0, target.indexOf('?')) : target;
        String query = target.contains("?") ? target.substring(target.indexOf('?') + 1) : "";
        if (!token.equals(param(query, "t"))) {
            respond(s.getOutputStream(), 403, "text/plain", "Lien expiré ou invalide : affichez un nouveau lien dans le jeu");
            return;
        }
        if (method.equals("GET") && path.equals("/")) {
            respond(s.getOutputStream(), 200, "text/html; charset=utf-8", page());
            return;
        }
        if (!method.equals("POST") || !path.equals("/upload")) {
            respond(s.getOutputStream(), 404, "text/plain", "Introuvable");
            return;
        }
        long length = -1;
        for (String l : lines) {
            if (l.toLowerCase(Locale.ROOT).startsWith("content-length:")) {
                try {
                    length = Long.parseLong(l.substring(15).trim());
                } catch (NumberFormatException e) {
                    length = -1;
                }
            }
        }
        if (length <= 0 || length > ImageLoader.MAX_FILE_BYTES) {
            respond(s.getOutputStream(), 413, "text/plain", "Image absente ou trop volumineuse (maximum 25 Mo)");
            return;
        }
        byte[] body = in.readNBytes((int) length);
        if (body.length != length) {
            respond(s.getOutputStream(), 400, "text/plain", "Envoi incomplet");
            return;
        }
        String ext = extension(body);
        if (ext == null) {
            respond(s.getOutputStream(), 415, "text/plain", "Format non pris en charge : utilisez PNG ou JPEG (les photos HEIC de l'iPhone sont "
                    + "converties automatiquement par Safari)");
            return;
        }
        try {
            ImageLoader.load(body); // décodage complet : rejette les fichiers corrompus ou piégés
        } catch (ImportException e) {
            respond(s.getOutputStream(), 422, "text/plain", e.getMessage());
            return;
        }
        ImageFiles.ensureDirs();
        Path file = ImageFiles.images().resolve("telephone_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ext);
        Files.write(file, body);
        respond(s.getOutputStream(), 200, "text/plain", "Image reçue par Minecraft !");
        net.minecraft.client.Minecraft.getInstance().execute(() -> onReceived.accept(file));
    }

    private static String readHead(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int state = 0;
        while (buf.size() < MAX_HEADER) {
            int b = in.read();
            if (b < 0) {
                return null;
            }
            buf.write(b);
            // Fin des en-têtes : \r\n\r\n
            state = (b == '\r' && (state == 0 || state == 2)) ? state + 1 : (b == '\n' && (state == 1 || state == 3)) ? state + 1 : 0;
            if (state == 4) {
                return buf.toString(StandardCharsets.ISO_8859_1);
            }
        }
        return null;
    }

    private static String param(String query, String key) {
        for (String kv : query.split("&")) {
            int eq = kv.indexOf('=');
            if (eq > 0 && kv.substring(0, eq).equals(key)) {
                return kv.substring(eq + 1);
            }
        }
        return null;
    }

    /** Extension déduite des octets réels (jamais du nom envoyé). */
    static String extension(byte[] b) {
        if (b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return ".png";
        }
        if (b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8) {
            return ".jpg";
        }
        if (b.length > 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F') {
            return ".gif";
        }
        if (b.length > 2 && b[0] == 'B' && b[1] == 'M') {
            return ".bmp";
        }
        return null;
    }

    private static void respond(OutputStream out, int code, String type, String body) throws IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        String status = switch (code) {
            case 200 -> "OK";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 413 -> "Payload Too Large";
            case 415 -> "Unsupported Media Type";
            case 422 -> "Unprocessable Entity";
            default -> "Bad Request";
        };
        String head = "HTTP/1.1 " + code + " " + status + "\r\nContent-Type: " + type + "\r\nContent-Length: " + data.length
                + "\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nConnection: close\r\n\r\n";
        out.write(head.getBytes(StandardCharsets.ISO_8859_1));
        out.write(data);
        out.flush();
    }

    private String page() {
        return "<!doctype html><html lang=fr><head><meta charset=utf-8><meta name=viewport content='width=device-width,initial-scale=1'>"
                + "<title>Ultimate Auto Builder</title><style>body{font-family:sans-serif;background:#16202a;color:#eef;margin:0;padding:24px}"
                + "h1{font-size:20px}button,input{font-size:18px;margin:12px 0;width:100%}button{padding:14px;border:0;border-radius:8px;"
                + "background:#4fb3d9;color:#012}#s{margin-top:16px;font-size:16px}</style></head><body><h1>Envoyer une image à Minecraft</h1>"
                + "<p>Choisissez une photo ou une image (PNG ou JPEG, 25 Mo maximum).</p><input type=file id=f accept='image/png,image/jpeg,image/*'>"
                + "<button id=b>Envoyer</button><div id=s></div><script>document.getElementById('b').onclick=async()=>{const f=document"
                + ".getElementById('f').files[0];const s=document.getElementById('s');if(!f){s.textContent='Aucune image choisie';return;}"
                + "s.textContent='Envoi en cours…';try{const r=await fetch('/upload?t=" + token + "',{method:'POST',body:f});s.textContent="
                + "await r.text();}catch(e){s.textContent='Échec : '+e;}};</script></body></html>";
    }

    /** Première adresse IPv4 privée d'une interface active (réseau local). */
    static String lanAddress() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address && a.isSiteLocalAddress()) {
                        return a.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            UltimateAutoBuilderMod.LOGGER.debug("Interfaces réseau illisibles", e);
        }
        return null;
    }
}
