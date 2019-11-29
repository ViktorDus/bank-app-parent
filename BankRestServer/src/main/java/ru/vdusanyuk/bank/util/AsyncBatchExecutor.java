package ru.vdusanyuk.bank.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * This class implements asynchronous task execution with specified delay for batch collecting.
 *
 * @param <T> Type of items to be processed
 */
public class AsyncBatchExecutor<T> {

    /**
     * Items batch collecting size
     */
    private static final int BATCH_PROCESSING_SIZE = 100;

    /**
     * Items batch collecting size
     */
    private static final int BATCH_PROCESSING_INTERVAL = 200;


    /**
     * Inner Scheduled Executor which implements separate thread execution and delay logic
     */
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    /**
     * This executor inner state flag
     */
    private final AtomicBoolean isTaskProcessing = new AtomicBoolean(false);

    /**
     * Queue implementation of items to process
     */
    private final BlockingQueue<T> queue = new LinkedBlockingDeque<>();

    /**
     * Task processing behavior provided
     */
    private final Consumer<? super Collection<T>> task;

    /**
     * Task processing delay (Tasks will start processing after this delay will expire)
     */
    private final int taskProcessingInterval;

    /**
     * Constructor
     *
     * @param task                Consumer which provides collection of items processing logic
     */
    public AsyncBatchExecutor(Consumer<? super Collection<T>> task) {
        this.task = task;
        this.taskProcessingInterval = BATCH_PROCESSING_INTERVAL;
    }

    /**
     * Add item for async processing
     *
     * @param item item to process
     */
    public void addProcessingItem(T item) {
        queue.add(item);

        if (isTaskProcessing.compareAndSet(false, true)) {
            submitTask();
        }
    }

     /**
     * updateState method is used to control inner state after async jobs was done
     */
    private void updateState() {
        if (queue.isEmpty()) {
            isTaskProcessing.set(false);
        } else {
            submitTask();
        }
    }

    /**
     * Submits current queue of items to process in separate thread after provided delay period
     */
    private void submitTask() {
        executor.schedule(this::processTask, taskProcessingInterval, TimeUnit.MILLISECONDS);
    }

    /**
     * Executes batch collecting and processing with callback calling after all
     */
    private void processTask() {
        try {
            List<T> batch = new ArrayList<>(BATCH_PROCESSING_SIZE);
            while (!queue.isEmpty() && batch.size() < BATCH_PROCESSING_SIZE) {
                batch.add(queue.poll());
            }
            task.accept(batch);
        } finally {
            updateState();
        }
    }
}
