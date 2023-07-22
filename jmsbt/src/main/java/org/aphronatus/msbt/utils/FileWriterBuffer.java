package org.aphronatus.msbt.utils;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class FileWriterBuffer implements AutoCloseable {

    private final RandomAccessFile file;

    public FileWriterBuffer(File file) {
        try {
            this.file = new RandomAccessFile(file, "rw");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void write(byte[] bytes) {
        try {
            file.write(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public long size() {
        try {
            return file.length();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void position(int position) {
        try {
            file.seek(position);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void align(int alignment, byte fill) {
        try {
            int position = (int) file.getFilePointer();
            int offset = position % alignment;
            if (offset != 0) {
                int paddingSize = alignment - offset;
                byte[] padding = new byte[paddingSize];
                Arrays.fill(padding, fill);

                file.write(padding);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
            file.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
