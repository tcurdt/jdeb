package org.vafer.jdeb;

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

public class TarOptions {

    private Compression compression = Compression.GZIP;
    private int longFileMode = TarArchiveOutputStream.LONGFILE_POSIX;
    private int bigNumberMode = TarArchiveOutputStream.BIGNUMBER_POSIX;

    public TarOptions compression(Compression compression) {
        this.compression = compression;

        return this;
    }

    public TarOptions longFileMode(String input) {
        if ("posix".equals(input)) {
            longFileMode = TarArchiveOutputStream.LONGFILE_POSIX;
        } else if ("error".equals(input)) {
            longFileMode = TarArchiveOutputStream.LONGFILE_ERROR;
        } else if ("truncate".equals(input)) {
            longFileMode = TarArchiveOutputStream.LONGFILE_TRUNCATE;
        } else {
            longFileMode = TarArchiveOutputStream.LONGFILE_GNU;
        }

        return this;
    }

    public TarOptions bigNumberMode(String input) {
        if ("posix".equals(input)) {
            bigNumberMode = TarArchiveOutputStream.BIGNUMBER_POSIX;
        } else if ("star".equals(input) || "gnu".equals(input)) {
            bigNumberMode = TarArchiveOutputStream.BIGNUMBER_STAR;
        } else {
            bigNumberMode = TarArchiveOutputStream.LONGFILE_ERROR;
        }

        return this;
    }

    public int longFileMode() {
        return longFileMode;
    }

    public int bigNumberMode() {
        return bigNumberMode;
    }

    public Compression compression() {
        return compression;
    }
}
