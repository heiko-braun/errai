package org.jboss.errai.bus.server.async;

import static java.lang.System.currentTimeMillis;

/**
 * A <tt>TimedTask</tt> is used for scheduling tasks, and making sure they are run at appropriate times and intervals  
 */
public abstract class TimedTask implements Runnable, Comparable<TimedTask> {
    protected volatile long nextRuntime;
    protected long period;
    protected volatile boolean cancel = false;

    /**
     * Gets the period of the task, and when it should be run next
     *
     * @return the interval length
     */
    public long getPeriod() {
        return period;
    }

    /**
     * Sets the period in which the task should be run next
     *
     * @param period
     */
    public void setPeriod(long period) {
        this.period = period;
    }

    /**
     * Gets the time in which the task will be run
     *
     * @return the time the task will be run in milliseconds
     */
    public long nextRuntime() {
        return nextRuntime;
    }

    /**
     * Disables the task
     */
    public void disable() {
        cancel = true;
    }

    /**
     * Runs the task if the specified time is greater than the tasks runtime, and the task is still enabled. If the
     * next runtime for the task shall happen before <tt>time</tt>, nothing is done or changed
     *
     * @param time - the time in which the task should be run
     * @return true if the task was run
     */
     boolean runIfDue(long time) {
        if (!cancel && nextRuntime < time) {
            if (nextRuntime == -1) {
                return false;
            }
            run();
            if (period != -1) {
                nextRuntime = currentTimeMillis() + period;
            }
            else {
                nextRuntime = -1;
            }
            return true;
        }
        return false;
    }

    /**
     * Returns 1 if this task is to be run at a different time than the specified task <tt>o</tt>
     *
     * @param o - the timed task to compare to this task
     * @return 0 if the tasks are to be run at the same time
     */
     public int compareTo(TimedTask o) {
        if (nextRuntime > o.nextRuntime)
            return 1;
        else if (nextRuntime < o.nextRuntime)
            return 1;
        else
            return 0;
    }
}