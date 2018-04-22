package gitlet;

import java.io.Serializable;
import java.util.Map;
import java.io.File;

/** Driver class for Branch.
 *  @author Anh Le & Roberto Romo
 */
public class Branch implements Serializable {

    /** Branch constructor.
     * @param name Name of this branch.
     * @param commit Current commit of this branch.
     */
    public Branch(String name, Commit commit) {
        _name = name;
        _commit = commit;
    }

    /** Returns the current commit of this branch. */
    public Commit commit() {
        return _commit;
    }

    /** Returns the name of this branch. */
    public String name() {
        return _name;
    }

    /** Returns file FILE in branch. */
    public File getFile(String file) {
        return new File(branchFiles().get(file) + "/" + file);
    }

    /** Returns the Branch's files. */
    public Map<String, String> branchFiles() {
        return _commit.files();
    }

    /** Name of the Branch. */
    private String _name;

    /** Current Commit. */
    private Commit _commit;

}
