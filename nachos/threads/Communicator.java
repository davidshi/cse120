package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit messages. Multiple threads
 * can be waiting to <i>speak</i>, and multiple threads can be waiting to <i>listen</i>. But there
 * should never be a time when both a speaker and a listener are waiting, because the two threads
 * can be paired off at this point.
 */
public class Communicator
{
	/**
	 * Allocate a new communicator.
	 */
	public Communicator()
	{
		//default values
		lock = new Lock();
		speaker = new Condition(lock);
		listener = new Condition(lock);
		message = 0;
		numberListeners = 0;
		messageReady = false;
	}

	/**
	 * Wait for a thread to listen through this communicator, and then transfer <i>word</i> to the
	 * listener.
	 * 
	 * <p>
	 * Does not return until this thread is paired up with a listening thread. Exactly one listener
	 * should receive <i>word</i>.
	 * 
	 * @param word
	 *            the integer to transfer.
	 */
	public void speak(int word)
	{
		lock.acquire();//get lock
		while (numberListeners == 0 || messageReady == true)//there is no listener or message to be sent
		{
			speaker.sleep();//wait!
		}
		messageReady = true;//set to true again
		message = word;//set the word used to the integer to transfer
		listener.wake();//tell the listener that it can take the word
		lock.release();//release lock
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return the <i>word</i> that
	 * thread passed to <tt>speak()</tt>.
	 * 
	 * @return the integer transferred.
	 */
	public int listen()
	{
        lock.acquire();//acquire the lock
        numberListeners++;//another listener is waiting
    	speaker.wake(); //use speaker
		listener.sleep();//sleep listener
		int word = message;//message to be sent
		messageReady = false;//finished using that word
		numberListeners--; //listener is done
		speaker.wake(); //use speaker again
		lock.release();//release the lock
		
		return word;//return the word that thread passed to speak       
	}

	private Lock lock;// Monitor's lock
	
	private Condition listener; // Monitor's listener 
	private Condition speaker; // Monitor's speaker 
	
	private int message; //shared word 
	private int numberListeners; //listener counter
	
	private boolean messageReady;//if the word is ready to send

}
