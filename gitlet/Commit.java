package gitlet;

import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Driver class for Commit.
 *  @author Anh Le & Roberto Romo
 */
public class Commit implements Serializable {

    /** Commit Constructor.
     * @param message The commit message.
     * @param pHash SHA-1 for the parent.
     * @param sHash SHA-1 for the commit.
     * @param blobs Stored files of this commit.
     * @param time Time the commit was made.
     */
    public Commit(String message, String pHash, String sHash,
                  Map<String, String> blobs, String time) {
        _blobs.putAll(blobs);
        _msg = message;
        _pHash = pHash;
        _sHash = sHash;
        _commTime = time;
        _dir = ".gitlet/" + _sHash;

        try {
            makeDir(_dir);
        } catch (IOException excp) {
            excp.printStackTrace();
        }
    }


    /** Initial Commit Constructor with no previous blobs.
     * @param message The initial commit message.
     * @param pHash SHA-1 for the parent.
     * @param sHash SHA-1 for the commit.
     * @param time Time the commit was made.
     */
    public Commit(String message, String pHash, String sHash, String time) {
        _msg = message;
        _pHash = pHash;
        _sHash = sHash;
        _commTime = time;
        _dir = null;
    }

    /** Copy a file source to file dest, adapted from
     * http://www.journaldev.com/861/4-ways-to-copy-file-in-java.
     * @param source The source of the file to be copied.
     * @param dest The destination of the copied file.
     */
    public void copyFile(File source, File dest) throws IOException {
        if (!source.exists()) {
            return;
        }
        File destination = dest.getParentFile();
        if (destination != null && !destination.exists()) {
            destination.mkdirs();
        }
        Files.copy(source.toPath(), dest.toPath(),
                StandardCopyOption.REPLACE_EXISTING);
    }

    /** Assigns directories to the blobs.
     * @param direc Directory of the commit.
     */
    private void makeDir(String direc) throws IOException {
        for (String f : _blobs.keySet()) {
            File source = new File(f);
            File dest = new File(direc + "/" + f);

            if (dest.getParentFile() != null) {
                dest.getParentFile().mkdirs();
            }
            Files.copy(source.toPath(), dest.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            _blobs.put(f, direc);
        }
    }

    /** Replaces files in working directory with blobs. */
    public void restore() {
        for (String file: files().keySet()) {
            File dest = new File(file);
            File source = new File(files().get(file) + "/" + file);
            try {
                copyFile(source, dest);
            } catch (IOException excp) {
                excp.printStackTrace();
            }
        }

    }

    /** Replaces a file in working directory with blob.
     * @param fileName Name of the tracked file to be restored back
     * to working directory.
     */
    public void restoreFile(String fileName) {
        for (String file: files().keySet()) {
            if (fileName.equals(file)) {
                File dest = new File(file);
                File source = new File(files().get(file) + "/" + file);
                try {
                    copyFile(source, dest);
                } catch (IOException excp) {
                    excp.printStackTrace();
                }
            }
        }
    }

    /** Returns stored files. */
    public Map<String, String> files() {
        return _blobs;
    }

    /** Return the directory of this commit. */
    public String directory() {
        return _dir;
    }

    /** Returns the SHA of this commit. */
    public String sha() {
        return _sHash;
    }

    /** Returns the SHA of this commit's parent. */
    public String psha() {
        return _pHash;
    }

    /** Returns the time/date of this commit. */
    public String time() {
        return _commTime;
    }

    /** Returns commit message. */
    public String message() {
        return _msg;
    }

    /** Directory of commit. */
    private String _dir;

    /** Commit date. */
    private String _commTime;

    /** Commit Message. */
    private String _msg;

    /** SHA-1 for the commit. */
    private String _sHash;

    /** SHA-1 for the parent. */
    private String _pHash;

    /** Blob references. */
    private Map<String, String> _blobs = new HashMap<>();
}
