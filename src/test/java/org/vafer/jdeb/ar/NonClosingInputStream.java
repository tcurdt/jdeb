/*
 * Copyright 2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vafer.jdeb.ar;

import java.io.IOException;
import java.io.InputStream;

/**
 * ATTENTION: don't use outside of jdeb
 * 
 * @author Torsten Curdt <tcurdt@vafer.org>
 */
public final class NonClosingInputStream extends InputStream {

    private final InputStream delegate;
    
    public NonClosingInputStream( final InputStream pDelegate ) {
        delegate = pDelegate;
    }

    public int available() throws IOException {
        return delegate.available();
    }

    public void close() throws IOException {
        // we DON'T close
        // delegate.close();
    }

    public void mark(int readlimit) {
        delegate.mark(readlimit);
    }

    public boolean markSupported() {
        return delegate.markSupported();
    }

    public int read() throws IOException {
        return delegate.read();
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return delegate.read(b, off, len);
    }

    public int read(byte[] b) throws IOException {
        return delegate.read(b);
    }

    public void reset() throws IOException {
        delegate.reset();
    }

    public long skip(long n) throws IOException {
        return delegate.skip(n);
    }
    
}
