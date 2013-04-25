package nachos.threads;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.PriorityQueue;

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
		waitingThreads = new PriorityQueue<AlarmThread>();
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
		Iterator it = waitingThreads.iterator();
		//AlarmThread[] alarmThreads = Arrays.sort(waitingThreads.toArray());
		while (it.hasNext())// while there are still threads
		{
			AlarmThread alarm = (AlarmThread) it.next();
			Lib.debug(dbgAlarm, "Alarm Time: " + Long.toString(alarm.time));
			Lib.debug(dbgAlarm, "Machine Time: " + Long.toString( Machine.timer().getTime()));
			if (alarm.time <= Machine.timer().getTime())// if another thread should be run
			{
				Lib.debug(dbgAlarm, "ready fucks up?");
				alarm.thread.ready();// context switch
				Lib.debug(dbgAlarm, "timer interrupt runsB?");
			}
			else
			{
				Lib.debug(dbgAlarm, "break?");
				break;
			}
		}
		Lib.debug(dbgAlarm, "out of it?");
		Machine.interrupt().restore(intStatus);// restore all interrupts
		Lib.debug(dbgAlarm, "what");
		KThread.yield();
		Lib.debug(dbgAlarm, "yield not");

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
		// add new thread/time to the queue of threads waiting
		waitingThreads.add(new AlarmThread(KThread.currentThread(), Machine.timer().getTime() + x));
		KThread.currentThread().sleep();// block current thread
		//KThread.sleep();
		Machine.interrupt().restore(intStatus);// restore all interrupts
	}

	/**
	 * Class to represent alarm threads that are in the wait cycle
	 */
	/* zack p1t3 */
	private class AlarmThread implements Comparable<AlarmThread>
	{
		private KThread thread;// alarm thread that is waiting
		private long time;// number of clock ticks to wait

		public AlarmThread(KThread k, long t)// constructor to set thread and time
		{
			this.thread = k;
			this.time = t;
		}

		@Override
		public int compareTo(AlarmThread a)
		{
			if (time < a.time)
			{
				return -1;
			}
			if (time > a.time)
			{
				return 1;
			} else
			{
				return 0;
			}
		}
	}

	// priority queue to hold alarm threads, by time
	PriorityQueue<AlarmThread> waitingThreads;

	private static final char dbgAlarm = 'a';

	/*public static void selfTest()
	{
		AlarmTest.runTest();
	}*/
	
	/**
	 * Run sanity check on Alarm.waitUntil
	 */
	public static void selfTest() {
		Lib.debug(dbgAlarm, "Alarm Self Test");

		// Test that alarm wakes up thread after proper amount of time
		KThread thread = new KThread(new Runnable() {
			public void run() {
				final long ticks = 1000;
				long sleepTime = Machine.timer().getTime();
				//Lib.debug(dbgAlarm, "In run");
				ThreadedKernel.alarm.waitUntil(ticks);
				//Lib.debug(dbgAlarm, "Wait Until runs");
				long wakeTime = Machine.timer().getTime();

				Lib.debug(dbgAlarm, (((wakeTime-sleepTime>=ticks) ? "[PASS]" : "[FAIL]") + ": Thread slept at least " + ticks + " ticks " + sleepTime + "->" + wakeTime));
			}
		});
		thread.fork();
		thread.join();

		// Test that several sleeping threads wake up in order
		KThread threadA = new KThread(new TestSeqThread('A',100));
		KThread threadB = new KThread(new TestSeqThread('B',700));
		KThread threadC = new KThread(new TestSeqThread('C',1400));

		threadA.fork(); threadB.fork(); threadC.fork();
		threadA.join(); threadB.join(); threadC.join();

		Lib.debug(dbgAlarm, (TestSeqThread.wakeSequence.equals("ABC") ? "[PASS]" : "[FAIL") + ": Threads woke up in order (" + TestSeqThread.wakeSequence + ")");
	}

	/**
	 * For testing:
	 * Thread which immediately sleeps and keeps a static record
	 * of the order in which it and its siblings wake up
	 */
	private static class TestSeqThread implements Runnable {
		char myName;
		long mySleepTicks;

		static String wakeSequence = "";
		static Lock lock = new Lock();

		public TestSeqThread(char name, long sleepTicks) {
			myName = name;
			mySleepTicks = sleepTicks;
		}

		public void run() {
			ThreadedKernel.alarm.waitUntil(mySleepTicks);
			lock.acquire();
			wakeSequence = wakeSequence + myName;
			lock.release();
		}
	}
}
