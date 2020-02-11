package org.objectrepository;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

final class Queue extends ThreadPoolTaskExecutor {

    private String queueName;
    private String bash;
    private String shellScript;
    private boolean topic = false;

    Queue(String queueName, String bash, String shellScript, boolean topic) {
        this.queueName = queueName;
        this.bash = bash;
        this.shellScript = shellScript;
        setCorePoolSize(1);
        setMaxPoolSize(1);
        setQueueCapacity(1);
        setTopic(topic);
    }

    String getBash() {
        return bash;
    }

    String getShellScript() {
        return shellScript;
    }

    boolean isTopic() {
        return topic;
    }

    private void setTopic(boolean topic) {
        this.topic = topic;
    }

    @Override
    public String toString() {
        return (this.isTopic()) ? "activemq:topic:" + this.queueName : "activemq:" + this.queueName;
    }
}
