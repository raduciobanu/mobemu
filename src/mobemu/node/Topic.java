/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.node;

import java.util.Set;

/**
 * Class representing an interest topic (tag).
 *
 * @author Radu
 */
public class Topic {

    private int topic; // ID of the topic
    private long time; // time when the topic becomes active

    /**
     * Instantiates a {@code Topic} object.
     *
     * @param topic the topic ID
     * @param time the time when the topic becomes active
     */
    public Topic(int topic, long time) {
        this.topic = topic;
        this.time = time;
    }

    /**
     * Gets the topic's ID.
     *
     * @return the topic's ID
     */
    public int getTopic() {
        return topic;
    }

    /**
     * Gets the topic's timestamp.
     *
     * @return the topic's timestamp
     */
    public long getTime() {
        return time;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Topic) {
            Topic other = (Topic) obj;

            return other.topic == topic;
        }

        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 61 * hash + this.topic;
        return hash;
    }

    /**
     * Checks whether a given topic is present in a topic list at a given time.
     *
     * @param topicList list of topics
     * @param topic topic to be searched
     * @param time current time
     * @return {@code true} if the topic is in the list at the given time, {@code false}
     * otherwise
     */
    public static boolean isTopicCommon(Set<Topic> topicList, Topic topic, long time) {

        for (Topic listItem : topicList) {
            if (listItem.topic == topic.topic && listItem.time <= time && topic.time <= time) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks whether a given topic is present in a topic list at a given time.
     *
     * @param topicList list of topics
     * @param topicID ID of the topic to be searched
     * @param time current time
     * @return {@code true} if the topic is in the list at the given time, {@code false}
     * otherwise
     */
    public static boolean isTopicCommon(Set<Topic> topicList, int topicID, long time) {

        for (Topic listItem : topicList) {
            if (listItem.topic == topicID && listItem.time <= time) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks whether a topic list has any topics at a given time
     *
     * @param topicList topic list
     * @param time current time
     * @return {@code true} if the topic list is not empty at a given time, {@code false}
     * otherwise
     */
    public static boolean hasTopics(Set<Topic> topicList, long time) {

        for (Topic listItem : topicList) {
            if (listItem.time <= time) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the size of the topic list at a given time.
     *
     * @param topicList topic list whose size is required
     * @param time time when the topic list's size is required
     * @return the size of the topic list at the given time
     */
    public static int getTopicsSize(Set<Topic> topicList, long time) {
        int size = 0;

        for (Topic listItem : topicList) {
            if (listItem.time <= time) {
                size++;
            }
        }

        return size;
    }

    /**
     * Gets a topic at a given position.
     *
     * @param topicList list of topics
     * @param time time when the topic is required
     * @param index index of the topic
     * @return topic at the given position
     */
    public static Topic getTopicAt(Set<Topic> topicList, long time, int index) {
        int currentIndex = 0;

        for (Topic listItem : topicList) {
            if (listItem.time <= time) {
                if (currentIndex++ == index) {
                    return listItem;
                }
            }
        }

        return null;
    }
    
    @Override
    public String toString()
    {
        return "" + topic +  "@" + time;
    }
}
