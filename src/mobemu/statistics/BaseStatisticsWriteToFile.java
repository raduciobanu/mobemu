package mobemu.statistics;

import mobemu.node.Node;
import mobemu.utils.Constants;
import mobemu.utils.message.IMessage;
import mobemu.utils.message.IMessageGenerator;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

/**
 * Created by radu on 5/12/2017.
 */
public abstract class BaseStatisticsWriteToFile<T extends IMessage, U extends IMessageGenerator<T>> extends BaseStatistics<T, U> {

    /**
     * Used to write results to file
     */
    protected PrintWriter writer;

    public BaseStatisticsWriteToFile(U messageGenerator) {
        super(messageGenerator);
    }

    @Override
    public void runBeforeTraceStart() {
        writer = openFile(Constants.responseTimesFileName);
    }

    @Override
    public void runAfterTraceEnd(Node[] nodes) {
        closeFile(writer);
    }

    public PrintWriter openFile(String fileName){
        PrintWriter writer = null;

        try {
            writer = new PrintWriter(fileName, "UTF-8");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        System.out.println(fileName + " opened!");

        return writer;
    }

    public void closeFile(PrintWriter writer){
        writer.close();
    }
}
