package gitlet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.io.Serializable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Anh Le & Roberto Romo
 */
public class Main implements Serializable {

    /** Gitlet initializer. */
    public Main() {
        staged = new HashSet<>();
        removed = new HashSet<>();
        commits = new HashMap<>();
        branches = new HashMap<>();
        messages = new HashMap<>();
        stagedFiles = new HashMap<>();
    }

    /** Checkout command.
     * @param args User's input.
     */
    private void checkout(String... args) {
        if (args.length == 2) {
            gitlet.checkoutB(args[1]);
        } else if (args.length == 3) {
            gitlet.checkoutF(args[2]);
        } else {
            gitlet.checkoutID(args[1], args[3]);
        }
    }

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        gitlet = loader();
        if (gitlet == null) {
            gitlet = new Main();
        }
        gitlet.isGitlet(args[0]);
        gitlet.checkOperands(args, args[0]);
        switch (args[0]) {
        case "init":
            gitlet.init();
            break;
        case "add":
            gitlet.add(args[1]);
            break;
        case "commit":
            gitlet.commit(args[1]);
            break;
        case "rm":
            gitlet.rm(args[1]);
            break;
        case "log":
            gitlet.log();
            break;
        case "global-log":
            gitlet.globalLog();
            break;
        case "find":
            gitlet.find(args[1]);
            break;
        case "status":
            gitlet.status();
            break;
        case "checkout":
            gitlet.checkout(args);
            break;
        case "branch":
            gitlet.branch(args[1]);
            break;
        case "rm-branch":
            gitlet.removeBranch(args[1]);
            break;
        case "reset":
            gitlet.reset(args[1]);
            break;
        case "merge":
            gitlet.merge(args[1]);
            break;
        default:
            System.out.println("No command with that name exists.");
            return;
        }
    }

    /** Creates a new gitlet version-control system in the current directory. */
    private void init() {
        if (new File(".gitlet").exists()) {
            System.out.println("A gitlet version-control system "
                    + "already exists in the current directory.");
        } else {
            File directory = new File(".gitlet");
            String message = "initial commit";
            directory.mkdir();
            String time = time();
            String sha = hashCommit(message, "0", time);
            Commit firstcommit = new Commit(message, "0", sha, time);
            String master = "master";
            Branch mstr = new Branch(master, firstcommit);
            _headName = master;
            _head = mstr;
            branches.put(master, mstr);
            commits.put(sha, firstcommit);
            messages.put(sha, message);
        }
        saver(gitlet);
    }

    /** Adds a copy of the file as it currently exists to the staging area.
     * @param name Name of file to be added to the staging area.
     */
    private void add(String name) {
        if (name == null) {
            return;
        }
        File file = new File(name);
        if (!file.exists()) {
            System.out.println("File does not exist.");
            return;
        }
        for (String s : removed) {
            if (s.equals(name)) {
                removed.remove(s);
                saver(gitlet);
                return;
            }
        }
        for (String f : _head.commit().files().keySet()) {
            if (Arrays.equals(Utils.readContents(_head.getFile(f)),
                    Utils.readContents(file))
                    && f.equals(name)) {
                return;
            }
        }
        staged.add(name);
        File stag = new File(name);
        String sha = Utils.sha1(Utils.readContents(stag));
        stagedFiles.put(sha, stag);
        saver(gitlet);
    }

    /** Saves a snapshot of certain files in the current commit and staging
     * area.
     * @param message The commit message.
     */
    private void commit(String message) {
        if (removed.isEmpty() && staged.isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        Map<String, String> blobs = new HashMap<>();

        if (_head.branchFiles() != null) {
            blobs.putAll(_head.branchFiles());
        }

        for (String file: staged) {
            blobs.put(file, null);
        }

        for (String file : removed) {
            blobs.remove(file);
        }

        String time = time();
        String parentSha = _head.commit().sha();
        String sha = hashCommit(message, parentSha, time);
        Commit commit = new Commit(message, parentSha, sha, blobs, time);
        messages.put(sha, message);
        commits.put(sha, commit);
        Branch branch = new Branch(_headName, commit);
        branches.put(_headName, branch);
        _head = branches.get(_headName);
        staged.clear();
        stagedFiles.clear();
        removed.clear();
        saver(gitlet);
    }

    /** Removes the file from the working directory if it was tracked
     * in the current commit. If the file had been staged, then unstage it.
     * @param filename Name of the file to be removed.
     */
    private void rm(String filename) {
        boolean bool  = true;
        if (staged.contains(filename)) {
            staged.remove(filename);
            File file = new File(filename);
            String sha = Utils.sha1(Utils.readContents(file));
            stagedFiles.remove(sha);
            bool = false;
        }
        if (_head.branchFiles().containsKey(filename)) {
            Utils.restrictedDelete(filename);
            removed.add(filename);
            bool = false;
        }
        if (bool) {
            System.out.println("No reason to remove the file.");
        }
        saver(gitlet);
    }

    /** Display information about each commit backwards along the commit tree
     * until the initial commit. */
    private void log() {
        Commit commit = _head.commit();
        String commitsha = commit.sha();
        while (commit != null) {
            String time = commit.time();
            String message = commit.message();
            System.out.println("===");
            System.out.println("Commit " + commitsha);
            System.out.println(time);
            System.out.println(message);
            System.out.println();
            commitsha = commit.psha();
            commit = commits.get(commitsha);
        }
    }

    /** Displays information about all commits ever made. */
    private void globalLog() {
        for (Commit comm: commits.values()) {
            Commit commit = comm;
            String commitsha = commit.sha();
            String time = commit.time();
            String message = commit.message();
            System.out.println("===");
            System.out.println("Commit " + commitsha);
            System.out.println(time);
            System.out.println(message);
            System.out.println();
        }
    }

    /** Prints out the ids of all commits that have the given commit message.
     * @param message The commit message.
     */
    private  void find(String message) {
        if (message == null || !messages.containsValue(message)) {
            System.out.println("Found no commit with that message.");
            return;
        }
        for (String sha : messages.keySet()) {
            if (messages.get(sha).equals(message)) {
                System.out.println(sha);
            }
        }
    }

    /** Returns true iff the file with the given fileName in working directory
     * has similar contents with a file with the same name in head commit.
     * @param fileName Name of the given file
     */
    private boolean notModified(String fileName) {
        File file = new File(fileName);
        byte[] arr = Utils.readContents(file);
        for (String f : _head.commit().files().keySet()) {
            if (f.equals(fileName)) {
                byte[] arr2 = Utils.readContents(_head.getFile(f));
                if (Arrays.equals(arr, arr2)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Prints out the branches for status command. */
    private void statusBranches() {
        TreeMap<String, Branch> brch = new TreeMap<>(branches);
        for (String branch: brch.keySet()) {
            if (branch.equals(_headName)) {
                System.out.print("*");
            }
            System.out.println(branch);
        }
        System.out.println();
    }

    /** Prints out the untracked files for status command. */
    private void statusUntracked() {
        File thisDir = new File(".");
        List<String> dirContent = Utils.plainFilenamesIn(thisDir);
        Collections.sort(dirContent);
        for (String fileName : dirContent) {
            if (!_head.branchFiles().containsKey(fileName)
                    && !staged.contains(fileName)
                    && !removed.contains(fileName)
                    && !fileName.equals("gitlet.ser")) {
                System.out.println(fileName);
            }
        }
    }

    /** Displays what branches currently exist, and marks the current branch
     * with a *. */
    private void status() {
        TreeSet<String> st = new TreeSet<>(staged);
        TreeSet<String> rm = new TreeSet<>(removed);
        System.out.println("=== Branches ===");
        statusBranches();
        System.out.println("=== Staged Files ===");
        for (String stages: st) {
            System.out.println(stages);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (String rmv: rm) {
            System.out.println(rmv);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        File thisDir = new File(".");
        Set<String> deleted = new TreeSet<>();
        Set<String> changed = new TreeSet<>();
        List<String> dirContent = Utils.plainFilenamesIn(thisDir);
        Collections.sort(dirContent);
        for (String sFile : st) {
            if (!dirContent.contains(sFile)) {
                deleted.add(sFile);
            }
        }
        for (String tra : _head.branchFiles().keySet()) {
            if (!dirContent.contains(tra) && !removed.contains(tra)) {
                deleted.add(tra);
            }
        }
        for (String del : deleted) {
            System.out.println(del + " (deleted)");
        }
        for (String fileName : dirContent) {
            if (_head.branchFiles().containsKey(fileName)
                    && !notModified(fileName)
                    && !staged.contains(fileName)) {
                changed.add(fileName);
            }
        }
        for (String sha : stagedFiles.keySet()) {
            for (String f : dirContent) {
                if (f.equals(stagedFiles.get(sha).getName())) {
                    File di = new File(f);
                    String sha2 = Utils.sha1(Utils.readContents(di));
                    if (!sha2.equals(sha)) {
                        changed.add(f);
                    }
                }
            }
        }
        for (String cha : changed) {
            System.out.println(cha + " (modified)");
        }
        System.out.println();
        System.out.println("=== Untracked Files ===");
        statusUntracked();
    }

    /** Check out for file name.
     * @param file Name of the file to be checked out in current branch.
     */
    private void checkoutF(String file) {
        if (!_head.branchFiles().containsKey(file)) {
            System.out.println("File does not exist in that commit.");
            return;
        } else {
            _head.commit().restoreFile(file);
        }
        saver(gitlet);
    }

    /** Checkout file name with commit id.
     * @param id A unique commit id.
     * @param file Name of the file to be checked out with the given commit id.
     */
    private void checkoutID(String id, String file) {
        boolean exit = true;
        if (id.length() >= 6) {
            for (String i : commits.keySet()) {
                if (i.substring(0, 6).equals(id.substring(0, 6))) {
                    exit = false;
                    Commit arg = commits.get(i);
                    if (!arg.files().containsKey(file)) {
                        System.out.println("File does not exist in that "
                                + "commit.");
                        return;
                    }
                    arg.restoreFile(file);
                }
            }
        }
        if (exit) {
            System.out.println("No commit with that id exists.");
            return;
        }
        saver(gitlet);
    }

    /** Check out for branch name.
     * @param branch Name of the branch in which all of its associated files
     * will be checked out.
     */
    private void checkoutB(String branch) {
        if (!branches.containsKey(branch)) {
            System.out.println("No such branch exists.");
            return;
        } else if (branch.equals(_headName)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        Branch b = branches.get(branch);
        File thisDir = new File(".");
        for (String file : Utils.plainFilenamesIn(thisDir)) {
            if (b.branchFiles().containsKey(file)
                    && !_head.branchFiles().containsKey(file)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it or add it first.");
                return;
            }
        }
        for (String filez : Utils.plainFilenamesIn(thisDir)) {
            if (_head.branchFiles().containsKey(filez)) {
                Utils.restrictedDelete(filez);
            }
        }
        b.commit().restore();
        _headName = branch;
        _head = branches.get(branch);
        staged.clear();
        stagedFiles.clear();
        saver(gitlet);
    }

    /** Creates a new branch with the given name, and points it at the current
     * head node.
     * @param branch Name of the new branch.
     */
    private void branch(String branch) {
        if (branches.containsKey(branch)) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        Branch b = new Branch(branch, _head.commit());
        branches.put(branch, b);
        saver(gitlet);
    }

    /** Deletes the branch with the given name.
     * @param branch Name of the branch to be deleted.
     */
    private void removeBranch(String branch) {
        if (!branches.containsKey(branch)) {
            System.out.println("A branch with that name does not exist.");
            return;
        } else if (branch.equals(_headName)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        branches.remove(branch);
        saver(gitlet);
    }

    /** Checks out all the files tracked by the given commit.
     * And moves the current branch's head to that commit node.
     * @param id A unique commit id.
     */
    private void reset(String id) {
        boolean exit = true;
        if (id.length() >= 6) {
            for (String i : commits.keySet()) {
                if (i.substring(0, 6).equals(id.substring(0, 6))) {
                    exit = false;
                    Commit commit = commits.get(i);
                    File thisDir = new File(".");
                    for (String file : Utils.plainFilenamesIn(thisDir)) {
                        if (commit.files().keySet().contains(file)
                              && !_head.branchFiles().keySet().contains(file)) {
                            System.out.println("There is an untracked file in "
                                    + "the way; delete it or add it first.");
                            return;
                        }
                    }
                    for (String filez : Utils.plainFilenamesIn(thisDir)) {
                        if (_head.branchFiles().containsKey(filez)) {
                            Utils.restrictedDelete(filez);
                        }
                    }
                    Branch res = new Branch(_headName, commit);
                    res.commit().restore();
                    branches.put(_headName, res);
                    _head = branches.get(_headName);
                    break;
                }
            }
        }
        if (exit) {
            System.out.println("No commit with that id exists.");
            return;
        }
        staged.clear();
        stagedFiles.clear();
        saver(gitlet);
    }

    /** Merge's failure cases.
     * @param branch Name of the given branch in merge
     */
    private void failMerge(String branch) {
        if (!branches.containsKey(branch)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }

        if (!staged.isEmpty() || !removed.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }

        File thisDir = new File(".");
        for (String file : Utils.plainFilenamesIn(thisDir)) {
            if (branches.get(branch).branchFiles().containsKey(file)
                    && !_head.branchFiles().containsKey(file)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it or add it first.");
                System.exit(0);
            }
        }


        if (_headName.equals(branch)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }

        Commit sp = split(_headName, branch);
        if (sp.sha().equals(branches.get(branch).commit().sha())) {
            System.out.println("Given branch is an ancestor of the "
                    + "current branch.");
            System.exit(0);
        }
        if (sp.sha().equals(_head.commit().sha())) {
            _head = branches.get(branch);
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }
    }


    /** Header for file writing. */
    private String header = "<<<<<<< HEAD";
    /** Body for file writing. */
    private String b2 = "=======";
    /** Tail for file writing. */
    private String tail = ">>>>>>>";


    /** Merges files from the given branch into the current branch.
     * @param branch Name of the branch in which its files will be merged
     * to the current branch.
     */
    private void merge(String branch) {
        failMerge(branch);
        mergeHelper(branch);
        mergeHelperX(branch);
        Commit sp = split(_headName, branch);
        Branch given = branches.get(branch);
        Branch head = branches.get(_headName);
        Map<String, String> split = sp.files();
        Map<String, String> infiles = given.branchFiles();
        Map<String, String> currfiles = head.branchFiles();
        for (String merg : infiles.keySet()) {
            if (!split.keySet().contains(merg)
                    && !currfiles.keySet().contains(merg)) {
                File f1 = new File(infiles.get(merg) + "/" + merg);
                byte[] giver = Utils.readContents(f1);
                File fl2 = new File(merg);
                Utils.writeContents(fl2, giver);
                staged.add(merg);
            } else if (split.keySet().contains(merg)
                    && !currfiles.keySet().contains(merg)) {
                File f1 = new File(infiles.get(merg) + "/" + merg);
                File temp = new File(split.get(merg) + "/" + merg);
                File f2 = new File(currfiles.get(merg) + "/" + merg);
                if (f1.isFile() && temp.isFile() && f2.isFile()) {
                    byte[] giver = Utils.readContents(f1);
                    byte[] splitter = Utils.readContents(temp);
                    if (!Arrays.equals(giver, splitter)) {
                        _bool = true;
                        File fl = new File(merg);
                        String con2 = new String(giver);
                        header = header.concat("\n"  + b2 + "\n" + con2
                                + "\n" + tail + "\n");
                        byte[] stuff = header.getBytes();
                        Utils.writeContents(fl, stuff);
                    }
                }
            }
        }
        if (bool()) {
            System.out.println("Encountered a merge conflict.");
            System.exit(0);

        } else {
            commit("Merged " + _headName + " with " + branch + ".");
        }
    }

    /** Return bool for merge. */
    public Boolean bool() {
        return _bool;
    }

    /** Boolean for merge. */
    private Boolean _bool = false;

    /** Helper for merge.
     * @param branch Name of the branch in which its files will be merged
     * to the current branch.
     */
    public void mergeHelperX(String branch) {
        Commit sp = split(_headName, branch);
        Branch given = branches.get(branch);
        Branch head = branches.get(_headName);
        Map<String, String> split = sp.files();
        Map<String, String> infiles = given.branchFiles();
        Map<String, String> currfiles = head.branchFiles();
        for (String curr : currfiles.keySet()) {
            if (split.keySet().contains(curr)
                    && !infiles.keySet().contains(curr)) {
                File f2 = new File(currfiles.get(curr) + "/" + curr);
                File temp = new File(split.get(curr) + "/" + curr);
                if (f2.isFile() && temp.isFile()) {
                    byte[] splitter = Utils.readContents(temp);
                    byte[] current = Utils.readContents(f2);
                    if (!Arrays.equals(current, splitter)) {
                        _bool = true;
                        File fl = new File(curr);
                        String con1 = new String(current);
                        header = header.concat("\n" + con1 + "\n" + b2 + "\n"
                                + tail + "\n");
                        byte[] stuff = header.getBytes();
                        Utils.writeContents(fl, stuff);
                    } else if (Arrays.equals(current, splitter)) {
                        removed.add(curr);
                        Utils.restrictedDelete(curr);        
                    }
                }
            }
        }
    }

    /** Helper for merge.
     * @param branch Name of the branch in which its files will be merged
     * to the current branch.
     */
    public void mergeHelper(String branch) {
        Commit sp = split(_headName, branch);
        Branch given = branches.get(branch);
        Branch head = branches.get(_headName);
        Map<String, String> split = sp.files();
        Map<String, String> infiles = given.branchFiles();
        Map<String, String> currfiles = head.branchFiles();
        for (String s : split.keySet()) {
            if (currfiles.keySet().contains(s)
                    && infiles.keySet().contains(s)) {
                File f1 = new File(infiles.get(s) + "/" + s);
                File f2 = new File(currfiles.get(s) + "/" + s);
                File temp = new File(split.get(s) + "/" + s);
                byte[] giver = Utils.readContents(f1);
                byte[] current = Utils.readContents(f2);
                byte[] splitter = Utils.readContents(temp);
                if (Arrays.equals(current, splitter)
                        && !Arrays.equals(giver, splitter)) {
                    File fl = new File(s);
                    Utils.writeContents(fl, giver);
                    staged.add(s);
                } else if (!Arrays.equals(current, splitter)
                        && !Arrays.equals(giver, splitter)) {
                    String con1 = new String(current);
                    String con2 = new String(giver);
                    _bool = true;
                    File fl = new File(s);
                    header = header.concat("\n" + con1 + "\n" + b2 + "\n"
                            + con2 + "\n" + tail + "\n");
                    byte[] stuff = header.getBytes();
                    Utils.writeContents(fl, stuff);
                }
            }
        }
    }

    /** Returns/Localizes the split commit between the two given branches.
     * @param b1 Current branch
     * @param b Given branch
     */
    private Commit split(String b1, String b) {
        List<String> c1 = new ArrayList<>();
        List<String> c2 = new ArrayList<>();

        Branch h1 = branches.get(b1);
        Commit commit = h1.commit();
        String sha = commit.sha();

        while (commit != null) {
            c1.add(commit.time());
            sha = commits.get(sha).psha();
            commit = commits.get(sha);
        }
        Branch h2 = branches.get(b);
        commit = h2.commit();
        sha = commit.sha();

        while (commit != null) {
            c2.add(commit.time());
            sha = commits.get(sha).psha();
            commit = commits.get(sha);
        }
        List<String> equal = new ArrayList<>();
        for (String name: c2) {
            if (c1.contains(name)) {
                equal.add(name);
            }
        }
        String s = null;
        String csha = Collections.max(equal);
        for (Commit comm: commits.values()) {
            if (comm.time().equals(csha)) {
                s = comm.sha();
            }
        }
        return commits.get(s);
    }

    /** Check if current directory is a gitlet directory.
     * @param cmmd The command argument.
     */
    private void isGitlet(String cmmd) {
        boolean check = true;
        switch (cmmd) {
        case "init":
            check = false;
            break;
        default:
            File thisDir = new File(".");
            File[] files = thisDir.listFiles();
            for (File file : files) {
                if (file.getName().equals(".gitlet")) {
                    check = false;
                    break;
                }
            }
            break;
        }
        if (check) {
            System.out.println("Not in an initialized gitlet directory");
            System.exit(0);
        }
    }

    /** Returns time for commit and SHA-1. */
    private String time() {
        String time;
        SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        time = date.format(new Date());
        return time;
    }

    /** Returns the SHA-1 of a commit.
     * @param message The commit message.
     * @param pHash SHA-1 for the parent.
     * @param time Time the commit was made.
     */
    public String hashCommit(String message, String pHash, String time) {
        ArrayList<Object> list = new ArrayList<>();
        list.add(message);
        list.add(pHash);
        if (_head != null && !_head.branchFiles().isEmpty()) {
            for (String n : _head.branchFiles().keySet()) {
                File file = new File(n);
                if (file.isFile()) {
                    list.add(Utils.readContents(file));
                }
            }
        }
        if (!staged.isEmpty()) {
            for (String name : staged) {
                File file = new File(name);
                list.add(Utils.readContents(file));
            }
        }
        list.add(time);
        saver(gitlet);
        return Utils.sha1(list);
    }

    /** Returns/load gitlet directory. */
    public static Main loader() {
        Main gl = null;
        File glFile = new File("gitlet.ser");
        if (glFile.exists()) {
            try {
                ObjectInputStream obj =
                        new ObjectInputStream(new FileInputStream(glFile));
                gl = (Main) obj.readObject();
            } catch (IOException | ClassNotFoundException excp) {
                System.out.println("IOException");
            }
        }
        return gl;
    }

    /** Save gitlet directory.
     * @param g A gitlet instance.
     */
    public static void saver(Main g) {
        File outFile = new File("gitlet.ser");
        try {
            ObjectOutputStream out =
                    new ObjectOutputStream(new FileOutputStream(outFile));
            out.writeObject(g);
            out.close();
        } catch (IOException excp) {
            excp.printStackTrace();
        }
    }

    /** Check for the correct number of operands from input.
     * @param args User's input.
     * @param cmmd The command argument.
     */
    private void checkOperands(String[] args, String cmmd) {
        boolean check = true;
        int length = args.length;
        switch (cmmd) {
        case "init": case "log": case "global-log": case "status":
            if (length != 1) {
                check = false;
            }
            break;
        case "commit":
            if (length > 2) {
                check = false;
            } else if (length == 1 || args[1].trim().isEmpty()) {
                System.out.println("Please enter a commit message.");
                System.exit(0);
            }
            break;
        case "add": case "rm": case "find": case "branch":
        case "rm-branch": case "reset": case "merge":
            if (length != 2) {
                check = false;
            }
            break;
        case "checkout":
            if (length == 1 || length > 4
                || (length == 3 && !args[1].equals("--"))
                || (length == 4 && !args[2].equals("--"))) {
                check = false;
            }
            break;
        default:
            break;
        }
        if (!check) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    /** The gitlet program. */
    private  static Main gitlet;

    /** Staged, or currently added files. */
    private Set<String> staged;

    /** Removed Files. */
    private Set<String> removed;

    /** Map of SHA-1 values and their respective commit messages. */
    private Map<String, String> messages;

    /** Map of SHA-1 values and their respective commits. */
    private Map<String, Commit> commits;

    /** Map of branches and their name.*/
    private Map<String, Branch> branches;

    /** Represents the current head of the branch. */
    private Branch _head;

    /** The name of the current branch.*/
    private String _headName;

    /** Map of SHA-1 values and their respective staged files. */
    private Map<String, File> stagedFiles;
}
