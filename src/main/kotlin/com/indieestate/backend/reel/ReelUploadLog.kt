package com.indieestate.backend.reel

import org.slf4j.LoggerFactory

/** Filter backend logs: `grep reel-upload` or `--logging.level.reel-upload=DEBUG`. */
object ReelUploadLog {
    private val log = LoggerFactory.getLogger("reel-upload")

    fun info(message: String) = log.info(message)

    fun debug(message: String) = log.debug(message)

    fun warn(message: String) = log.warn(message)

    fun error(message: String, throwable: Throwable? = null) {
        if (throwable != null) log.error(message, throwable) else log.error(message)
    }
}
