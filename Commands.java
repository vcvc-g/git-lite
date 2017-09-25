/**
 * Created by Andy Hyunouk Ko on 7/13/2017.
 */
package gitlet;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.io.FileWriter;
import static gitlet.Utils.plainFilenamesIn;
import static gitlet.Utils.restrictedDelete;

public class Commands {
    String currentDir = System.getProperty("user.dir");
    String stageIn = currentDir + "/.gitlet/stagingArea";

    public void init() {
        File[] directories = new File(currentDir).listFiles(File::isDirectory);
        for (File f: directories) {
            if (f.getName().equals(".gitlet")) {
                System.out.println("A gitlet version-control system "
                        + "already exists in the current directory.");
            }
            return;
        }

        File dir = new File(currentDir, ".gitlet");
        dir.mkdir();
        File dir2 = new File(currentDir, ".gitlet/commits");
        dir2.mkdir();
        File dir3 = new File(currentDir, ".gitlet/blobs");
        dir3.mkdir();
        File dir4 = new File(currentDir, ".gitlet/branches");
        dir4.mkdir();
        File dir5 = new File(currentDir, ".gitlet/stagingArea");
        dir5.mkdir();
        File dir6 = new File(currentDir, ".gitlet/untrackedFiles");
        dir6.mkdir();
        Commit initCommit = new Commit();
        Branch master = new Branch(initCommit.ID, "master");
        Branch head = new Branch(initCommit.ID, "head", "master");
    }

    public void add(String fileName) {
        File dir = new File(currentDir);
        File[] workFiles = dir.listFiles();
        List<String> list2 = plainFilenamesIn(currentDir + "/.gitlet/untrackedFiles");
        if (list2.contains(fileName)) {
            File file = new File(currentDir + "/.gitlet/untrackedFiles/" + fileName);
            file.delete();
            return;
        }

        for (int i = 0; i < workFiles.length; i++) {

            if (workFiles[i].getName().equals(fileName)) {
                String wanted = workFiles[i].getName();
                Blob newBlob = new Blob(workFiles[i]);

                List<String> list = plainFilenamesIn(stageIn);
                if (list.size() != 0) {
                    for (String blobid : list) {
                        Blob oneBlob = readObj("stagingArea", blobid);
                        if (oneBlob.getFileName().equals(fileName)) {
                            File blobFile = new File(stageIn + "/" + oneBlob.getFileID());
                            //Utils.join(currentDir,".gitlet","stagingArea",oneBlob.getFileID())
                            blobFile.delete();
                        }
                    }
                }

                Branch head = getHead();
                Commit currCommit = readObj("commits", head.commitPointer);
                if (currCommit.nameToBlob.containsKey(fileName)) {
                    if (currCommit.nameToBlob.get(fileName).equals(newBlob.getFileID())) {
                        return;
                    }
                }

                try {
                    FileOutputStream fileOut =
                            new FileOutputStream(stageIn + "/" + newBlob.getFileID());
                    ObjectOutputStream out = new ObjectOutputStream(fileOut);
                    out.writeObject(newBlob);
                    out.close();
                    fileOut.close();
                } catch (IOException e) {
                    System.out.println(e);
                }
                return;
            }
        }
        System.out.println("File does not exist.");
    }


    public void commit(String msg) {
        Branch head = getHead();
        try {
            Commit nCommit = new Commit(head.commitPointer, msg);
            Branch currBranch = readObj("branches", head.branchPointer);
            currBranch.commitPointer = nCommit.ID;
            File file = new File(currentDir + "/.gitlet/branches/" + currBranch.branchName);
            file.delete();
            Branch newBranch = new Branch(nCommit.ID, currBranch.branchName);
            Branch newhead = new Branch(nCommit.ID, "head", head.branchPointer);
        } catch (IllegalStateException e) {
            System.out.println("No changes added to the commit.");
        }
    }

    public void rm(String name) {
        boolean stagedOrTracked = false;
        Branch head = getHead();
        Commit currCommit = readObj("commits", head.commitPointer);
        if (currCommit.nameToBlob.containsKey(name)) {
            restrictedDelete(currentDir + "/" + name);
            stagedOrTracked = true;
            File toRemove = new File(currentDir + "/.gitlet/untrackedFiles/" + name);
            try {
                ObjectOutputStream out =
                        new ObjectOutputStream(new FileOutputStream(toRemove));
                out.writeObject(name);
                out.close();
            } catch (IOException excp) {
                System.out.println("Serializing error");
            }
        }
        //read blob objects from staging area.
        List<String> list = plainFilenamesIn(currentDir + "/.gitlet/stagingArea");
        for (String blobID : list) {
            Blob oneBlob = readObj("stagingArea", blobID);
            if (oneBlob.getFileName().equals(name)) {
                File toRemove = new File(currentDir + "/.gitlet/stagingArea/" + blobID);
                toRemove.delete();
                stagedOrTracked = true;
            }
        }
        if (!stagedOrTracked) {
            System.out.print("No reason to remove the file.");
        }
    }

    public void log() {
        Branch head = getHead();
        Commit currCom = readObj("commits", head.commitPointer);
        System.out.println("===");
        System.out.println("Commit " + currCom.ID);
        System.out.println(currCom.timeStamp);
        System.out.println(currCom.msg);
        System.out.println();

        do {
            currCom = readObj("commits", currCom.parentCode);
            System.out.println("===");
            System.out.println("Commit " + currCom.ID);
            System.out.println(currCom.timeStamp);
            System.out.println(currCom.msg);
            System.out.println();
        } while (currCom.parentCode != null);
    }

    public void globalLog() {
        List<String> commits = Utils.plainFilenamesIn(currentDir + "/.gitlet/commits");
        for (String c: commits) {
            Commit currCom = readObj("commits", c);
            System.out.println("===");
            System.out.println("Commit " + currCom.ID);
            System.out.println(currCom.timeStamp);
            System.out.println(currCom.msg);
            System.out.println();
        }
    }

    public void find(String msg) {
        List<String> commits = Utils.plainFilenamesIn(currentDir + "/.gitlet/commits");
        ArrayList<String> idList = new ArrayList<>();

        for (String c: commits) {
            Commit currCom = readObj("commits", c);
            if (currCom.msg.equals(msg)) {
                idList.add(currCom.ID);
            }
        }

        if (idList.isEmpty()) {
            System.out.println("Found no commit with that message.");
        } else {
            for (String id: idList) {
                System.out.println(id);
            }
        }
    }

    public void status() {
        //PRINT BRANCHES *************************
        //first have to retrieve head object, then run a for
        // loop to retrieve all branch objects
        //and compare each branch object's hash id to head's hash id.
        Branch head = getHead();
        System.out.println("=== Branches ===");
        //loop through all files in branches directory, retrieve them into objects
        //and compare hash ids
        String branchString = currentDir + "/.gitlet/branches";
        File branchesFolder = new File(branchString);
        File[] listOfFiles = branchesFolder.listFiles();
        //print in lexicographic order, save them as list first
        ArrayList<String> listOfBranchNames = new ArrayList<>(listOfFiles.length);
        for (int i = 0; i < listOfFiles.length; i++) {
            Branch branch;
            File inputFile = new File(listOfFiles[i].getPath());
            try {
                ObjectInputStream inp =
                        new ObjectInputStream(new FileInputStream(inputFile));
                branch = (Branch) inp.readObject();
                inp.close();
            } catch (IOException | ClassNotFoundException exception) {
                System.out.println(exception.getMessage());
                branch = null;
            }
            if (!branch.branchName.equals("head")) {
                if (head.branchPointer.equals(branch.branchName)) {
                    System.out.println("*" + branch.branchName);
                } else {
                    listOfBranchNames.add(branch.branchName);
                }
            }
            listOfBranchNames.sort((a, b) -> a.compareTo(b));
            for (String s : listOfBranchNames) {
                System.out.println(s);
            }
        }
        System.out.println();
        //PRINT STAGED FILES *************************
//        StagingArea stage;
//        File inputStageFile = new File(currentDir, ".gitlet/stagingArea");
//        try {
//            ObjectInputStream inp =
//                    new ObjectInputStream(new FileInputStream(inputStageFile));
//            stage = (StagingArea) inp.readObject();
//            inp.close();
//        } catch (IOException | ClassNotFoundException exception) {
//            System.out.println(exception.getMessage());
//            stage = null;
//        }
        System.out.println("=== Staged Files ===");
        //HashSet<String> keySet = (HashSet<String>) stage.nameToBlob.keySet();
        ArrayList<String> stageItem = new ArrayList();
        List<String> list = plainFilenamesIn(stageIn);
        //System.out.println(list.size());
        if (list.size() != 0) {
            //System.out.println("enter if loop");
            for (String blobid : list) {
                //System.out.println("enter for loop");
                Blob oneBlob = readObj("stagingArea", blobid);
                stageItem.add(oneBlob.getFileName());
            }
            //System.out.println(stageItem.get(1));
            stageItem.sort((a, b) -> a.compareTo(b));
            for (String s : stageItem) {
                System.out.println(s);
            }
        }
        System.out.println();
        //PRINT REMOVED FILES *************************
        System.out.println("=== Removed Files ===");
        String untrackedString = currentDir + "/.gitlet/untrackedFiles";
        File untrackedFolder = new File(untrackedString);
        File[] listOfUntrackedFiles = untrackedFolder.listFiles();
        ArrayList<String> listOfUntrackedNames = new ArrayList<>(listOfUntrackedFiles.length);
        //print in lexicographic order, save them as list first
        for (File file : listOfUntrackedFiles) {
            if (file.isFile()) {
                listOfUntrackedNames.add(file.getName());
            }
        }
        if (listOfUntrackedNames != null) {
            listOfUntrackedNames.sort((a, b) -> a.compareTo(b));
            for (String s : listOfUntrackedNames) {
                System.out.println(s);
            }
        }
        System.out.println();
        //PRINT MODIFIED FILES & UNTRACKED FILES *************************
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        System.out.println();
    }

    public void checkout(String name) {
        Branch head = getHead();
        Commit currCommit = readObj("commits", head.commitPointer);
        if (!currCommit.nameToBlob.containsKey(name)) {
            System.out.println("File does not exist in that commit.");
        } else {
            File outFile = new File(currentDir + "/" + name);
            Blob blob = readObj("blobs", currCommit.nameToBlob.get(name));
            Utils.writeContents(outFile, blob.getByteCode());
        }
    }

    public void checkout2(String id, String name) {
        Commit currCommit = readObj("commits", id);
        if (currCommit == null) {
            System.out.println("No commit with that id exists.");
        } else if (!currCommit.nameToBlob.containsKey(name)) {
            System.out.println("File does not exist in that commit.");
        } else {
            File outFile = new File(currentDir + "/" + name);
            Blob blob = readObj("blobs", currCommit.nameToBlob.get(name));
            Utils.writeContents(outFile, blob.getByteCode());
        }
    }

    public void checkout3(String branchName) {
        Branch head = getHead();
        Branch branch = readObj("branches", branchName);
        if (branch == null) {
            System.out.println("No such branch exists");
            return;
        } else {
            if (head.branchPointer.equals(branchName)) {
                System.out.println("No need to checkout the current branch");
                return;
            }
            Commit currCommit = readObj("commits", head.commitPointer);
            File dir = new File(currentDir);
            Commit newCommit = readObj("commits", branch.commitPointer);
            for (File file: dir.listFiles()) {
                if (!file.isDirectory() && !currCommit.nameToBlob.containsKey(file.getName())
                        && newCommit.nameToBlob.containsKey(file.getName())) {
                    System.out.println("There is an untracked file in"
                            + " the way; delete it or add it first.");
                    return;
                }
            }

            Branch head2 = new Branch(branch.commitPointer, "head", branch.branchName);
            for (File file: dir.listFiles()) {
                if (!file.isDirectory()) {
                    file.delete();
                }
            }

            Commit branchHead = readObj("commits", branch.commitPointer);
            for (String key : branchHead.nameToBlob.keySet()) {
                File outFile = new File(currentDir + "/" + key);
                Blob blob = readObj("blobs", branchHead.nameToBlob.get(key));
                Utils.writeContents(outFile, blob.getByteCode());
            }
        }
    }

    public <T> T readObj(String obj, String id) {
        T out;
        String fid = id;
        if (obj.equals("commits") && id.length() < 9) {
            File dir = new File(currentDir + "/.gitlet/commits");
            int fileLen = id.length();
            for (File file : dir.listFiles()) {
                if (file.getName().substring(0, id.length()).equals(id)) {
                    fid = file.getName();
                }
            }
        }
        File inFile = new File(currentDir, ".gitlet/" + obj + "/" + fid);
        try {
            ObjectInputStream inp =
                    new ObjectInputStream(new FileInputStream(inFile));
            out = (T) inp.readObject();
            inp.close();
            return out;
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    public void branch(String name) {
        Branch head = getHead();
        try {
            Branch newBranch = new Branch(head.commitPointer, name);
        } catch (IllegalStateException e) {
            System.out.println("A branch with that name already exists.");
        }
    }

    public void rmBranch(String branchName) {
        File deleteFile = new File(currentDir, ".gitlet/branches/" + branchName);
        deleteFile.delete();
    }

    public void reset(String commitID) {
        Commit commit = readObj("commits", commitID);
        if (commit == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        File dir = new File(currentDir);
        Branch head = getHead();
        Commit currCommit = readObj("commits", head.commitPointer);
        for (File file: dir.listFiles()) {
            if (!currCommit.nameToBlob.containsKey(file.getName())
                    && commit.nameToBlob.keySet().contains(file.getName())) {
                System.out.println("There is an untracked file in "
                        + "the way; delete it or add it first.");
                return;
            }
        }
        for (File file: dir.listFiles()) {
            if (!file.isDirectory()) {
                file.delete();
            }
        }
        for (String name : commit.nameToBlob.keySet()) {
            checkout2(commitID, name);
        }
        //clear staging area
        File dir2 = Utils.join(currentDir,".gitlet","stagingArea");
        for (File file: dir2.listFiles()) {
            if (!file.isDirectory()) {
                file.delete();
            }
        }
        Branch branch = readObj("branches", head.branchPointer);
//        File file = new File(currentDir + "/.gitlet/branches/" + branch.branchName);
//        file.delete();
        Branch newHead = new Branch(commitID, "head", branch.branchName);
        rmBranch(branch.branchName);
        Branch newBranch = new Branch(commitID, branch.branchName);
    }

    public void merge(String branchName) {

        Boolean conflict = false;
        List<String> workDir = plainFilenamesIn(currentDir);
        List<String> stagFile = plainFilenamesIn(stageIn);
        List<String> branchFile = plainFilenamesIn(Utils.join(currentDir,".gitlet","branches"));
        if(!branchFile.contains(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        Branch head = getHead();
        if(head.branchName.equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }

        Commit tempCommit = readObj("commits", head.commitPointer);//head commit
        Commit currCommit = readObj("commits", head.commitPointer);
        Branch goal = readObj("branches", branchName);
        Commit goalCommit = readObj("commits", goal.commitPointer);
        Commit splitCommit = readObj("commits", goal.commitPointer);

        if(stagFile.size() != 0) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        for (String f :workDir) {
            if (!currCommit.nameToBlob.containsKey(f)) {
                System.out.println("There is an untracked file in the way; delete it or add it first.");
                return;
            }
        }

        ArrayList<String> headCommitLine = new ArrayList<>();
        while (!tempCommit.msg.equals("initial commit")) {
            headCommitLine.add(tempCommit.ID);
            tempCommit = readObj("commits", tempCommit.parentCode);
        }
        while (!headCommitLine.contains(splitCommit.ID)) {
            splitCommit = readObj("commits", splitCommit.parentCode);
        }
        if (splitCommit.ID.equals(goalCommit.ID)) {
            System.out.println("Given branch is an ancestor of the current branch.");//pass
            return;
        }
        if (currCommit.ID.equals(splitCommit.ID)) {
            System.out.println("Current branch fast-forwarded.");//pass
            Branch newHead = new Branch(goalCommit.ID, "head", head.branchPointer);
            File toDelete = new File(currentDir +"/.gitlet/branches/" + head.branchPointer);
            toDelete.delete();
            Branch branch2 = new Branch(goalCommit.ID, head.branchPointer);
            return;
        }

        ArrayList<String> modSinceSP = new ArrayList<>();
        for (String key :goalCommit.nameToBlob.keySet()) {
            if (splitCommit.nameToBlob.keySet().contains(key)
                    && currCommit.nameToBlob.keySet().contains(key)) {//all contain same filename

                //given modified, curr not modified    not merge conflict
                if (!splitCommit.nameToBlob.get(key).equals(goalCommit.nameToBlob.get(key))
                        && currCommit.nameToBlob.get(key).equals(splitCommit.nameToBlob.get(key))) {
                    checkout2(goalCommit.ID, key);
                    add(key);
                }

                else if(!currCommit.nameToBlob.get(key).equals(splitCommit.nameToBlob.get(key))//diff file content
                        && !goalCommit.nameToBlob.get(key).equals(splitCommit.nameToBlob.get(key))
                        && !goalCommit.nameToBlob.get(key).equals(currCommit.nameToBlob.get(key))) {

                    System.out.println("Encountered a merge conflict.");
                    conflict = true;
                    File mergedFile = Utils.join(currentDir, key);
                    File goalFile = Utils.join(currentDir, ".gitlet","blobs",goalCommit.nameToBlob.get(key));
                    mergedFile = mergeHelper(mergedFile,goalFile);
                    return;

                }

            }

            if (!splitCommit.nameToBlob.keySet().contains(key) //not in split, only in given
                    && !currCommit.nameToBlob.containsKey(key)) {
                checkout2(goalCommit.ID, key);
                add(key);
            }

            //not in split, in cur&goal diff content
            if (currCommit.nameToBlob.containsKey(key)
                    && !currCommit.nameToBlob.get(key).equals(goalCommit.nameToBlob.get(key))) {
                System.out.println("Encountered a merge conflict.");
                conflict = true;
                File mergedFile = Utils.join(currentDir, key);
                File goalFile = Utils.join(currentDir, ".gitlet","blobs",goalCommit.nameToBlob.get(key));
                mergedFile = mergeHelper(mergedFile,goalFile);
                return;
                //merged file
            }

        }

        for (String key :splitCommit.nameToBlob.keySet()) {  //in split

            if((!currCommit.nameToBlob.containsKey(key) && goalCommit.nameToBlob.containsKey(key)
                    && !goalCommit.nameToBlob.get(key).equals(splitCommit.nameToBlob.get(key))))
                    //modified in goal not in curr
            {
                System.out.println("Encountered a merge conflict.");
                conflict = true;
                File mergedFile = Utils.join(currentDir, key);
                File goalFile = Utils.join(currentDir, ".gitlet","blobs",goalCommit.nameToBlob.get(key));
                mergedFile = mergeTail(mergedFile,goalFile);
                return;
                //merged file
            }

            if((currCommit.nameToBlob.containsKey(key) && !goalCommit.nameToBlob.containsKey(key)
                    && !currCommit.nameToBlob.get(key).equals(splitCommit.nameToBlob.get(key))))
                    //modified in curr not in goal
            {
                System.out.println("Encountered a merge conflict.");
                conflict = true;
                File mergedFile = Utils.join(currentDir, key);
                File goalFile = new File(currentDir,"empty");
                mergedFile = mergeHead(mergedFile);
                goalFile.delete();
                return;
                //merged file
            }


            if (currCommit.nameToBlob.containsKey(key)//in split, unmodified in curr, absent in given
                    && currCommit.nameToBlob.get(key).equals(splitCommit.nameToBlob.get(key))
                    && !goalCommit.nameToBlob.containsKey(key)) {
                rm(key);
            }

            if(!conflict) {
                Commit newCommit = new Commit(currCommit.ID,"Merged "
                        + head.branchPointer + " with " + branchName + ".");
            }
        }
    }

    public File mergeHead(File mergedFile) {

        File copiedFile = Utils.join(currentDir, "merging");
        try {
            CopyFile(mergedFile, copiedFile);
        } catch (IOException e) {
            System.out.println("IOException copying merge");
        }

        try {
            FileWriter mergeWriter = new FileWriter(mergedFile, false);
            mergeWriter.write("<<<<<<< HEAD\n");
            mergeWriter.flush();
        } catch (IOException e) {
            System.out.println("IOException mergeWriter");
        }

        try {
            CopyFile(copiedFile, mergedFile);
        } catch (IOException e) {
            System.out.println("IOException copying merge");
        }

        try {
            FileWriter mergeWriter = new FileWriter(mergedFile, true);
            mergeWriter.write("=======\n");
            mergeWriter.flush();
        } catch (IOException e) {
            System.out.println("IOException mergeWriter");
        }

        try {
            FileWriter mergeWriter = new FileWriter(mergedFile, true);
            mergeWriter.write(">>>>>>>\n");
            mergeWriter.flush();
        } catch (IOException e) {
            System.out.println("IOException mergeWriter");
        }

        copiedFile.delete();
        return mergedFile;
    }

    public File mergeTail(File mergedFile, File goalFile) {

        File copiedFile = Utils.join(currentDir, "merging");
        try {
            CopyFile(mergedFile, copiedFile);
        } catch (IOException e) {
            System.out.println("IOException copying merge");
        }

        try {
            FileWriter mergeWriter = new FileWriter(mergedFile, false);
            mergeWriter.write("<<<<<<< HEAD\n");
            mergeWriter.flush();
        } catch (IOException e) {
            System.out.println("IOException mergeWriter");
        }

        try {
            FileWriter mergeWriter = new FileWriter(mergedFile, true);
            mergeWriter.write("=======\n");
            mergeWriter.flush();
        } catch (IOException e) {
            System.out.println("IOException mergeWriter");
        }

        try {
            CopyFile(goalFile, mergedFile);
        } catch (IOException e) {
            System.out.println("IOException copying merge");
        }

        try {
            FileWriter mergeWriter = new FileWriter(mergedFile, true);
            mergeWriter.write(">>>>>>>");
            mergeWriter.flush();
        } catch (IOException e) {
            System.out.println("IOException mergeWriter");
        }

        copiedFile.delete();
        return mergedFile;
    }


    public File mergeHelper(File mergedFile, File goalFile) {

        File copiedFile = Utils.join(currentDir, "merging");
        try {
            CopyFile(mergedFile, copiedFile);
        } catch (IOException e) {
            System.out.println("IOException copying merge");
        }

        try {
            FileWriter mergeWriter = new FileWriter(mergedFile, false);
            mergeWriter.write("<<<<<<< HEAD\n");
            mergeWriter.flush();
        } catch (IOException e) {
            System.out.println("IOException mergeWriter");
        }

        try {
            CopyFile(copiedFile, mergedFile);
        } catch (IOException e) {
            System.out.println("IOException copying merge");
        }

        try {
            FileWriter mergeWriter = new FileWriter(mergedFile, true);
            mergeWriter.write("=======\n");
            mergeWriter.flush();
        } catch (IOException e) {
            System.out.println("IOException mergeWriter");
        }

        try {
            CopyFile(goalFile, mergedFile);
        } catch (IOException e) {
            System.out.println("IOException copying merge");
        }

        try {
            FileWriter mergeWriter = new FileWriter(mergedFile, true);
            mergeWriter.write(">>>>>>>\n");
            mergeWriter.flush();
        } catch (IOException e) {
            System.out.println("IOException mergeWriter");
        }

        copiedFile.delete();
        return mergedFile;
    }


    public void CopyFile(File inFile, File outFile) throws IOException {

            FileInputStream in = null;
            FileOutputStream out = null;

            try {
                in = new FileInputStream(inFile);
                out = new FileOutputStream(outFile);

                int c;
                while ((c = in.read()) != -1) {
                    out.write(c);
                }
            } finally
             {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            }

    }



    public Branch getHead() {
        Branch head;
        File inFile = new File(currentDir, ".gitlet/branches/head");
        try {
            ObjectInputStream inp =
                    new ObjectInputStream(new FileInputStream(inFile));
            head = (Branch) inp.readObject();
            inp.close();
            return head;
        } catch (IOException | ClassNotFoundException e) {
            head = null;
            System.out.println("Serializing error");
            return null;
        }
    }
}
