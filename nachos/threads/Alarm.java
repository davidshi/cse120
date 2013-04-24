package nachos.threads;

import java.util.ArrayList;

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
		waitingThreads = new ArrayList<AlarmThread>();
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
		boolean intStatus = Machine.interrupt().disable();//disable all interrupts
		long machineTime = Machine.timer().getTime();//machine time
		
		while(!waitingThreads.isEmpty())//while there are still threads
		{
			AlarmThread alarm = waitingThreads.remove(0);//set alarm as the first thread passed
			if(alarm.time <= machineTime)//if another thread should be run
			{
				alarm.thread.ready();//context switch
			}
		}
		
		Machine.interrupt().restore(intStatus);//restore all interrupts
		
		KThread.currentThread().yield();//causes the current thread to yield
		KThread.yield();
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
		long wakeTime = Machine.timer().getTime() + x;
		/*while (wakeTime > Machine.timer().getTime())
			KThread.yield();*/
		
		boolean intStatus = Machine.interrupt().disable();//disable all interrupts
		//add new thread/time to the queue of threads waiting
		waitingThreads.add(new AlarmThread(KThread.currentThread(), wakeTime));
		KThread.currentThread().sleep();//block current thread
		KThread.sleep();
		Machine.interrupt().restore(intStatus);//restore all interrupts
	}
	
	/**
	 * Class to represent alarm threads that are in the wait cycle
	 */
	/* zack p1t3 */
	private class AlarmThread //implements Comparable<AlarmThread>
	{
		private KThread thread;//alarm thread that is waiting
		private long time;//number of clock ticks to wait
		
		public AlarmThread(KThread k, long t)//constructor to set thread and time
		{
			this.thread = k;
			this.time = t;
		}

		/*@Override
        public int compareTo(AlarmThread a)
        {
	        if(time < a.time)
	        {
	        	return -1;
	        }
	        if(time > a.time)
	        {
	        	return 1;
	        }
	        else
	        {
	        	return 0;	
	        }
        }*/
	}
	//priority queue to hold alarm threads, by time
	ArrayList<AlarmThread> waitingThreads;

}
