package org.objectrepository.services;

import org.apache.log4j.Logger;

import java.util.Date;

/**
 * HeartBeats
 * <p/>
 * Sends an update about the task.
 *
 * @author Jozsef Gabor Bone <bonej@ceu.hu>
 * @author Lucien van Wouw <lwo@iisg.nl>
 */
class HeartBeats {

    static void message(HttpClientService httpClientService, String messageQueue, int statusCode, String info,
                        String identifier, int exitValue) {

        log.info("Update status identifier:" + identifier +
                " with update exitValue:" + exitValue + ",statusCode:" + statusCode);
        httpClientService.status(identifier, messageQueue, statusCode, info, new Date());
    }

    final private static Logger log = Logger.getLogger(HeartBeats.class);
}
