import java.io.*;

public class FileManager {
    private final File sharedDir;

    public FileManager(File sharedDir) {
        this.sharedDir = sharedDir;
        if (!sharedDir.exists()) sharedDir.mkdirs();
    }

    // Check if file exists in shared dir
    public boolean hasFile(String name) {
        File f = new File(sharedDir, name);
        return f.exists() && f.isFile();
    }

    // Return File object for a shared file (null if not exists)
    public File getSharedFile(String name) {
        File f = new File(sharedDir, name);
        return (f.exists() && f.isFile()) ? f : null;
    }

    // Save bytes to file (used by downloader)
    public void saveFile(String name, InputStream in, long size) throws IOException {
        File outFile = new File(sharedDir.getParentFile(), name); // save to parent dir by default
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[8192];
            long remaining = size;
            while (remaining > 0) {
                int toRead = (int)Math.min(buffer.length, remaining);
                int r = in.read(buffer, 0, toRead);
                if (r < 0) throw new EOFException("Unexpected end of stream");
                fos.write(buffer, 0, r);
                remaining -= r;
            }
        }
    }

    // Copy arbitrary file into shared directory (used by UI when user adds a file to share)
    public File addToShared(File source) throws IOException {
        File dest = new File(sharedDir, source.getName());
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
        }
        return dest;
    }

    // List files in shared directory
    public File[] listSharedFiles() {
        File[] files = sharedDir.listFiles((f) -> f.isFile());
        return files == null ? new File[0] : files;
    }
}
