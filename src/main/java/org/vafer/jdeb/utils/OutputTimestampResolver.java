package org.vafer.jdeb.utils;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.maven.archiver.MavenArchiver;
import org.vafer.jdeb.Console;

public class OutputTimestampResolver {
    private final Console console;
    private final EnvironmentVariablesReader envReader;

    public OutputTimestampResolver(Console console) {
        this(console, new EnvironmentVariablesReader());
    }

    OutputTimestampResolver(Console console, EnvironmentVariablesReader envReader) {
        this.console = console;
        this.envReader = envReader;
    }

    public Long resolveOutputTimestamp(String paramValue) {
        if (paramValue != null) {
            Optional<Instant> outputDate = MavenArchiver.parseBuildOutputTimestamp(paramValue);
            if (outputDate.isPresent()) {
                console.info("Accepted outputTimestamp parameter: " + paramValue);
                return outputDate.get().toEpochMilli();
            }
        }

        String sourceDate = envReader.getSourceDateEpoch();
        if (sourceDate != null && !sourceDate.isEmpty()) {
            try {
                long sourceDateVal = Long.parseLong(sourceDate);
                console.info("Accepted SOURCE_DATE_EPOCH environment variable: " + sourceDate);
                return sourceDateVal * TimeUnit.SECONDS.toMillis(1);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid SOURCE_DATE_EPOCH environment variable value: " + sourceDate, e);
            }
        }

        return null;
    }

    static class EnvironmentVariablesReader {
        String getSourceDateEpoch() {
            return System.getenv("SOURCE_DATE_EPOCH");
        }
    }
}
