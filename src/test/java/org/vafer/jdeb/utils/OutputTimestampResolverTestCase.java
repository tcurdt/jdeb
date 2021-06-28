package org.vafer.jdeb.utils;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.vafer.jdeb.NullConsole;

public class OutputTimestampResolverTestCase extends Assert {
    private OutputTimestampResolver resolver;
    private OutputTimestampResolver.EnvironmentVariablesReader envReader;

    @Before
    public void setUp() {
        envReader = mock(OutputTimestampResolver.EnvironmentVariablesReader.class);

        resolver = new OutputTimestampResolver(new NullConsole(), envReader);
    }

    @Test
    public void testParameterInIsoFormat() {
        Long result = resolver.resolveOutputTimestamp("2021-01-01T12:00:00Z");

        assertEquals((Long) 1609502400000L, result);
    }

    @Test
    public void testParameterInIsoFormatWithSpecificTimezone() {
        Long result = resolver.resolveOutputTimestamp("2021-01-01T12:00:00+13:00");

        assertEquals((Long) 1609455600000L, result);
    }

    @Test
    public void testParameterInEpochSeconds() {
        Long result = resolver.resolveOutputTimestamp("1600000000");

        assertEquals((Long) 1600000000000L, result);
    }

    @Test
    public void testInvalidParameter() {
        try {
            resolver.resolveOutputTimestamp("invalid");
        } catch (IllegalArgumentException e) {
            return;
        }
        fail("Exception expected on invalid value");
    }

    @Test
    public void testEnvVariable() {
        doReturn("1600000000").when(envReader).getSourceDateEpoch();

        Long result = resolver.resolveOutputTimestamp(null);

        assertEquals((Long) 1600000000000L, result);
    }

    @Test
    public void testInvalidEnvVariable() {
        doReturn("invalid").when(envReader).getSourceDateEpoch();

        try {
            resolver.resolveOutputTimestamp(null);
        } catch (IllegalArgumentException e) {
            return;
        }
        fail("Exception expected on invalid value");
    }

    @Test
    public void testNoTimestamp() {
        Long result = resolver.resolveOutputTimestamp(null);

        assertNull(result);
    }

    @Test
    public void testParamTakesPrecedenceOverEnvVar() {
        doReturn("1600000000").when(envReader).getSourceDateEpoch();

        Long result = resolver.resolveOutputTimestamp("1610000000");

        assertEquals((Long) 1610000000000L, result);
    }
}
