/*
 * Copyright 2007-2026 The jdeb developers.
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
package org.vafer.jdeb;

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

public class TarOptions {

  private Compression compression = Compression.GZIP;
  private int longFileMode = TarArchiveOutputStream.LONGFILE_GNU;
  private int bigNumberMode = TarArchiveOutputStream.BIGNUMBER_STAR;

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
    if ("error".equals(input)) {
      bigNumberMode = TarArchiveOutputStream.BIGNUMBER_ERROR;
    } else if ("posix".equals(input)) {
      bigNumberMode = TarArchiveOutputStream.BIGNUMBER_POSIX;
    } else {
      bigNumberMode = TarArchiveOutputStream.BIGNUMBER_STAR;
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
