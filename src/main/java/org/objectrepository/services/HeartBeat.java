package org.objectrepository.services;

import java.util.TimerTask;

/**
 * HeartBeat
 * <p/>
 * Responsible for sending back messages to the queue. Thus indicating we are still alive.
 */
public class HeartBeat extends TimerTask {

    private HttpClientService httpClientService;
    private String messageQueue;
    private int statusCode;
    private String identifier;
    private long start;

    HeartBeat(HttpClientService httpClientService, String messageQueue, int statusCode, String identifier, long start) {
        this.httpClientService = httpClientService;
        this.messageQueue = messageQueue;
        this.statusCode = statusCode;
        this.identifier = identifier;
        this.start = start;
    }

    @Override
    public void run() {
        HeartBeats.message(httpClientService, messageQueue, statusCode, "Working...", identifier, Integer.MAX_VALUE);
    }
}
