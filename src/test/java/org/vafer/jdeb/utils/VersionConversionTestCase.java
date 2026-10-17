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

package org.vafer.jdeb.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.regex.Pattern;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.Assert;
import org.junit.Test;

/**
 * Property based tests for {@link Utils#convertToDebianVersion}.
 *
 * Debian does not define how to map a Maven version, it only defines which
 * versions are valid and how they are ordered. So instead of hand written
 * expectations these tests check that the conversion
 * <ul>
 * <li>always yields a valid Debian upstream_version</li>
 * <li>preserves the Maven ordering under the dpkg ordering</li>
 * <li>leaves versions alone that are already written the Debian way</li>
 * </ul>
 *
 * https://www.debian.org/doc/debian-policy/ch-controlfields.html#version
 * https://github.com/tcurdt/jdeb/issues/314
 */
public final class VersionConversionTestCase extends Assert {

  // upstream_version without epoch and without debian_revision,
  // so neither ':' nor '-' are allowed
  private static final Pattern DEBIAN_UPSTREAM_VERSION = Pattern.compile(
    "^[0-9][A-Za-z0-9.+~]*$"
  );

  private static final String[] MAVEN_VERSIONS = {
    "1.0-alpha-1",
    "1.0-alpha-2",
    "1.0-beta-1",
    "1.0-M1",
    "1.0-RC1",
    "1.0-rc2",
    "1.0-SNAPSHOT",
    "1.0",
    // maven orders numbers after strings, dpkg digits before letters, so
    // a numeric suffix like this one and a qualifier like "sp1" can not be
    // ordered the same way. Projects rarely mix them, so this is accepted.
    "1.0-3-g1b4b40e",
    "1.0.1",
    "1.1-alpha1",
    "1.1",
  };

  // versions that already use the Debian '~' convention
  private static final String[] DEBIAN_STYLE_VERSIONS = {
    "3.7.1~dev24+g1b4b40e",
    "1.2.3~RC.0",
    "1.0~RC.2",
    "1.0~alpha3",
    "1.0~Beta-4",
    "1.0~milestone-4",
    "1.0~rc7",
    "1.0~SNAPSHOT",
  };

  private static final String[] ODD_VERSIONS = {
    "1.0-prj_3:M3",
    "1.0-prj_._-M3",
    "1.0-prj_3-c+++++++-SNAPSHOT",
    "1.0+g1a2b3c",
  };

  private static final class SnapshotMode {

    final boolean expand;
    final String template;

    SnapshotMode(boolean expand, String template) {
      this.expand = expand;
      this.template = template;
    }

    @Override
    public String toString() {
      return expand ? "expand=" + template : "no-expand";
    }
  }

  private static final SnapshotMode NO_EXPAND = new SnapshotMode(false, null);

  // the default timestamp starts with a digit and therefore sorts before
  // all alpha, beta and rc versions. Kept for compatibility, users that
  // care about the order can use a template starting with a letter.
  private static final SnapshotMode EXPAND_TIMESTAMP = new SnapshotMode(
    true,
    null
  );

  private static final SnapshotMode EXPAND_TEMPLATE = new SnapshotMode(
    true,
    "snapshot[yyyyMMddHHmmss]"
  );

  private static final SnapshotMode[] ALL_MODES = {
    NO_EXPAND,
    EXPAND_TIMESTAMP,
    EXPAND_TEMPLATE,
  };

  private static final SnapshotMode[] ORDERED_MODES = {
    NO_EXPAND,
    EXPAND_TEMPLATE,
  };

  private static Date timestamp() {
    Calendar cal = new GregorianCalendar(2013, Calendar.FEBRUARY, 17);
    cal.setTimeZone(TimeZone.getTimeZone("UTC"));
    return cal.getTime();
  }

  private static String convert(String version, SnapshotMode mode) {
    return Utils.convertToDebianVersion(
      version,
      mode.expand,
      "SNAPSHOT",
      mode.template,
      timestamp()
    );
  }

  /**
   * Port of verrevcmp() from dpkg lib/dpkg/version.c
   */
  static int compareDebianVersions(String a, String b) {
    int i = 0;
    int j = 0;
    while (i < a.length() || j < b.length()) {
      int firstDiff = 0;
      while (
        (i < a.length() && !isDigit(a.charAt(i))) ||
        (j < b.length() && !isDigit(b.charAt(j)))
      ) {
        int ac = order(charAt(a, i));
        int bc = order(charAt(b, j));
        if (ac != bc) {
          return ac - bc;
        }
        i++;
        j++;
      }
      while (charAt(a, i) == '0') {
        i++;
      }
      while (charAt(b, j) == '0') {
        j++;
      }
      while (isDigit(charAt(a, i)) && isDigit(charAt(b, j))) {
        if (firstDiff == 0) {
          firstDiff = charAt(a, i) - charAt(b, j);
        }
        i++;
        j++;
      }
      if (isDigit(charAt(a, i))) {
        return 1;
      }
      if (isDigit(charAt(b, j))) {
        return -1;
      }
      if (firstDiff != 0) {
        return firstDiff;
      }
    }
    return 0;
  }

  private static char charAt(String s, int i) {
    return i < s.length() ? s.charAt(i) : 0;
  }

  private static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private static int order(char c) {
    if (isDigit(c)) {
      return 0;
    } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
      return c;
    } else if (c == '~') {
      return -1;
    } else if (c != 0) {
      return c + 256;
    } else {
      return 0;
    }
  }

  @Test
  public void testDebianVersionComparison() {
    // examples from the Debian policy
    assertTrue(compareDebianVersions("~~", "~~a") < 0);
    assertTrue(compareDebianVersions("~~a", "~") < 0);
    assertTrue(compareDebianVersions("~", "") < 0);
    assertTrue(compareDebianVersions("", "a") < 0);

    assertTrue(compareDebianVersions("1.0~rc1", "1.0") < 0);
    assertTrue(compareDebianVersions("1.0~rc1", "1.0~rc2") < 0);
    assertTrue(compareDebianVersions("1.0", "1.0.1") < 0);
    assertTrue(compareDebianVersions("1.0", "1.0+1") < 0);
    assertTrue(compareDebianVersions("1.0+1", "1.0.1") < 0);
    assertTrue(compareDebianVersions("1.0", "1.0a") < 0);
    assertTrue(compareDebianVersions("1.0A", "1.0a") < 0);
    assertTrue(compareDebianVersions("2", "10") < 0);
    assertEquals(0, compareDebianVersions("1.01", "1.1"));
    assertEquals(0, compareDebianVersions("1.0", "1.0"));
  }

  @Test
  public void testConvertedVersionsAreValid() {
    StringBuilder failures = new StringBuilder();
    for (SnapshotMode mode : ALL_MODES) {
      for (String[] versions : new String[][] {
        MAVEN_VERSIONS,
        DEBIAN_STYLE_VERSIONS,
        ODD_VERSIONS,
      }) {
        for (String version : versions) {
          String converted = convert(version, mode);
          if (!DEBIAN_UPSTREAM_VERSION.matcher(converted).matches()) {
            failures.append(
              String.format("%n  %-34s %-22s -> %s", mode, version, converted)
            );
          }
        }
      }
    }
    assertTrue("invalid Debian versions:" + failures, failures.length() == 0);
  }

  @Test
  public void testConversionPreservesMavenOrdering() {
    StringBuilder failures = new StringBuilder();
    for (SnapshotMode mode : ORDERED_MODES) {
      for (int i = 0; i < MAVEN_VERSIONS.length; i++) {
        for (int j = i + 1; j < MAVEN_VERSIONS.length; j++) {
          String a = MAVEN_VERSIONS[i];
          String b = MAVEN_VERSIONS[j];
          int maven = Integer.signum(
            new ComparableVersion(a).compareTo(new ComparableVersion(b))
          );
          String da = convert(a, mode);
          String db = convert(b, mode);
          int debian = Integer.signum(compareDebianVersions(da, db));
          if (maven != debian) {
            failures.append(
              String.format(
                "%n  %-34s maven: %-14s %s %-14s  debian: %-22s %s %s",
                mode,
                a,
                symbol(maven),
                b,
                da,
                symbol(debian),
                db
              )
            );
          }
        }
      }
    }
    assertTrue("ordering not preserved:" + failures, failures.length() == 0);
  }

  @Test
  public void testDebianStyleVersionsAreKept() {
    StringBuilder failures = new StringBuilder();
    for (SnapshotMode mode : ALL_MODES) {
      for (String version : DEBIAN_STYLE_VERSIONS) {
        // only characters Debian does not allow get replaced
        String expected = version
          .replaceAll("[^.+~A-Za-z0-9]", "+")
          .replaceAll("\\++", "+");
        String converted = convert(version, mode);
        if (!expected.equals(converted)) {
          failures.append(
            String.format(
              "%n  %-34s %-22s expected %-22s got %s",
              mode,
              version,
              expected,
              converted
            )
          );
        }
      }
    }
    assertTrue(
      "Debian style versions changed:" + failures,
      failures.length() == 0
    );
  }

  private static String symbol(int signum) {
    return signum < 0 ? "<" : signum > 0 ? ">" : "=";
  }
}
