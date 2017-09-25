/**
 * Created by Andy Hyunouk Ko on 7/13/2017.
 */
package gitlet;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static gitlet.Utils.plainFilenamesIn;

public class Commit implements Serializable {
    String parentCode;
    String ID;
    HashMap<String, String> nameToBlob;
    String timeStamp;
    String msg;

    public Commit() {
        this.parentCode = null;
        this.msg = "initial commit";
        this.nameToBlob = new HashMap<>();
        this.timeStamp = currDateTime();

        ID = Utils.sha1(toBytes());
        String currentDir = System.getProperty("user.dir");
        File outFile = new File(currentDir, ".gitlet/commits/" + ID);
        try {
            ObjectOutputStream out =
                    new ObjectOutputStream(new FileOutputStream(outFile));
            out.writeObject(this);
            out.close();
        } catch (IOException excp) {
            System.out.println("Serializing error");
        }
    }

    public Commit(String parentCode, String msg) throws IllegalStateException {
        this.parentCode = parentCode;
        this.msg = msg;
        this.nameToBlob = new HashMap<String, String>();
        this.timeStamp = currDateTime();
        String currentDir = System.getProperty("user.dir");

        if (parentCode != null) {
            Commit parentCommit;
            try {
                ObjectInputStream inp =
                        new ObjectInputStream(new FileInputStream(
                                new File(currentDir, ".gitlet/commits/" + parentCode)));
                parentCommit = (Commit) inp.readObject();
                inp.close();
            } catch (IOException | ClassNotFoundException e) {
                parentCommit = null;
                System.out.println("Deserilzing parent commit errored");
            }
            nameToBlob.putAll(parentCommit.nameToBlob);
            List<String> list = plainFilenamesIn(currentDir + "/.gitlet/untrackedFiles");
            for (String elem : list) {
                nameToBlob.remove(elem);
            }
        }

        File dir = new File(currentDir, ".gitlet/stagingArea");
        File dir2 = new File(currentDir, ".gitlet/untrackedFiles");
        if (dir.listFiles().length == 0 && dir2.listFiles().length == 0) {
            throw new IllegalStateException();
        }
        for (File file : dir.listFiles()) {
            Blob out;
            try {
                ObjectInputStream inp =
                        new ObjectInputStream(new FileInputStream(file));
                out = (Blob) inp.readObject();
                inp.close();
            } catch (IOException | ClassNotFoundException e) {
                out = null;
            }
            nameToBlob.put(out.getFileName(), out.getFileID());
        }
        StagingArea area = new StagingArea();
        try {
            area.copy();
        } catch (IOException e) {
            System.out.println("Serializing error");
        }
        area.clear();
        ID = Utils.sha1(toBytes());

        //clear untrackedFiles directory.
        for (File file: dir2.listFiles()) {
            if (!file.isDirectory()) {
                file.delete();
            }
        }

        File outFile = new File(currentDir, ".gitlet/commits/" + ID);
        try {
            ObjectOutputStream out =
                    new ObjectOutputStream(new FileOutputStream(outFile));
            out.writeObject(this);
            out.close();
        } catch (IOException excp) {
            System.out.println("Serializing error");
        }

        //Change current branch's commit pointer to this commit and head's commit pointer
        Branch head = getHead();
        Branch currBranch;
        File inFile = new File(currentDir, ".gitlet/branches/" + head.branchPointer);
        try {
            ObjectInputStream inp =
                    new ObjectInputStream(new FileInputStream(inFile));
            currBranch = (Branch) inp.readObject();
            inp.close();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Deserializing failed");
            currBranch = null;
        }
        File deleteFile = new File(currentDir, ".gitlet/branches/" + currBranch.branchName);
        deleteFile.delete();
        Branch movedBranch = new Branch(ID, currBranch.branchName);
        Branch newHead = new Branch(ID, "head", currBranch.branchName);
    }



    public String currDateTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        return formatter.format(date);
    }

    public Branch getHead() {
        String currentDir = System.getProperty("user.dir");
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

    public byte[] toBytes() {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(stream);
            objectStream.writeObject(this);
            objectStream.close();
            return stream.toByteArray();
        } catch (IOException e) {
            throw new Error("Internal error serializing commit.");
        }
    }
}
