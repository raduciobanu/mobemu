package mobemu.statistics;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

/**
 * Created by radu.dragan on 6/1/2017.
 */
public class WriterWrapper {
    private static boolean ENABLED = false;
    PrintWriter writer;

    public void openFile(String fileName){
        if(!ENABLED)
            return;

        PrintWriter writer = null;

        try {
            writer = new PrintWriter(fileName, "UTF-8");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        System.out.println(fileName + " opened!");
    }

    public void println(Double value){
        if(!ENABLED)
            return;
        writer.println(value);
    }

    public void println(String value){
        if(!ENABLED)
            return;

        writer.println(value);
    }

    public void closeFile(){
        if(!ENABLED)
            return;

        writer.close();
    }
}
