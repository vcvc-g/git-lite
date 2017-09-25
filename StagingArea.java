/**
 * Created by Andy Hyunouk Ko on 7/13/2017.
 */
package gitlet;
import java.io.*;
import java.util.*;

public class StagingArea implements Serializable {
    HashMap<String, Blob> nameToBlob = new HashMap<>();
    String currDir = System.getProperty("user.dir");
    File dir = new File(currDir + "/.gitlet/stagingArea");


    public void clear() {
        for (File file : dir.listFiles()) {
            file.delete();
        }
        for (String key : nameToBlob.keySet()) {
            nameToBlob.remove(key);
        }
    }

    public void copy() throws IOException {
        String stage = currDir + "/.gitlet/stagingArea";
        String blobs = currDir + "/.gitlet/blobs";
        File stageFolder = new File(stage);
        File blobsFolder = new File(blobs);
        String[] files = stageFolder.list();
        for (int i = 0; i < files.length; i++) {
            copyFile(new File(stageFolder, files[i]),
                    new File(blobsFolder, files[i]));
        }
    }

    public void copyFile(File file1, File file2) throws IOException {
        InputStream in = new FileInputStream(file1);
        OutputStream out = new FileOutputStream(file2);
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
    }

}
