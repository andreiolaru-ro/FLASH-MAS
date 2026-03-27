package net.xqhs.flash.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import net.xqhs.util.logging.DumbLogger;
import net.xqhs.util.logging.Logger;

/**
 * A generic multi-queue processor that manages multiple queues with a single worker thread. Each queue has its own
 * processor function that is called for objects in that queue.
 * <p>
 * The processor uses a round-robin strategy to fairly process items from all queues. When all queues are empty, the
 * worker thread waits efficiently until new items are added.
 * <p>
 * Queues can be <i>suspended</i> so that the processor for that queue is not called until the queue is <i>resumed</i>.
 * <p>
 * This class is thread-safe for adding items to queues. The addProcessor method is not designed to be called
 * concurrently from multiple threads.
 *
 * @param <T>
 *            the type of objects to be processed
 */
public class Dispatcher<T> {
	
	/**
	 * List of blocking queues, one for each processor. The queue at index i corresponds to the processor at index i in
	 * the processors list.
	 */
	protected final List<BlockingQueue<T>>	queues;
	/**
	 * List of processor functions that handle objects from their corresponding queues. The processor at index i
	 * processes items from the queue at index i in the queues list.
	 */
	protected final List<Consumer<T>>		processors;
	/**
	 * List of boolean flags indicating whether each queue is suspended. A suspended queue (true) will not be processed
	 * until it is resumed. The flag at index i corresponds to the queue at index i.
	 */
	protected final List<Boolean>			suspended;
	/**
	 * The single worker thread that processes items from all queues in round-robin fashion. This thread is created when
	 * the first processor is added and runs until shutdown() is called.
	 */
	protected Thread						workerThread;
	/**
	 * Atomic boolean flag indicating whether the worker thread should continue running. Set to true when the first
	 * processor is added, and false when shutdown() is called.
	 */
	protected final AtomicBoolean			running;
	/**
	 * Lock object used for wait/notify mechanism to efficiently pause the worker thread when all queues are empty and
	 * wake it up when new items are added.
	 */
	protected final Object					lock;
	/**
	 * The logger to use.
	 */
	protected Logger						log;
	
	/**
	 * Constructs a new Dispatcher with no queues or processors. The worker thread will be created when the first
	 * processor is added via addProcessor().
	 * 
	 * @param log
	 *            - the {@link Logger} instance to use. if <code>null</code>, a {@link DumbLogger} will be used.
	 */
	public Dispatcher(Logger log) {
		this.queues = new ArrayList<>();
		this.processors = new ArrayList<>();
		this.suspended = new ArrayList<>();
		this.running = new AtomicBoolean(false);
		this.lock = new Object();
		this.log = log != null ? log : new DumbLogger("<Dispatch>");
	}
	
	/**
	 * Adds a new queue with its corresponding processor function. The queue will be created in an active
	 * (non-suspended) state. Starts the worker thread if this is the first queue being added.
	 * <p>
	 * The processor function will be called for each item added to this queue. If the processor throws an exception, it
	 * will be caught and logged, and processing will continue with the next item.
	 *
	 * @param processor
	 *            the function that will process objects from this queue
	 * @return the index of the newly added queue, which should be used when calling add()
	 */
	public int addProcessor(Consumer<T> processor) {
		return addProcessor(processor, false);
	}
	
	/**
	 * Adds a new queue with its corresponding processor function, optionally in suspended state. Starts the worker
	 * thread if this is the first queue being added.
	 * <p>
	 * The processor function will be called for each item added to this queue, unless the queue is suspended. If the
	 * processor throws an exception, it will be caught and logged, and processing will continue with the next item.
	 *
	 * @param processor
	 *            the function that will process objects from this queue
	 * @param startSuspended
	 *            if true, the queue will be created in suspended state and will not process items until resume() is
	 *            called
	 * @return the index of the newly added queue, which should be used when calling add()
	 */
	public int addProcessor(Consumer<T> processor, boolean startSuspended) {
		queues.add(new LinkedBlockingQueue<>());
		processors.add(processor);
		suspended.add(Boolean.valueOf(startSuspended));
		
		int index = queues.size() - 1;
		
		// Start worker thread if this is the first queue
		if(index == 0) {
			startWorkerThread();
		}
		
		return index;
	}
	
	/**
	 * Asynchronously adds an object to the queue at the specified index. This method returns immediately after adding
	 * the item to the queue. The item will be processed by the worker thread when it reaches this queue in the
	 * round-robin cycle.
	 * <p>
	 * This method is thread-safe and can be called concurrently from multiple threads.
	 *
	 * @param queueIndex
	 *            the index of the queue to add the object to
	 * @param item
	 *            the item to add to the queue
	 * @throws IndexOutOfBoundsException
	 *             if queueIndex is invalid
	 */
	public void add(int queueIndex, T item) {
		if(queueIndex < 0 || queueIndex >= queues.size())
			throw new IndexOutOfBoundsException("Invalid queue index: " + queueIndex);
		queues.get(queueIndex).offer(item);
		
		// Notify the worker thread that there's work to do
		synchronized(lock) {
			lock.notify();
		}
	}
	
	/**
	 * Shuts down the processor, stopping the worker thread gracefully. The worker thread will finish processing any
	 * items currently being processed before stopping. Items remaining in queues will not be processed.
	 * <p>
	 * This method blocks until the worker thread has fully terminated. After shutdown, the processor cannot be
	 * restarted.
	 */
	public void stop() {
		running.set(false);
		synchronized(lock) {
			lock.notify();
		}
		
		if(workerThread != null) {
			try {
				workerThread.join();
			} catch(InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}
	
	/**
	 * Returns the number of queues currently managed by this processor. This is equal to the number of times
	 * addProcessor() has been called.
	 *
	 * @return the number of queues
	 */
	public int getQueueCount() {
		return queues.size();
	}
	
	/**
	 * Returns the number of items currently waiting in the specified queue. This count represents items that have been
	 * added but not yet processed.
	 * <p>
	 * Note: This is a snapshot of the queue size at the time of the call. The actual size may change immediately after
	 * this method returns due to concurrent processing or additions.
	 *
	 * @param queueIndex
	 *            the index of the queue to query
	 * @return the number of items in the specified queue
	 * @throws IndexOutOfBoundsException
	 *             if queueIndex is invalid
	 */
	public int getItemCount(int queueIndex) {
		if(queueIndex < 0 || queueIndex >= queues.size())
			throw new IndexOutOfBoundsException("Invalid queue index: " + queueIndex);
		return queues.get(queueIndex).size();
	}
	
	/**
	 * Suspends processing for the specified queue. Items can still be added to a suspended queue, but they will not be
	 * processed until the queue is resumed. If the queue is already suspended, this method has no effect.
	 * <p>
	 * This method is thread-safe and can be called while the worker thread is running.
	 *
	 * @param queueIndex
	 *            the index of the queue to suspend
	 * @throws IndexOutOfBoundsException
	 *             if queueIndex is invalid
	 */
	public void suspend(int queueIndex) {
		if(queueIndex < 0 || queueIndex >= queues.size())
			throw new IndexOutOfBoundsException("Invalid queue index: " + queueIndex);
		if(isSuspended(queueIndex))
			log.lw("Queue [] already suspended.", Integer.valueOf(queueIndex));
		else
			suspended.set(queueIndex, Boolean.TRUE);
	}
	
	/**
	 * Resumes processing for the specified queue. If the queue has pending items, they will begin to be processed in
	 * the next round-robin cycle. If the queue is already active, this method has no effect.
	 * <p>
	 * This method is thread-safe and can be called while the worker thread is running. If all queues were suspended and
	 * this resumes the first queue, the worker thread will be notified to wake up.
	 *
	 * @param queueIndex
	 *            the index of the queue to resume
	 * @throws IndexOutOfBoundsException
	 *             if queueIndex is invalid
	 */
	public void resume(int queueIndex) {
		if(queueIndex < 0 || queueIndex >= queues.size())
			throw new IndexOutOfBoundsException("Invalid queue index: " + queueIndex);
		if(!isSuspended(queueIndex))
			log.lw("Queue [] was not suspended.", Integer.valueOf(queueIndex));
		else {
			suspended.set(queueIndex, Boolean.FALSE);
			// Notify the worker thread that there may be work to do
			synchronized(lock) {
				lock.notify();
			}
		}
	}
	
	/**
	 * Checks whether the specified queue is currently suspended.
	 *
	 * @param queueIndex
	 *            the index of the queue to check
	 * @return true if the queue is suspended, false if it is active
	 * @throws IndexOutOfBoundsException
	 *             if queueIndex is invalid
	 */
	public boolean isSuspended(int queueIndex) {
		if(queueIndex < 0 || queueIndex >= queues.size())
			throw new IndexOutOfBoundsException("Invalid queue index: " + queueIndex);
		return suspended.get(queueIndex).booleanValue();
	}
	
	/**
	 * Starts the worker thread that processes items from all queues. This is called automatically when the first
	 * processor is added. Sets the running flag to true and creates a new daemon thread.
	 */
	protected void startWorkerThread() {
		running.set(true);
		workerThread = new Thread(this::processQueues, "Dispatcher-Worker");
		workerThread.start();
	}
	
	/**
	 * The main processing loop that runs in the worker thread. Continuously cycles through all queues in round-robin
	 * fashion, processing one item from each queue that has items available. When all queues are empty, the thread
	 * waits on the lock object until notified that new items have been added.
	 * <p>
	 * If a processor throws an exception, the exception is caught, logged to System.err, and processing continues with
	 * the next item.
	 * <p>
	 * The loop continues until the running flag is set to false by shutdown().
	 */
	protected void processQueues() {
		int currentQueueIndex = 0;
		
		while(running.get()) {
			boolean processedAny = false;
			
			// Round-robin through all queues
			for(int i = 0; i < queues.size(); i++) {
				currentQueueIndex = (currentQueueIndex + 1) % queues.size();
				// Skip suspended queues
				if(suspended.get(currentQueueIndex).booleanValue())
					continue;
				BlockingQueue<T> queue = queues.get(currentQueueIndex);
				Consumer<T> processor = processors.get(currentQueueIndex);
				
				T item = queue.poll();
				if(item != null) {
					processedAny = true;
					try {
						processor.accept(item);
					} catch(Exception e) {
						// Log and continue processing
						log.le("Error processing item in queue []:", Integer.valueOf(currentQueueIndex),
								PlatformUtils.printException(e));
						e.printStackTrace();
					}
				}
			}
			
			// If no items were processed, wait for notification
			if(!processedAny) {
				synchronized(lock) {
					try {
						lock.wait();
					} catch(InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}
		}
	}
}