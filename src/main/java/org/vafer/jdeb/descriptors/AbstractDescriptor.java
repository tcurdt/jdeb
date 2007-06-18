package org.vafer.jdeb.descriptors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractDescriptor {

	
	private final Map values = new HashMap();

	protected void parse( InputStream is ) throws IOException, ParseException {
		final BufferedReader br = new BufferedReader(new InputStreamReader(is));
		StringBuffer buffer = new StringBuffer();
		String key = null;
		int linenr = 0;
		while(true) {
			final String line = br.readLine();
			
			if (line == null) {
				if (buffer.length() > 0) {
					// flush value of previous key
					add(key, buffer.toString());
					buffer = null;
				}
				break;
			}

			linenr++;
			
			final char first = line.charAt(0); 
			if (Character.isLetter(first)) {
				
				// new key
				
				if (buffer.length() > 0) {
					// flush value of previous key
					add(key, buffer.toString());
					buffer = new StringBuffer();
				}
				
				
				final int i = line.indexOf(':');
				
				if (i < 0) {
					throw new ParseException("Line misses ':' delimitter", linenr);
				}
				
				key = line.substring(0, i);
				buffer.append(line.substring(i+1).trim());
				
				continue;
			}
			
			// continueing old value
			buffer.append('\n').append(line.substring(1));
		}
		br.close();
		
	}
	
	private void add( String key, String value ) {
		System.out.println(key + " = " + value);
		values.put(key, value);
	}
	
	public boolean isValid() {
		return true;
	}

	public String toString( String[] keys ) {
		final StringBuffer s = new StringBuffer();
		for (int i = 0; i < keys.length; i++) {
			final String key = keys[i];
			final String value = (String) values.get(key);
			if (value != null) {
				s.append(key).append(": ").append(value);
			}			
		}
		return s.toString();
	}
}
