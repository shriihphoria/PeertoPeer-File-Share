import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

/*
 Simple UI:
 - Left: list of shared files
 - Button to "Add file to shared" (copies file into ./shared)
 - Right: fields to enter peer host, peer port, filename, and Download button
 - Shows a progress bar (indeterminate while downloading)
*/
public class PeerUI {
    private final JFrame frame;
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final FileManager fileManager;
    private final PeerServer server;
    private final PeerClient client = new PeerClient();

    public PeerUI(int serverPort) {
        File shared = new File("shared");
        fileManager = new FileManager(shared);
        server = new PeerServer(serverPort, fileManager);
        server.start();

        frame = new JFrame("Simple P2P - Java Swing");
        frame.setSize(700, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initUI();
        refreshSharedList();
    }

    private void initUI() {
        JPanel left = new JPanel(new BorderLayout());
        JList<String> sharedList = new JList<>(listModel);
        left.add(new JScrollPane(sharedList), BorderLayout.CENTER);
        JButton addBtn = new JButton("Add file to shared");
        addBtn.addActionListener(e -> addFileAction());
        left.add(addBtn, BorderLayout.SOUTH);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        JPanel form = new JPanel(new GridLayout(4,2,5,5));
        JTextField hostField = new JTextField("localhost");
        JTextField portField = new JTextField("5001");
        JTextField nameField = new JTextField();
        form.add(new JLabel("Peer host:")); form.add(hostField);
        form.add(new JLabel("Peer port:")); form.add(portField);
        form.add(new JLabel("Filename to request:")); form.add(nameField);

        JButton downloadBtn = new JButton("Download");
        JProgressBar progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);

        downloadBtn.addActionListener(e -> {
            String host = hostField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());
            String filename = nameField.getText().trim();
            if (host.isEmpty() || filename.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please enter host and filename.");
                return;
            }
            // choose where to save
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File(filename));
            if (fc.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
            File target = fc.getSelectedFile();

            progressBar.setVisible(true);
            progressBar.setIndeterminate(true);
            progressBar.setString("Downloading...");

            // run download in background thread
            new Thread(() -> {
                PeerClient.DownloadResult result = client.download(host, port, filename, target);
                SwingUtilities.invokeLater(() -> {
                    progressBar.setIndeterminate(false);
                    if (result.result == PeerClient.Result.SUCCESS) {
                        progressBar.setValue(100);
                        progressBar.setString("Done");
                        JOptionPane.showMessageDialog(frame, "Downloaded to: " + result.savedFile.getAbsolutePath());
                    } else if (result.result == PeerClient.Result.NOTFOUND) {
                        progressBar.setString("Not found");
                        JOptionPane.showMessageDialog(frame, "File not found on peer.");
                    } else {
                        progressBar.setString("Error");
                        JOptionPane.showMessageDialog(frame, "Error: " + result.message);
                    }
                    progressBar.setVisible(false);
                    refreshSharedList();
                });
            }).start();
        });

        right.add(form);
        right.add(Box.createRigidArea(new Dimension(0,10)));
        right.add(downloadBtn);
        right.add(Box.createRigidArea(new Dimension(0,10)));
        right.add(progressBar);

        frame.getContentPane().setLayout(new GridLayout(1,2));
        frame.getContentPane().add(left);
        frame.getContentPane().add(right);
    }

    private void addFileAction() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        File src = fc.getSelectedFile();
        try {
            File dest = fileManager.addToShared(src);
            refreshSharedList();
            JOptionPane.showMessageDialog(frame, "Added to shared: " + dest.getName());
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error adding file: " + ex.getMessage());
        }
    }

    private void refreshSharedList() {
        SwingUtilities.invokeLater(() -> {
            listModel.clear();
            for (File f : fileManager.listSharedFiles()) listModel.addElement(f.getName());
        });
    }

    public void show() {
        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }

    public static void main(String[] args) {
        int port = 5001;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (Exception ignored) {}
        }
        PeerUI ui = new PeerUI(port);
        ui.show();
    }
}
