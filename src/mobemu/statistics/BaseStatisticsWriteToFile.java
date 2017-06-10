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
    protected WriterWrapper writerWrapper;

    public BaseStatisticsWriteToFile(U messageGenerator) {
        super(messageGenerator);
        writerWrapper = new WriterWrapper();
    }

    @Override
    public void runBeforeTraceStart() {
        writerWrapper.openFile(Constants.responseTimesFileName);
    }

    @Override
    public void runAfterTraceEnd(Node[] nodes) {
        writerWrapper.closeFile();
    }
}
