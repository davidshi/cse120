package nachos.threads;

import java.util.ArrayList;
import java.util.LinkedList;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 * 
 * Using interrupt enable and disable to provide atomicity
 * 
 * Atomicity - executes as though it could not be interrupted "all or nothing"
 * 
 * <p>
 * You must implement this.
 * 
 * @see nachos.threads.Condition
 */
public class Condition2
{
	/**
	 * Allocate a new condition variable.
	 * 
	 * @param conditionLock
	 *            the lock associated with this condition variable. The current
	 *            thread must hold this lock whenever it uses <tt>sleep()</tt>,
	 *            <tt>wake()</tt>, or <tt>wakeAll()</tt>.
	 */
	public Condition2(Lock conditionLock)
	{
		this.conditionLock = conditionLock;
		/*zack p1t2*/
		otherWaitQueue = new ArrayList<KThread>();
	}

	/**
	 * Atomically release the associated lock and go to sleep on this condition
	 * variable until another thread wakes it using <tt>wake()</tt>. The current
	 * thread must hold the associated lock. The thread will automatically
	 * reacquire the lock before <tt>sleep()</tt> returns.
	 */
	public void sleep()
	{
		/*zack p1t2*/
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());//make sure thread holds lock
		boolean intStatus = Machine.interrupt().disable();//disable all interrupts
		otherWaitQueue.add(KThread.currentThread());//stores the current thread until wake is called
		
		conditionLock.release();
		
		KThread.currentThread().sleep();//go to sleep on current thread
		Machine.interrupt().restore(intStatus);//restore all interrupts
		
		conditionLock.acquire();
	}

	/**
	 * Wake up at most one thread sleeping on this condition variable. The
	 * current thread must hold the associated lock.
	 */
	public void wake()
	{
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		
		boolean intStatus = Machine.interrupt().disable();//disable all interrupts
		
		/*zack p1t2*/
		if(!otherWaitQueue.isEmpty())//if there is still a thread in the queue, wake it up
		{
			KThread wakeUp = otherWaitQueue.remove(0);//next thread to wake
			wakeUp.ready();//wake it up
		}
		
		Machine.interrupt().restore(intStatus);//restore all interrupts
	}

	/**
	 * Wake up all threads sleeping on this condition variable. The current
	 * thread must hold the associated lock.
	 */
	public void wakeAll()
	{
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		
		boolean intStatus = Machine.interrupt().disable();//disable all interrupts
		
		/*zack p1t2*/
		while(!otherWaitQueue.isEmpty())//wake all threads up 
		{
			KThread wakeUp = otherWaitQueue.remove(0);//next thread to wake
			wakeUp.ready();//wake it up
		}
		
		Machine.interrupt().restore(intStatus);//restore all interrupts
	}
	
	private Lock conditionLock;
	/*zack p1t2*/
	private ArrayList<KThread> otherWaitQueue;//queue of wait threads
}
