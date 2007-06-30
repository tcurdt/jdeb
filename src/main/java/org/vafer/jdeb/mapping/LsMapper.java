package org.vafer.jdeb.mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.tar.TarEntry;

/**
 * ls -laR > mapping.txt 
 * 
 * @author tcurdt
 */
public final class LsMapper implements Mapper {

	private final Map mapping;

	
	public final static class ParseError extends Exception {

		private static final long serialVersionUID = 1L;

		public ParseError() {
			super();
		}

		public ParseError(String message, Throwable cause) {
			super(message, cause);
		}

		public ParseError(String message) {
			super(message);
		}

		public ParseError(Throwable cause) {
			super(cause);
		}
		
	};
	
	public LsMapper( final InputStream pInput ) throws IOException, ParseError {
		mapping = parse(pInput);
	}
	
	/*
./trunk/target/test-classes/org/vafer/dependency:
total 176
drwxr-xr-x   23 tcurdt  tcurdt   782 Jun 25 03:48 .
drwxr-xr-x    3 tcurdt  tcurdt   102 Jun 25 03:48 ..
-rw-r--r--    1 tcurdt  tcurdt  2934 Jun 25 03:48 DependenciesTestCase.class
-rw-r--r--    1 tcurdt  tcurdt   786 Jun 25 03:48 JarCombiningTestCase$1.class
-rw-r--r--    1 tcurdt  tcurdt  2176 Jun 25 03:48 WarTestCase.class
drwxr-xr-x    4 tcurdt  tcurdt   136 Jun 25 03:48 classes

./trunk/target/test-classes/org/vafer/dependency/classes:
	 */
	
	final private Pattern basePattern = Pattern.compile("^\\./(.*):$");
	final private Pattern totalPattern = Pattern.compile("^total ([0-9]+)$");
	final private Pattern dirPattern = Pattern.compile("^d([rwx-]{9})\\s+([0-9]+)\\s+(.*)\\s+(.*)\\s+([0-9]+)\\s+(.*)\\s+[\\.]{1,2}$");
	final private Pattern filePattern = Pattern.compile("^([d-])([rwx-]{9})\\s+([0-9]+)\\s+(.*)\\s+(.*)\\s+([0-9]+)\\s+(.*)\\s+(.*)$");
	final private Pattern newlinePattern = Pattern.compile("$");

	private String readBase( final BufferedReader reader ) throws IOException, ParseError {
		final String line = reader.readLine();
		if (line == null) {
			return null;
		}
		final Matcher matcher = basePattern.matcher(line);
		if (!matcher.matches()) {
			throw new ParseError("expected base line but got \"" + line + "\"");
		}
		return matcher.group(1);
	}

	private String readTotal( final BufferedReader reader ) throws IOException, ParseError {
		final String line = reader.readLine();
		final Matcher matcher = totalPattern.matcher(line);
		if (!matcher.matches()) {
			throw new ParseError("expected total line but got \"" + line + "\"");
		}
		return matcher.group(1);
	}

	private TarEntry readDir( final BufferedReader reader, final String base ) throws IOException, ParseError {
		final String current = reader.readLine();
		final Matcher currentMatcher = dirPattern.matcher(current);
		if (!currentMatcher.matches()) {
			throw new ParseError("expected dirline but got \"" + current + "\"");
		}

		final String parent = reader.readLine();
		final Matcher parentMatcher = dirPattern.matcher(parent);
		if (!parentMatcher.matches()) {
			throw new ParseError("expected dirline but got \"" + parent + "\"");
		}
		
		final TarEntry entry = new TarEntry(base);
		//entry.setGroupName(currentMatcher.group(2));
		
		return entry;
	}

	private TarEntry readFile( final BufferedReader reader, final String base ) throws IOException, ParseError {
		
		while(true) {
			final String line = reader.readLine();
			
			if (line == null) {
				return null;
			}
			
			final Matcher matcher = filePattern.matcher(line);
			if (!matcher.matches()) {
				final Matcher newlineMatcher = newlinePattern.matcher(line);
				if (newlineMatcher.matches()) {
					return null;
				}
				throw new ParseError("expected file line but got \"" + line + "\"");
			}
			
			final String type = matcher.group(1);
			if (type.startsWith("-")) {
				final TarEntry entry = new TarEntry(base + "/" + matcher.group(8));

				//entry.setGroupName(currentMatcher.group(2));
				
				return entry;				
			}			
		}
		
	}
	
	private Map parse( final InputStream pInput ) throws IOException, ParseError {
		final Map mapping = new HashMap();
		
		final BufferedReader reader = new BufferedReader(new InputStreamReader(pInput));
		
		while(true) {

			final String base = readBase(reader);

			if (base == null) {
				break;
			}

			readTotal(reader);
			final TarEntry dir = readDir(reader, base);
			mapping.put(dir.getName(), dir);

			while(true) {
				final TarEntry file = readFile(reader, base);

				if (file == null) {
					break;
				}
				
				mapping.put(file.getName(), file);
			}
		}
		
		return mapping;
	}
	
	public TarEntry map( final TarEntry pEntry ) {
		
		final TarEntry entry = (TarEntry) mapping.get(pEntry.getName());
		
		if (entry != null) {
			return entry;
		}
		
		return pEntry;
	}

}
