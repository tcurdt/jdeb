package org.vafer.jdeb.mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.tools.tar.TarEntry;

/**
 * ls -laR > mapping.txt 
 * 
 * @author tcurdt
 */
public final class LsMapper implements Mapper {

	private final Map mapping;
	
	public LsMapper( final InputStream pInput ) throws IOException {
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

	private String readBase( final BufferedReader reader ) throws IOException {
		final String line = reader.readLine();
		if (line == null) {
			return null;
		}
		return line.substring(2, line.length() - 1);
	}

	private int readTotal( final BufferedReader reader ) throws IOException {
		reader.readLine();
		return 0;
	}

	private TarEntry readDir( final BufferedReader reader, final String base ) throws IOException {
		reader.readLine();
		reader.readLine();
		return new TarEntry(base);
	}

	private TarEntry readFile( final BufferedReader reader, final String base ) throws IOException {
		final String line = reader.readLine();

		if (line.length() < 50) {
			return null;
		}

		return new TarEntry(base + "/" + line.substring(50));
	}
	
	private Map parse( final InputStream pInput ) throws IOException {
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
			System.out.println(dir.getName());

			while(true) {
				final TarEntry file = readFile(reader, base);

				if (file == null) {
					break;
				}
				
				mapping.put(file.getName(), file);

				System.out.println(file.getName());
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
