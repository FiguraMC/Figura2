package org.figuramc.figura.model.renderers.vbo;

import org.figuramc.figura.util.exception.ThrowingConsumer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL46;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Helper for the part data SSBO that's used to transfer part data into the optimized vertex shader
 */
public class PartDataStorageBuffer {

    private final StorageBufferUpdater updater = new StorageBufferUpdater();
    private int bufferHandle = -1;

    public void build(int partDataCount) {
        // Free the previous buffer handle, if there was one
        if (bufferHandle != -1)
            GL46.glDeleteBuffers(bufferHandle);
        // Create a new buffer with the proper size
        bufferHandle = GL46.glCreateBuffers();
        GL46.glNamedBufferStorage(bufferHandle, (long) partDataCount * PartDataStruct.SIZE, GL46.GL_DYNAMIC_STORAGE_BIT);
        int err = GL46.glGetError();
        if (err != GL46.GL_NO_ERROR)
            throw new IllegalStateException("ERROR!!! " + err );
        // Resize the updater
        updater.resize(partDataCount, bufferHandle);
    }

    // Run the consumer with a storage buffer updater, and apply the changes it makes.
    public <E extends Throwable> void updatePartData(ThrowingConsumer<StorageBufferUpdater, E> updateFunction) throws E {
        // Reset the updater, use it for whatever the caller wants, then apply its changes
        updater.reset();
        updateFunction.accept(updater);
        updater.applyChanges(bufferHandle);
    }

    // Bind the SSBO to the given buffer index.
    public void bind(int bufferIndex) {
        if (bufferHandle == -1) throw new IllegalStateException("Attempt to upload PartDataStorageBuffer that isn't created");
        GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, bufferIndex, bufferHandle);
    }

    // Call this at the end or else memory leak
    public void clear() {
        if (bufferHandle == -1)
            GL46.glDeleteBuffers(bufferHandle);
    }

    /**
     * An object which is used for updating the storage buffer!
     */
    public static class StorageBufferUpdater {
        private int maxElements = -1;
        private PartDataStruct[] dataStructs; // The data in structs
        private boolean[] changed; // The indices which have been changed
        private ByteBuffer nativeBuffer; // A native buffer which will store bytes for the data structs

        // Provides a reference to the data at the given index, and marks it as changed
        public PartDataStruct updatePartData(int index) {
            changed[index] = true;
            return dataStructs[index];
        }

        private void resize(int newSize, int newBufferHandle) {
            if (newSize > maxElements) {
                // Resize the arrays if needed
                PartDataStruct[] newArray = new PartDataStruct[newSize];
                changed = new boolean[newSize];
                if (dataStructs != null) {
                    System.arraycopy(dataStructs, 0, newArray, 0, dataStructs.length);
                }
                for (int i = dataStructs == null ? 0 : dataStructs.length; i < newSize; i++)
                    newArray[i] = new PartDataStruct();
                dataStructs = newArray;
                ByteBuffer newNativeBuffer = BufferUtils.createByteBuffer(newSize * PartDataStruct.SIZE);
                if (nativeBuffer != null) {
                    nativeBuffer.clear();
                    newNativeBuffer.put(nativeBuffer);
                    newNativeBuffer.clear();
                }
                nativeBuffer = newNativeBuffer;
                // Upload everything to initialize the new buffer
                uploadAll(newBufferHandle);
            }
            maxElements = newSize;
        }

        // Run once just to initialize with identity matrices
        private void uploadAll(int bufferHandle) {
            for (int i = 0; i < dataStructs.length; i++)
                dataStructs[i].write(nativeBuffer, i);
            GL46.glNamedBufferSubData(bufferHandle, 0, nativeBuffer);
        }

        // Reset everything to changed being false
        private void reset() {
            Arrays.fill(changed, false);
        }

        // Merge threshold for regions. A value of "1" means that:
        // [updated, updated, not updated, updated, ...] will be merged into a single glBufferSubData call,
        // even though the central one was not updated, because it might be more efficient than
        // splitting it into 2 separate glBufferSubData calls.
        // 0 will never merge, -1 will always merge (so there's only one glBufferSubData call every time)
        // TODO: Tune value?
        private static final int REGION_MERGE_THRESHOLD = 1;

        // Apply all changes using glBufferSubData into the handle.
        // Batches adjacent calls together according to the REGION_MERGE_THRESHOLD.
        private void applyChanges(int bufferHandle) {
            // I feel like I'm back in school with all this int math
            int index = 0;
            int regionStart = index;
            int regionEnd = index;
            int untilThreshold = 0;
            while (index < maxElements) {
                if (changed[index]) {
                    // This was changed, so update it in the native buffer
                    untilThreshold = REGION_MERGE_THRESHOLD;
                    dataStructs[index].write(nativeBuffer, index);
                    index++;
                    regionEnd = index;
                } else if (untilThreshold != 0) {
                    // Don't end the search yet - just decrement the threshold and move on
                    untilThreshold--;
                    index++;
                } else if (regionEnd - regionStart > 0) {
                    // We ran out of threshold and didn't find a changed element, so send out the glBufferSubData!
                    int offset = regionStart * PartDataStruct.SIZE;
                    ByteBuffer slice = nativeBuffer.slice(offset, (regionEnd - regionStart) * PartDataStruct.SIZE);
                    GL46.glNamedBufferSubData(bufferHandle, offset, slice);
                    int err = GL46.glGetError();
                    if (err != GL46.GL_NO_ERROR)
                        throw new IllegalStateException("ERROR!!! " + err );
                    // Inc index
                    index++;
                } else {
                    // We're not inside a region right now, so just increment the index and bump the region start.
                    index++;
                    regionStart = index + 1;
                }
            }
            // If by the end we still have a region to upload, then do so
            if (regionEnd - regionStart > 0) {
                int offset = regionStart * PartDataStruct.SIZE;
                ByteBuffer slice = nativeBuffer.slice(offset, (regionEnd - regionStart) * PartDataStruct.SIZE);
                GL46.glNamedBufferSubData(bufferHandle, offset, slice);
                int err = GL46.glGetError();
                if (err != GL46.GL_NO_ERROR)
                    throw new IllegalStateException("ERROR!!! " + err );
            }

        }
    }

}
