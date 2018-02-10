/**
 * Created by Andy Hyunouk Ko on 7/13/2017.
 */

import java.io.File;
import java.io.Serializable;

public class Blob implements Serializable{
    String ID;

    public Blob(String path){
        byteCdoe = Serializable.readContent(new File(path));
        ID = Utils.sha1(byteCode);
    }
}
