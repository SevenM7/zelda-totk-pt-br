package org.aphronatus.msbt.utils;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

public class ByteBufferFile {
    private ByteBuffer fileBuffer;

    public ByteBufferFile(String path, String mode, ByteOrder order) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(path, mode)) {
            fileBuffer = ByteBuffer.allocate((int) file.length()).order(order);
            file.getChannel().read(fileBuffer);
            fileBuffer.position(0);
        }
    }

    public ByteBufferFile(String path) throws IOException {
        this(path, "r", ByteOrder.LITTLE_ENDIAN);
    }

    public int readInt() {
        return fileBuffer.getInt();
    }

    public short readShort() {
        return fileBuffer.getShort();
    }

    public byte[] read(int length) {
        byte[] bytes = new byte[length];
        fileBuffer.get(bytes);
        return bytes;
    }

    public ByteBuffer read(int length, ByteOrder order) {
        return ByteBuffer.wrap(read(length)).order(order);
    }

    public String readString(int length, Charset charset) {
        byte[] bytes = new byte[length];
        fileBuffer.get(bytes);
        return new String(bytes, charset);
    }

    public void skip(int length) {
        int position = fileBuffer.position();
        fileBuffer.position(position + length);
    }

    public void align(int alignment) {
        int position = fileBuffer.position();
        int offset = position % alignment;
        if (offset != 0) {
            fileBuffer.position(position + (alignment - offset));
        }
    }

    public void position(int position) {
        fileBuffer.position(position);
    }

    public int position() {
        return fileBuffer.position();
    }

}
