package com.custom.topicrename;

import java.util.Map;
import org.apache.kafka.common.Configurable;
import org.apache.kafka.connect.mirror.DefaultReplicationPolicy;
import org.apache.kafka.connect.mirror.ReplicationPolicy;

import java.util.regex.Pattern;
import org.apache.kafka.connect.mirror.MirrorClientConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomReplicationPolicy implements ReplicationPolicy, Configurable   {

    private static final Logger log = LoggerFactory.getLogger(CustomReplicationPolicy.class);

    // In order to work with various metrics stores, we allow custom separators.
    public static final String SEPARATOR_CONFIG = MirrorClientConfig.REPLICATION_POLICY_SEPARATOR;
    public static final String SEPARATOR_DEFAULT = "";

    private String separator = SEPARATOR_DEFAULT;
    private Pattern separatorPattern = Pattern.compile(Pattern.quote(SEPARATOR_DEFAULT));


    String SOURCE_TOPIC_NAME_FIELD = "source.topic.name";
    String TARGET_TOPIC_NAME_FIELD = "target.topic.name";
    String SOURCE_CLUSTER_ALIAS = "source.alias";
    private String sourceTopicName = "";
    private String targetTopicName = "";
    private String heartBeatTopic = "";
    private String offsetSyncsTopic = "";
    private String checkpointTopic = "";
    private String sourceClusterAlias = "";



    /**
     * Configure this class with the given key-value pairs
     */
    @Override
    public void configure(Map<String, ?> props) {
        if (props.containsKey("replication.policy.separator")) {
            this.separator = (String)props.get("replication.policy.separator");
            log.info("Using custom remote topic separator: '{}'", this.separator);
            this.separatorPattern = Pattern.compile(Pattern.quote(this.separator));
        }

        if (props.containsKey(SOURCE_TOPIC_NAME_FIELD) == true){
            sourceTopicName = (String) props.get(SOURCE_TOPIC_NAME_FIELD);
        }

        if (props.containsKey(TARGET_TOPIC_NAME_FIELD) == true){
            targetTopicName = (String) props.get(TARGET_TOPIC_NAME_FIELD);

        }
        if (props.containsKey(SOURCE_CLUSTER_ALIAS) == true){
            sourceClusterAlias =(String) props.get(SOURCE_CLUSTER_ALIAS);
        }
    }

    /** How to rename remote topics; generally should be like us-west.topic1.
      However we completly ignore sourceClusterAlias and just use targetTopicName */
    @Override
    public String formatRemoteTopic(String sourceClusterAlias, String topic) {
        if (!topic.isEmpty() && topic.equals(sourceTopicName)) {
            return targetTopicName;
        }
        return topic;
    }

    /** Source cluster alias of given remote topic, e.g. "us-west" for "us-west.topic1".
        Returns null if not a remote topic.
    However we completly ignore sourceClusterAlias and just use targetTopicName */
    @Override
    public String topicSource(String topic) {
        if (!topic.isEmpty() && topic.equals(sourceTopicName)) {
            return targetTopicName;
        }
        return null;
    }

    /** Name of topic on the source cluster, e.g. "topic1" for "us-west.topic1".
        Topics may be replicated multiple hops, so the immediately upstream topic
        may itself be a remote topic.
        Returns null if not a remote topic.
    */
    @Override
    public String upstreamTopic(String topic) {
        return topicSource(topic);
    }


    /** Internal topics are never replicated.  */
    @Override
    public boolean isInternalTopic(String topic){
        boolean isKafkaInternalTopic = topic.startsWith("__") || topic.startsWith(".");
        boolean isDefaultConnectTopic = topic.endsWith("-internal") || topic.endsWith(".internal");
        return isMM2InternalTopic(topic) || isKafkaInternalTopic || isDefaultConnectTopic;
    }

    /** Check topic is one of MM2 internal topic, this is used to make sure the topic doesn't need to be replicated. */
    @Override
    public boolean isMM2InternalTopic(String topic) {
        return topic.endsWith(".internal");
    }


    @Override
    public String offsetSyncsTopic(String clusterAlias) {
        if(offsetSyncsTopic.equals("")){
                return sourceTopicName+"-offsetSyncs.internal";
        }
        return offsetSyncsTopic;
    }



    @Override
    public String heartbeatsTopic() {
        if(heartBeatTopic.equals("")){
            return sourceTopicName+"-heartBeat.internal";
        }
        return heartBeatTopic;
    }

    @Override
    public boolean isHeartbeatsTopic(String topic) {
        return heartbeatsTopic().equals(heartBeatTopic);
    }

//    /** check if topic is a checkpoint topic.  */
    @Override
    public String checkpointsTopic(String clusterAlias) {
        if(checkpointTopic.equals("")){
            return sourceTopicName+"-checkpoint.internal";
        }
        return checkpointTopic;
    }

}
