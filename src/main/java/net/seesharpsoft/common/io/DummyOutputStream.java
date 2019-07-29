package net.seesharpsoft.common.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An output stream implementation acting just as a dummy.
 */
public class DummyOutputStream extends OutputStream {
    public static final OutputStream DUMMY_OUTPUT_STREAM = new DummyOutputStream();

    public DummyOutputStream() {
    }

    public void write(byte[] bytes, int off, int len) {
    }

    public void write(int singleByte) {
    }

    public void write(byte[] bytes) throws IOException {
    }
}
