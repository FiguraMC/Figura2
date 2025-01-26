package org.figuramc.figura.manage;

import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.data.AvatarImportingException;
import org.figuramc.figura.util.ErrorReporting;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class AvatarSubManager<K> {

    private final ConcurrentHashMap<K, Avatar<K>> loadedAvatars = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<K, CompletableFuture<@Nullable Avatar<K>>> inProgressAvatars = new ConcurrentHashMap<>();

    public synchronized void tick() {
        // Iterate over in-progress Avatars, see if any are complete.
        // If they are, move them to the map of loaded Avatars.
        for (var entry : inProgressAvatars.entrySet()) {
            K key = entry.getKey();
            CompletableFuture<Avatar<K>> future = inProgressAvatars.get(key);
            if (future.isDone()) {
                // Fetch the result of the future:
                Avatar<K> result;
                try {
                    // If the following getNow() call doesn't throw, then the async task finished without throwing
                    result = future.getNow(null);
                } catch (CompletionException ex) {
                    Throwable cause = ex.getCause();

                    // For now, we'll ALWAYS report to chat/console.
                    // Maybe later we'll disable this for multiplayer avatars (whenever we get to that lol)
                    switch (cause) {
                        case AvatarImportingException importingException -> ErrorReporting.avatarImporting(importingException);
                        case AvatarLoadingException loadingException -> ErrorReporting.avatarLoading(loadingException);
                        default -> ErrorReporting.unexpectedError(cause);
                    }

                    // Cancel the in-progress Avatar, since it errored
                    cancelInProgress(key);
                    continue;
                } catch (Throwable unexpected) {
                    ErrorReporting.unexpectedError(unexpected);
                    cancelInProgress(key);
                    continue;
                }

                // If no result, then just remove it from the map and end
                if (result == null) {
                    cancelInProgress(key);
                    continue;
                }

                // We have the result. Wait until it's ready-async:
                if (result.isReadyAsync()) {
                    // Once the result is completely ready:
                    // - Remove from the in-progress map and add to the loaded map
                    // - Run its main-thread initialization
                    inProgressAvatars.remove(key);
                    loadedAvatars.put(key, result);
                    result.mainThreadInitialize();
                }
            }
        }

        // Tick all loaded Avatars.
        for (Avatar<K> avatar : loadedAvatars.values())
            avatar.tick();
    }

    /**
     * Clear out the entire sub manager.
     */
    public synchronized void clear() {
        for (K key : inProgressAvatars.keySet())
            cancelInProgress(key);
        for (K key : loadedAvatars.keySet())
            unload(key);
        inProgressAvatars.clear();
        loadedAvatars.clear();
    }

    /**
     * Run the consumer on each loaded Avatar.
     */
    public synchronized void forEach(Consumer<? super Avatar<K>> consumer) {
        for (Avatar<K> avatar : loadedAvatars.values()) consumer.accept(avatar);
    }

    /**
     * Get an Avatar if it's loaded. Return null if no Avatar exists for this key.
     */
    public @Nullable Avatar<K> get(K key) { return loadedAvatars.get(key); }

    /**
     * Check if there's an avatar in progress for this key:
     */
    public boolean isInProgress(K key) { return inProgressAvatars.containsKey(key); }

    /**
     * Unload an Avatar that is currently loaded, and destroy it.
     * If there is no Avatar for the given key, does nothing.
     */
    public synchronized void unload(K key) {
        Avatar<K> oldAvatar = loadedAvatars.remove(key);
        if (oldAvatar != null) oldAvatar.destroy();
    }

    /**
     * Cancel an Avatar in progress.
     * This removes it from the in-progress map, and queues up an action on the
     * future to destroy the Avatar once it's loaded.
     * Unfortunately we can't actually cancel a future in progress, so the best we can do
     * is (asynchronously) immediately destroy the Avatar once it's done.
     */
    public synchronized void cancelInProgress(K key) {
        CompletableFuture<Avatar<K>> oldTask = inProgressAvatars.remove(key);
        if (oldTask != null) oldTask.whenComplete((avatar, error) -> {
            if (avatar != null) avatar.destroy();
        });
    }

    /**
     * Launch an async task, then hand it here for awaiting completion.
     */
    public synchronized void load(K key, CompletableFuture<@Nullable Avatar<K>> loadTask) {
        cancelInProgress(key); // Cancel any previous task
        inProgressAvatars.put(key, loadTask); // Store the new task
    }


}
