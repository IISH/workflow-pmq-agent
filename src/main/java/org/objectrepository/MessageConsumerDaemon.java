/*
 * Copyright 2010 International Institute for Social History, The Netherlands.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.objectrepository;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.log4j.Logger;
import org.objectrepository.services.HttpClientService;
import org.objectrepository.services.MediatorQueue;
import org.springframework.context.support.GenericXmlApplicationContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.RejectedExecutionHandler;

public class MessageConsumerDaemon extends Thread implements Runnable {

    private static MessageConsumerDaemon instance;
    private boolean keepRunning = true;
    private GenericXmlApplicationContext context;
    private long timer;
    private long timerInterval = 10000;
    private long heartbeatInterval = 10000;
    private List<Queue> taskExecutors;
    final private static Logger log = Logger.getLogger(MessageConsumerDaemon.class);
    private boolean pause = false;
    private boolean stop = false;

    private MessageConsumerDaemon() {
        timer = System.currentTimeMillis() + timerInterval;
    }

    @Override
    public synchronized void start() {
        init();
        while (keepRunning) {
            if (isStop()) {
                for (Queue queue : taskExecutors) {
                    keepRunning = keepRunning && (queue.getActiveCount() != 0);
                }
            } else {
                for (Queue queue : taskExecutors) {
                    if (!isPause() || queue.isTopic()) {
                        if (queue.getActiveCount() < queue.getMaxPoolSize()) {
                            log.debug(queue + " has activeCount " + queue.getActiveCount() + " / maxPoolSize " + queue.getMaxPoolSize());
                            queue.execute(mediatorInstance(queue));
                        }
                    }
                }
            }
            heartbeat();
        }
        context.close();
    }

    private void init() {

        log.info("Startup service...");
        GenericXmlApplicationContext context = new GenericXmlApplicationContext();
        context.setValidating(false);
        context.load("/META-INF/spring/application-context.xml", "META-INF/spring/dispatcher-servlet.xml");
        context.refresh();
        setContext(context);
        context.registerShutdownHook();

        final RejectedExecutionHandler rejectedExecutionHandler = context.getBean(RejectedExecutionHandler.class);
        for (Queue taskExecutor : taskExecutors) {
            taskExecutor.setRejectedExecutionHandler(rejectedExecutionHandler);
            taskExecutor.initialize();
            log.info("Initialized " + taskExecutor);
        }
    }

    private Runnable mediatorInstance(Queue queue) {
        log.info("Adding " + queue);
        return new MediatorQueue(context.getBean(HttpClientService.class), context.getBean(ConsumerTemplate.class), context.getBean(ProducerTemplate.class), queue.toString(), queue.getBash(), queue.getShellScript(), heartbeatInterval);
    }

    /**
     * heartbeat
     * <p/>
     * Keeps the overall environment from overworking by pausing every ten seconds or so.
     */
    private void heartbeat() {

        long currentTime = System.currentTimeMillis();
        if (timer - currentTime < 0) {
            timer = currentTime + timerInterval;
            if (isPause()) {
                log.info("We are in pause mode.");
            } else {
                log.info("Actively listening to queues.");
            }
        }

        try {
            sleep(1000);
        } catch (InterruptedException e) {
            interrupt();
        } catch (Throwable t) {
            log.error("Cannot put thread to sleep..."); // Can we ignore this ?
        }
    }

    /**
     * setTaskExecutors
     * <p/>
     * Sets the queues.
     */
    private void setTaskExecutors(List<Queue> taskExecutors) {
        this.taskExecutors = taskExecutors;
    }

    /**
     * Method clone should not be allowed for a singleton.
     *
     * @return The cloned object that never will be returned
     * @throws CloneNotSupportedException We cannot clone a singleton object.
     */
    @Override
    public Object clone()
            throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    private static synchronized MessageConsumerDaemon getInstance(List<Queue> queues, long heartbeatInterval) {

        if (instance == null) {
            instance = new MessageConsumerDaemon();
            instance.setTaskExecutors(queues);
            instance.setDaemon(true);
            instance.setHeartbeatInterval(heartbeatInterval);
        }
        return instance;
    }

    /**
     * main
     * <p/>
     * Accepts one folder as argument: --message_queues
     * That folder ought to contain one or more folders ( or symbolic links ) to the files
     * The folder has the format: [foldername] or [foldername].[maxTasks]
     * MaxTasks is to indicate the total number of jobs being able to run.
     * <p/>
     * long
     */
    public static void main(String[] argv) throws FileNotFoundException {

        if (instance == null) {
            final Properties properties = new Properties();

            if (argv.length > 0) {
                for (int i = 0; i < argv.length; i += 2) {
                    try {
                        properties.put(argv[i], argv[i + 1]);
                    } catch (ArrayIndexOutOfBoundsException arr) {
                        System.out.println("Missing value after parameter " + argv[i]);
                        System.exit(-1);
                    }
                }
            }
            // Environmental variables override properties.
            final String messageQueues = System.getenv("MESSAGE_QUEUES");
            if (messageQueues != null) {
                properties.setProperty("--message_queues", messageQueues);
            }

            final String heartbeatinterval = System.getenv("HEARTBEAT_INTERVAL");
            if (heartbeatinterval != null) {
                properties.setProperty("--heartbeatinterval", heartbeatinterval);
            }

            if (properties.size() == 0) {
                log.fatal("Usage: pmq-agent.jar --message_queues [queues] --heartbeatinterval [interval in ms]\n" +
                        "or use environmental variables MESSAGE_QUEUES and " +
                        "The queues is a folder that contains symbolic links to the startup scripts.");
                System.exit(-1);
            }

            if (log.isInfoEnabled()) {
                log.info("Arguments set: ");
                for (String key : properties.stringPropertyNames()) {
                    log.info("'" + key + "'='" + properties.getProperty(key) + "'");
                }
            }

            if (!properties.containsKey("--message_queues")) {
                log.fatal("Expected case sensitive parameter: --message_queues");
                System.exit(-1);
            }

            final File message_queues = new File((String) properties.get("--message_queues"));
            if (!message_queues.exists()) {
                log.fatal("Cannot find folder for message_queues: " + message_queues.getAbsolutePath());
                System.exit(-1);
            }

            if (message_queues.isFile()) {
                log.fatal("--message_queues should point to a folder, not a file: " + message_queues.getAbsolutePath());
                System.exit(-1);
            }

            final String bash = (String) properties.get("-bash");
            if (bash != null && !new File(bash).exists()) {
                log.fatal("Bash not found: " + bash);
                System.exit(-1);
            }

            long heartbeatInterval = 5000;
            if (properties.containsKey("--heartbeatinterval")) {
                heartbeatInterval = Long.parseLong((String) properties.get("heartbeatinterval"));
            }

//            final String CYGWIN_HOME = System.getenv("CYGWIN_HOME");
//            final String[] scriptNames = (properties.containsKey("-startup"))
//                    ? new String[]{properties.getProperty("-startup")}
//                    : new String[]{"/startup.sh", "\\startup.bat"};

            final List<Queue> queues = registerQueues(bash, message_queues, "message", new ArrayList<>());
            getInstance(registerQueues(bash, message_queues, "topic", queues), heartbeatInterval).start();
        }

        System.exit(0);
    }

    private static List<Queue> registerQueues(String bash, File message_queues, String type, List<Queue> queues) throws FileNotFoundException {

        final String CYGWIN_HOME = System.getenv("CYGWIN_HOME");
        final String namespace = System.getenv("NAMESPACE");
        final String[] scriptNames = {"/startup.sh", "\\startup.bat"};

        final boolean isTopic = type.equalsIgnoreCase("topic");
        final File folder = new File(message_queues, type);
        final File[] files = folder.listFiles();
        if (files == null) {
            throw new FileNotFoundException("No such folder: " + folder.getAbsolutePath());
        } else
            for (File file : files) {
                if (file.isDirectory()) {
                    final String name = file.getName();
                    final String[] split = name.split("\\.", 2); // myqueuename.1
                    final String _queueName = split[0];
                    if (_queueName.equalsIgnoreCase(".")) // hidden folder
                        continue;

                    final String queueName = (namespace == null) ? _queueName : namespace + "_" + _queueName;

                    for (String scriptName : scriptNames) {
                        final String _shellScript = file.getAbsolutePath() + scriptName;
                        final String shellScript = (CYGWIN_HOME == null)
                                ? _shellScript
                                : _shellScript.substring(CYGWIN_HOME.length()).replace("\\", "/");
                        final int maxTask = (split.length == 1) ? 1 : Integer.parseInt(split[1]);
                        if (new File(_shellScript).exists()) {
                            final Queue queue = new Queue(queueName, bash, shellScript, isTopic);
                            queue.setCorePoolSize(1);
                            queue.setMaxPoolSize(maxTask);
                            queue.setQueueCapacity(1);
                            queues.add(queue);
                            log.info("Candidate mq client for " + queue + " maxTasks " + maxTask);
                            break;
                        } else {
                            log.warn("... skipping, because no startup script found at " + shellScript);
                        }
                    }
                }
            }

        if (queues.size() == 0) {
            log.warn("No queue folders seen in " + folder.getAbsolutePath());
        }

        return queues;
    }

    private void setContext(GenericXmlApplicationContext context) {
        this.context = context;
    }

    private boolean isPause() {
        return pause;
    }

    private boolean isStop() {
        return stop;
    }

    private void setHeartbeatInterval(long heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }
}
