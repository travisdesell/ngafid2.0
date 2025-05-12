package org.ngafid.processor;


import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.SeekableInputStream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 */
public class NioInputFile implements InputFile {
    private final Path path;
    private long length = -1;

    public NioInputFile(Path file) {
        path = file;
    }
    @Override
    public long getLength() throws IOException {
        if (length == -1) {
            length = Files.size(path);
        }
        return length;
    }

    @Override
    public SeekableInputStream newStream() throws IOException {

        return new SeekableInputStream() {

            private final SeekableByteChannel byteChannel = Files.newByteChannel(path);
            private final ByteBuffer singleByteBuffer = ByteBuffer.allocate(1);

            @Override
            public int read() throws IOException {
                // There has to be a better way to do this?
                singleByteBuffer.clear();
                final int numRead = read(singleByteBuffer);
                if (numRead >= 0) {
                    int value = (int)singleByteBuffer.get(0) & 0xFF;
                    return value;
                } else {
                    return -1;
                }
            }

            @Override
            public long getPos() throws IOException {
                return byteChannel.position();
            }

            @Override
            public void seek(long newPos) throws IOException {
                byteChannel.position(newPos);
            }

            @Override
            public void readFully(byte[] bytes) throws IOException {
                readFully(bytes, 0, bytes.length);
            }

            @Override
            public void readFully(byte[] bytes, int start, int len) throws IOException {
                final ByteBuffer buf = ByteBuffer.wrap(bytes);
                buf.position(start);
                readFully(buf);
            }

            @Override
            public int read(ByteBuffer buf) throws IOException {
                return byteChannel.read(buf);
            }

            @Override
            public void readFully(ByteBuffer buf) throws IOException {
                int numRead = 0;
                while (numRead < buf.limit()) {
                    final int code = read(buf);
                    if (code == -1) {
                        return;
                    } else {
                        numRead += code;
                    }
                }
            }

            @Override
            public void close() throws IOException {
                byteChannel.close();
            }
        };
    }
}

