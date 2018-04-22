package gitlet;

import ucb.junit.textui;
import org.junit.Test;

import javax.imageio.IIOException;

import static org.junit.Assert.*;
import java.io.File;
import java.io.IOException;

/** The suite of all JUnit tests for the gitlet package.
 *  @author Anh Le & Roberto Romo
 */
public class UnitTest {

    /** Run the JUnit tests in the loa package. Add xxxTest.class entries to
     * the arguments of runClasses to run other JUnit tests. */
    public static void main(String[] ignored) {
        textui.runClasses(UnitTest.class);
    }

    /** Tests Init. */
    @Test
    public void init() {
        File dir = new File(".gitlet");
        if (!dir.exists()) {
            dir.mkdirs();
        } else {
            remove(dir);
        }
    }

    /** Tests Init Part 2. */
    @Test
    public void init2() {
        Main.main("init");
        assertTrue(new File(".gitlet").exists());
    }

    /** Tests add. */
    @Test
    public void add() throws IOException {
        File f1 = new File("dang");
        try {
            f1.createNewFile();
        } catch (IIOException excp) {
            excp.printStackTrace();
        }
        Main.main("add", "dang");
    }

    /** Tests remove. */
    @Test
    public void remove() throws IOException {
        Main.main("rm", "dang");
    }

    /** Tests commit. */
    @Test
    public void commit() throws IOException {
        add();
        Main.main("commit", "hey");
        File f1 = new File("boom");
        try {
            f1.createNewFile();
        } catch (IIOException excp) {
            excp.printStackTrace();
        }

        Main.main("add", "boom");
    }

    /** Tests Branch and Checkout. */
    @Test
    public void branchAndCheckout() {
        Main.main("branch", "cow");
        Main.main("rm-branch", "cow");
        remove(new File("cow"));
    }

    /** Delete the files from directory. */
    public void remove(File d) {
        if (d.isDirectory()) {
            for (File f : d.listFiles()) {
                remove(f);
            }
        }
        d.delete();
    }
}

