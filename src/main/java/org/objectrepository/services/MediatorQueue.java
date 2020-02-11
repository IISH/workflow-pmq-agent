package org.objectrepository.services;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.exec.*;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.Timer;

/**
 * MediatorQueue
 * <p/>
 * Listens to a queue; accepts the message and parses the message into command line parameters.
 *
 * @author Lucien van Wouw <lwo@iisg.nl>
 * @author Jozsef Gabor Bone <bonej@ceu.hu>
 */
public class MediatorQueue implements Runnable {

    final private static Logger log = Logger.getLogger(MediatorQueue.class);
    private static final int StatusCodeTaskReceipt = 300;
    private static final int StatusCodeTaskComplete = 500;
    private static final int StatusCodeTaskCompleteWithError = 450;
    private static final int StatusCodeTaskError = 400;
    private static final int MESSAGE_SIZE = 10000;

    private HttpClientService httpClientService;
    private ConsumerTemplate consumer;
    private String messageQueue;
    private String shellScript;
    private String bash;
    private long heartbeatInterval;
    private ProducerTemplate producer;

    public MediatorQueue(HttpClientService httpClientService, ConsumerTemplate consumer, ProducerTemplate producer, String messageQueue, String bash, String shellScript, long heartbeatInterval) {
        this.httpClientService = httpClientService;
        this.consumer = consumer;
        this.producer = producer;
        this.messageQueue = messageQueue;
        this.shellScript = shellScript;
        this.bash = bash;
        this.heartbeatInterval = heartbeatInterval;
    }

    @Override
    public void run() {

        log.debug("Start listening to " + messageQueue);
        String identifier = consumer.receiveBody(messageQueue, String.class);
        if (identifier == null) return;
        log.debug("Message received from " + messageQueue + " : " + identifier);
        log.info("Message received: " + identifier);
        HeartBeats.message(httpClientService, messageQueue, StatusCodeTaskReceipt, "Task received", identifier, 0);

        final long start = new Date().getTime();
        Timer timer = new Timer();
        long TIMER_DELAY = 10;
        timer.scheduleAtFixedRate(new HeartBeat(httpClientService, messageQueue, StatusCodeTaskReceipt, identifier, start), TIMER_DELAY, heartbeatInterval);


        final DefaultExecutor executor = new DefaultExecutor();
        //Using Std out for the output/error stream - to do it later...
        //http://stackoverflow.com/questions/621596/how-would-you-read-image-data-in-from-a-program-like-image-magick-in-java
        final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        executor.setStreamHandler(new PumpStreamHandler(stdout));
        executor.setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT));
        executor.setProcessDestroyer(new ShutdownHookProcessDestroyer());

        //Executing the command
        final CommandLine commandLine = Commandizer.makeCommand(bash, shellScript, identifier);
        final DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        log.debug("Executing command: " + commandLine.toString());
        try {
            executor.execute(commandLine, resultHandler);
        } catch (Exception e) {
            timer.cancel();
            HeartBeats.message(httpClientService, messageQueue, StatusCodeTaskError, e.getMessage(), identifier, -1);
            //producer.sendBody(identifier);
            log.info(e.getMessage());
            return;
        }

        //Anything after this will be executed only when the task completes
        boolean ok = false;
        try {
            do {
                resultHandler.waitFor(heartbeatInterval);
                final String info = info(stdout.toString());
                HeartBeats.message(httpClientService, messageQueue, StatusCodeTaskReceipt, info, identifier, 0);
            } while (!resultHandler.hasResult());
            ok = true;
        } catch (InterruptedException e) {
            HeartBeats.message(httpClientService, messageQueue, StatusCodeTaskError, e.getMessage(), identifier, -1);
            //producer.sendBody(identifier);
            log.error(e.getMessage());
        } finally {
            boolean interrupted = Thread.interrupted();
            log.info("interrupted " + interrupted);
        }
        timer.cancel();
        if (ok) {
            log.info("resultHandler.exitValue=" + resultHandler.getExitValue());
            final String info = info(stdout.toString());
            int status = (resultHandler.getExitValue() == 0) ? StatusCodeTaskComplete : StatusCodeTaskCompleteWithError;
            if (resultHandler.getExitValue() == 255) {
                status = -StatusCodeTaskCompleteWithError;
            }
            HeartBeats.message(httpClientService, messageQueue, status, info, identifier, resultHandler.getExitValue());
            //producer.sendBody(identifier);
        }
    }

    private static String info(String text) {
        int length = text.length();
        if (length > MESSAGE_SIZE) {
            return text.substring(length - MESSAGE_SIZE);
        } else {
            return text;
        }
    }

}
