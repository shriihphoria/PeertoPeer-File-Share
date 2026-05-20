import java.net.*;
import java.io.*;

/*
 Simple server that listens for "GET <filename>\n"
 If file exists in shared/ it replies "FOUND <size>\n" then raw bytes,
 otherwise replies "NOTFOUND\n".
*/
public class PeerServer {
    private final int port;
    private final FileManager fileManager;
    private ServerSocket serverSocket;
    private volatile boolean running = false;

    public PeerServer(int port, FileManager fileManager) {
        this.port = port;
        this.fileManager = fileManager;
    }

    // Start server in a new thread
    public void start() {
        Thread t = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                running = true;
                System.out.println("PeerServer listening on port " + port);
                while (running) {
                    Socket client = serverSocket.accept();
                    // handle each connection in its own small thread
                    new Thread(() -> handleClient(client)).start();
                }
            } catch (IOException e) {
                if (running) e.printStackTrace();
            }
        }, "PeerServer-Thread");
        t.setDaemon(true);
        t.start();
    }

    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
    }

    private void handleClient(Socket s) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
             OutputStream out = s.getOutputStream();
             PrintWriter writer = new PrintWriter(out, true)) {

            String line = reader.readLine(); // expect "GET filename"
            if (line == null) return;
            if (!line.startsWith("GETcd ")) {
                writer.println("INVALID");
                return;
            }
            String filename = line.substring(4).trim();
            File f = fileManager.getSharedFile(filename);
            if (f == null) {
                writer.println("NOTFOUND");
                return;
            }
            long size = f.length();
            writer.println("FOUND " + size);

            // send file bytes
            try (FileInputStream fis = new FileInputStream(f)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = fis.read(buf)) != -1) out.write(buf, 0, r);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { s.close(); } catch (IOException ignored) {}
        }
    }
}
