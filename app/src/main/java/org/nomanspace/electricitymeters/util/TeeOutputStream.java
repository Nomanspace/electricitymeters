package org.nomanspace.electricitymeters.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * An output stream that writes its output to two different OutputStreams.
 * Similar to the Unix 'tee' command.
 */
public class TeeOutputStream extends PrintStream {
    private final OutputStream second;

    public TeeOutputStream(OutputStream first, OutputStream second) {
        super(first);
        this.second = second;
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        try {
            super.write(buf, off, len);
            second.write(buf, off, len);
        } catch (IOException e) {
            // If we can't write to the second stream, just continue with the first one
            super.write(buf, off, len);
        }
    }

    @Override
    public void write(int b) {
        super.write(b);
        try {
            second.write(b);
        } catch (IOException e) {
            // If we can't write to the second stream, just continue with the first one
            super.write(b);
        }
    }

    @Override
    public void flush() {
        super.flush();
        try {
            second.flush();
        } catch (IOException e) {
            // Ignore
        }
    }

    @Override
    public void close() {
        try {
            super.close();
        } finally {
            try {
                second.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}
