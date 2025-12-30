package com.patelheet.paradiseteamchat.managers;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Manages asynchronous task execution to prevent database operations from
 * blocking the main server thread.
 */
public class AsyncTaskManager {
    private final ParadiseTeamChatPlugin plugin;

    /**
     * Constructor for AsyncTaskManager.
     * 
     * @param plugin The main plugin instance.
     */
    public AsyncTaskManager(ParadiseTeamChatPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialise the async task manager. Will remain empty as Bukkit's scheduler
     * handles async tasks natively, but this method is here for consistency and
     * logic.
     */
    public void initialise() {
        plugin.getLogger().info("AsyncTaskManager initialised successfully.");
    }

    /**
     * Run a task asynchronously (off the main thread).
     * 
     * @param task The task to run asynchronously.
     * @return A CompletableFuture that completes when the task finishes.
     */
    public CompletableFuture<Void> runAsync(Runnable task) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                task.run();
                future.complete(null);
            } catch (Exception e) {
                plugin.getLogger().severe("Error in async task: " + e.getMessage());
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /**
     * Run a task asynchronously and return a result.
     * 
     * @param <T>      The type of result to return.
     * @param supplier The task to run that produces a result.
     * @return A CompletableFuture containing the result.
     */
    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                T result = supplier.get();
                future.complete(result);
            } catch (Exception e) {
                plugin.getLogger().severe("Error in async supplier task: " + e.getMessage());
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /**
     * Run a task on the main server thread. This is important for tasks that
     * interact with the Bukkit API, such as updating caches or messages.
     * 
     * @param task The task to run on the main thread.
     */
    public void runSync(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    /**
     * Run a task on the main thread after a delay.
     * 
     * @param task  The task to run.
     * @param delay The delay in ticks (20 ticks = 1 second).
     * @return The BukkitTask that was scheduled.
     */
    public BukkitTask runSyncLater(Runnable task, long delay) {
        return Bukkit.getScheduler().runTaskLater(plugin, task, delay);
    }

    /**
     * Run a task repeatedly on the main thread.
     * 
     * @param task   The task to run.
     * @param delay  Initial delay before first execution (ticks).
     * @param period Delay between subsequent executions (ticks).
     * @return The BukkitTask that was scheduled.
     */
    public BukkitTask runSyncRepeating(Runnable task, long delay, long period) {
        return Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
    }

    /**
     * Common pattern: Run database operation async, then update cache on main
     * thread.
     * 
     * @param <T>            The type of result from the database operation.
     * @param databaseTask   The async database operation (returns result).
     * @param mainThreadTask What to do with the result on the main thread.
     */
    public <T> void asyncThenSync(Supplier<T> databaseTask, Consumer<T> mainThreadTask) {
        supplyAsync(databaseTask).thenAccept(result -> {
            runSync(() -> mainThreadTask.accept(result));
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Error in asyncThenSync operation: " + throwable.getMessage());
            throwable.printStackTrace();
            return null;
        });
    }

    /**
     * Execute a database write operation asynchronously with error handling.
     * 
     * @param operation The database write operation.
     * @return CompletableFuture<Boolean> - true if successful, false otherwise.
     */
    public CompletableFuture<Boolean> executeAsync(Supplier<Boolean> operation) {
        return supplyAsync(() -> {
            try {
                return operation.get();
            } catch (Exception e) {
                plugin.getLogger().severe("Database operation failed: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Cancel all pending tasks (for plugin shutdown).
     * Note: This only cancels repeating tasks, not one-time async tasks already
     * running.
     */
    public void cancelAllTasks() {
        Bukkit.getScheduler().cancelTasks(plugin);
        plugin.getLogger().info("All scheduled tasks cancelled.");
    }
}