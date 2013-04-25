package nachos.threads;

import nachos.machine.*;

/**
 * A multi-threaded OS kernel.
 */
public class ThreadedKernel extends Kernel {
	/**
	 * Allocate a new multi-threaded kernel.
	 */
	public ThreadedKernel() {
		super();
	}
  
	/**
	 * Initialize this kernel. Creates a scheduler, the first thread, and an
	 * alarm, and enables interrupts. Creates a file system if necessary.
	 */
	public void initialize(String[] args) {
		// set scheduler
		String schedulerName = Config.getString("ThreadedKernel.scheduler");
		scheduler = (Scheduler) Lib.constructObject(schedulerName);
    
		// set fileSystem
		String fileSystemName = Config.getString("ThreadedKernel.fileSystem");
		if (fileSystemName != null)
			fileSystem = (FileSystem) Lib.constructObject(fileSystemName);
		else if (Machine.stubFileSystem() != null)
			fileSystem = Machine.stubFileSystem();
		else
			fileSystem = null;
    
		// start threading
		new KThread(null);
    
		alarm = new Alarm();
    
		Machine.interrupt().enable();
	}
  
	/**
	 * Test this kernel. Test the <tt>KThread</tt>, <tt>Semaphore</tt>,
	 * <tt>SynchList</tt>, and <tt>ElevatorBank</tt> classes. Note that the
	 * autograder never calls this method, so it is safe to put additional tests
	 * here.
	 */
	public void selfTest() {
    // My additional tests.
    Lib.enableDebugFlags("cj");
		commTest.runTest();
		joinTest.runTest();
    
    // Pre-defined tests.
		KThread.selfTest();
		Semaphore.selfTest();
		SynchList.selfTest();
		//Alarm.selfTest();
		//Communicator.selfTest();
		//Condition2.selfTest();
		if (Machine.bank() != null) {
			ElevatorBank.selfTest();
		}
	}
  
	/**
	 * A threaded kernel does not run user programs, so this method does
	 * nothing.
	 */
	public void run() {
	}
  
	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() {
		Machine.halt();
	}
  
	/** Globally accessible reference to the scheduler. */
	public static Scheduler scheduler = null;
  
	/** Globally accessible reference to the alarm. */
	public static Alarm alarm = null;
  
	/** Globally accessible reference to the file system. */
	public static FileSystem fileSystem = null;
  
	// dummy variables to make javac smarter
	private static RoundRobinScheduler dummy1 = null;
  
	private static PriorityScheduler dummy2 = null;
  
	private static LotteryScheduler dummy3 = null;
  
	private static Condition2 dummy4 = null;
  
	private static Communicator dummy5 = null;
  
	private static Rider dummy6 = null;
  
	private static ElevatorController dummy7 = null;
	public static class alarmTest 
	{
		public static KThread controller = null;
		public static KThread a = null;
		public static KThread b = null;
		public static Alarm alarm = new Alarm();
	}
	
	/**
	 * Exercises my implementation of the listen and speak method inside
	 * nachos.threads.Communication.
   *
   * @author, Tutor Brett Ryan, CSE 120, Spring 2013
	 */
	public static class commTest {
		public static KThread controller = null;
		public static KThread a = null;
		public static KThread b = null;
		public static Communicator comm = new Communicator();
    
		/**
		 * Defines a program that first sends a message and then recieves a
		 * message.
		 */
		private static class ProgramA implements Runnable {
			public void run() {
				// Send message to ProgramB
				comm.speak(1);
				
				// Recieve message from ProgramB
				Lib.debug(dbgThread, "2 == " + comm.listen());
        
				// Restart controller.
				if (--runningThreads == 0) {
					Machine.interrupt().disable();
					controller.ready();
				}
			}
		}
    
		/**
		 * Defines a program that first recieves a message and then sends a
		 * message.
		 */
		private static class ProgramB implements Runnable {
			public void run() {
				// Recieve message from ProgramA
				Lib.debug(dbgThread, "1 == " + comm.listen());
				
				// Send message to ProgramA
				comm.speak(2);
        
				// Restart controller.
				if (--runningThreads == 0) {
					Machine.interrupt().disable();
					controller.ready();
				}
			}
		}
    
		/**
		 * Runs this test.
		 */
		public static void runTest() {
			Lib.debug(dbgThread, "Entering ThreadedKernel.commTest.runTest");
      
			// Define the controller in global variable.
			controller = KThread.currentThread();
      
			// Create Threads
			a = new KThread(new ProgramA()).setName("Thread A");
			b = new KThread(new ProgramB()).setName("Thread B");
      
			a.fork();
			b.fork();
			runningThreads = 2;
      
			// Set the currently running processes to wait.
			Machine.interrupt().disable();
			controller.sleep();
      
			Lib.debug(dbgThread, "Exiting ThreadedKernel.commTest.runTest");
		}
    
		/**
		 * Character used to enable output for this test. Turned on by a call to
		 * Machine.enableDebugFlags(String s) where s includes the flag defined
		 * below.
		 */
		private static final char dbgThread = 'c';
    
		/**
		 * Keeps track of the number of running threads in test.
		 */
		private static int runningThreads = 0;
	}
  
	/**
	 * Exercises my implementation of the join method. Note: This was designed
	 * with the RoundRobinScheduler and will behave very differently depending
	 * on the type of scheduling algoirhtm being used.
   *
   * @author, Tutor Brett Ryan, CSE 120, Spring 2013
	 */
	public static class joinTest {
    
		/**
		 * Local static variables so the threads can communicate.
		 */
		public static KThread controller = null;
		public static KThread a = null;
		public static KThread b = null;
		public static KThread c = null;
    
		/**
		 * Program A: Based on the implementation of runTest(), since this
		 * recieved the first call to fork, it should be the first to execute
		 * (given a RoundRobin implementation) and will print a message. Since
		 * there is no pre-emption, it will start ProgramB with a call to
		 * b.join(). When B is done executing, A should then finish it's program
		 * and print it's closing message.
		 */
		private static class ProgramA implements Runnable {
			public void run() {
				Lib.debug(dbgThread, " Starting Program A");
				b.join();
				Lib.debug(dbgThread, " Ending Program A");
        
				// Restart controller.
				if (--runningThreads == 0) {
					Lib.debug(dbgThread, " Exiting Join Test Correctly");
					Machine.interrupt().disable();
					controller.ready();
				}
			}
		}
    
		/**
		 * Program B: Based on the implementation of runTest(), since this
		 * recieved the second call to fork, it should execute (given a
		 * RoundRobin implementation) once ProgramA calls b.join(). Once started
		 * it will print a message. Since there is no pre-emption, it will start
		 * ProgramC with a call to c.join(). When C is done executing, B should
		 * then finish it's program and print it's closing message and then the
		 * scheduler will jump back to the only remaining ready thread, the
		 * thread running ProgramA.
		 */
		private static class ProgramB implements Runnable {
			public void run() {
				Lib.debug(dbgThread, " Starting Program B");
				c.join();
				Lib.debug(dbgThread, " Ending Program B");
        
				// Restart controller.
				if (--runningThreads == 0) {
					Lib.debug(dbgThread,
                    " ERROR: Exiting Join Test Incorrectly");
					Machine.interrupt().disable();
					controller.ready();
				}
			}
		}
    
		/**
		 * Program C: Based on the implementation of runTest(), since this
		 * recieved the final call to fork, it should be the last to execute
		 * (given a RoundRobin implementation) and will print a message once
		 * stated. Since there is no pre-emption, it will complete it's entire
		 * execution. Even if there was pre-emption, it is the only ready thread
		 * and that is why a call to yield is performed. ProgramC should yield
		 * to itself.
		 */
		private static class ProgramC implements Runnable {
			public void run() {
				Lib.debug(dbgThread, " Starting Program C ");
				KThread.currentThread().yield(); // Should yield to self.
				Lib.debug(dbgThread, " Ending Program C ");
        
				// Restart controller.
				if (--runningThreads == 0) {
					Lib.debug(dbgThread,
                    " ERROR: Exiting Join Test Incorrectly");
					Machine.interrupt().disable();
					controller.ready();
				}
			}
		}
    
		/**
		 * Runs this test.
		 */
		public static void runTest() {
			Lib.debug(dbgThread, "Entering ThreadedKernel.joinTest.runTest");
      
			// Define the controller in global variable.
			controller = KThread.currentThread();
      
			// Create Threads
			a = new KThread(new ProgramA()).setName("Thread A");
			b = new KThread(new ProgramB()).setName("Thread B");
			c = new KThread(new ProgramC()).setName("Thread C");
      
			a.fork();
			b.fork();
			c.fork();
			runningThreads = 3;
      
			// Set the currently running processes to wait.
			Machine.interrupt().disable();
			controller.sleep();
      
			Lib.debug(dbgThread, "Exiting ThreadedKernel.joinTest.runTest");
		}
    
		/**
		 * Character used to enable output for this test. Turned on by a call to
		 * Machine.enableDebugFlags(String s) where s includes the flag defined
		 * below.
		 */
		private static final char dbgThread = 'j';
    
		/**
		 * Keeps track of the number of running threads in test.
		 */
		private static int runningThreads = 0;
	}
}
