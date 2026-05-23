import java.net.*;
import java.io.*;

/*
 Simple client: connect to host:port, send "GET <filename>\n".
 Expect "FOUND <size>" or "NOTFOUND".
 If FOUND, read exactly <size> bytes from socket and save to a target file path.
*/
public class PeerClient {

    public enum Result { SUCCESS, NOTFOUND, ERROR }

    public static class DownloadResult {
        public Result result;
        public String message;
        public File savedFile;
    }

    // Download file from a peer and save to targetFile (absolute or relative)
    public DownloadResult download(String host, int port, String filename, File targetFile) {
        DownloadResult res = new DownloadResult();
        try (Socket s = new Socket(host, port);
             BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
             OutputStream out = s.getOutputStream();
             PrintWriter writer = new PrintWriter(out, true)) {

            writer.println("GET " + filename);
            String header = reader.readLine();
            if (header == null) {
                res.result = Result.ERROR;
                res.message = "No response from peer";
                return res;
            }
            if (header.startsWith("NOTFOUND")) {
                res.result = Result.NOTFOUND;
                res.message = "File not found on peer";
                return res;
            }
            if (!header.startsWith("FOUND ")) {
                res.result = Result.ERROR;
                res.message = "Unexpected response: " + header;
                return res;
            }
            long size = Long.parseLong(header.substring(6).trim());

            // Now read 'size' bytes from socket InputStream
            InputStream is = s.getInputStream();
            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[8192];
                long remaining = size;
                while (remaining > 0) {
                    int toRead = (int)Math.min(buffer.length, remaining);
                    int r = is.read(buffer, 0, toRead);
                    if (r < 0) throw new EOFException("Unexpected end of stream");
                    fos.write(buffer, 0, r);
                    remaining -= r;
                }
            }

            res.result = Result.SUCCESS;
            res.message = "Downloaded successfully";
            res.savedFile = targetFile;
            return res;

        } catch (Exception e) {
            res.result = Result.ERROR;
            res.message = e.getMessage();
            return res;
        }
    }
}
