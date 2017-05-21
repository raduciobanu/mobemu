package mobemu.utils.message;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by radu on 5/6/2017.
 *
 * A generic wrapper over the default ArrayList of messages
 *
 */
public abstract class MessageList <T extends IMessage> implements Iterable<T>{

    protected List<T> messageList;

    public MessageList() {
        this.messageList = new ArrayList<T>();
    }

    public boolean matches(T existingMessage, T newMessage){
        return existingMessage.getId() == newMessage.getId();
    }

    public T exists(T message){
        for(T existingMessage: messageList){
            if(matches(existingMessage, message)){
                return existingMessage;
            }
        }

        return null;
    }

    public void add(T message){
        T existingMessage = exists(message);

        //if there is no message with the given id in the list, add 'message'
        if(existingMessage == null){
            messageList.add(message);
            return;
        }

        //if there is already a newer version of the message in the list, do nothing
        if(existingMessage.getTimestamp() > message.getTimestamp()){
            return;
        }

        //remove the old message
        messageList.remove(existingMessage);

        //add the new message
        messageList.add(message);
    }

    @Override
    public Iterator<T> iterator() {
        return messageList.iterator();
    }
}
