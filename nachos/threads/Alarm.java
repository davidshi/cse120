package nachos.threads;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep until a certain
 * time.
 */
public class Alarm
{
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm()
	{
		alarmThreads = new ConcurrentSkipListMap<Long, KThread>();
		Machine.timer().setInterruptHandler(new Runnable()
		{
			public void run()
			{
				timerInterrupt();
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer periodically
	 * (approximately every 500 clock ticks). Causes the current thread to yield, forcing a context
	 * switch if there is another thread that should be run.
	 */
	public void timerInterrupt()
	{
		boolean intStatus = Machine.interrupt().disable();// disable all interrupts
		//while still threads in the map that should be woken up
		while ((!alarmThreads.isEmpty()) && (alarmThreads.firstKey() <= Machine.timer().getTime()) )
		{
			Lib.debug(dbgAlarm, "Machine Time is:  " + Machine.timer().getTime());
			Lib.debug(dbgAlarm, "Thread Wakeup Time is: " + alarmThreads.firstKey());
			if((alarmThreads.firstEntry().getValue() != null))//if the thread is not null
			{
				alarmThreads.firstEntry().getValue().ready();//wake the thread up
				alarmThreads.remove(alarmThreads.firstKey());//remove from the map
			}
		}
		
		KThread.currentThread().yield();//forces a context switch
		Machine.interrupt().restore(intStatus);// restore all interrupts

	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up in the timer
	 * interrupt handler. The thread must be woken up (placed in the scheduler ready set) during the
	 * first timer interrupt where . A thread calls waitUntil to suspend its own execution until
	 * time has advanced to at least now + x. This is useful for threads that operate in real-time,
	 * for example, for blinking the cursor once per second. There is no requirement that threads
	 * start running immediately after waking up; just put them on the ready queue in the timer
	 * interrupt handler after they have waited for at least the right amount of time. Do not fork
	 * any additional threads to implement waitUntil(); you need only modify waitUntil() and the
	 * timer interrupt handler. waitUntil is not limited to one thread; any number of threads may
	 * call it and be suspended at any one time. Note however that only one instance of Alarm may
	 * exist at a time (due to a limitation of Nachos).
	 * 
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x
	 *            the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	/* zack p1t3 */
	public void waitUntil(long x)
	{
		// for now, cheat just to get something working (busy waiting is bad)
		// long wakeTime = Machine.timer().getTime() + x;
		/*
		 * while (wakeTime > Machine.timer().getTime()) KThread.yield();
		 */

		boolean intStatus = Machine.interrupt().disable();// disable all interrupts
		alarmThreads.put(Machine.timer().getTime() + x, KThread.currentThread());// add new thread/time to the hashmap
		KThread.currentThread().sleep();// block current thread
		Machine.interrupt().restore(intStatus);// restore all interrupts
	}

	private ConcurrentSkipListMap<Long, KThread> alarmThreads;//hashmap to hold all threads with their associated wait times

	private static final char dbgAlarm = 'a';
}
