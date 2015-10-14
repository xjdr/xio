package com.xjeffrose.xio.client;

import com.xjeffrose.xio.client.retry.TracerDriver;
import java.util.concurrent.TimeUnit;

/**
 * Utility to time a method or portion of code
 */
public class TimeTrace
{
    private final String name;
    private final TracerDriver driver;
    private final long startTimeNanos = System.nanoTime();

    /**
     * Create and start a timer
     *
     * @param name name of the event
     * @param driver driver
     */
    public TimeTrace(String name, TracerDriver driver)
    {
        this.name = name;
        this.driver = driver;
    }

    /**
     * Record the elapsed time
     */
    public void commit()
    {
        long        elapsed = System.nanoTime() - startTimeNanos;
        driver.addTrace(name, elapsed, TimeUnit.NANOSECONDS);
    }
}
