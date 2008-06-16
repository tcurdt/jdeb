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
package org.vafer.jdeb.descriptors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.vafer.jdeb.utils.Utils;
import org.vafer.jdeb.utils.VariableResolver;

/**
 * A descriptor holds the usual key value pairs.
 *
 * @see <a href="http://www.debian.org/doc/debian-policy/ch-controlfields.html">Debian Policy Manual - Control files and their fields</a>
 * 
 * @author tcurdt
 */
public abstract class AbstractDescriptor {
	
	private final Map values = new HashMap();
	private final VariableResolver resolver;
	
	public AbstractDescriptor( final VariableResolver pResolver ) {
		resolver = pResolver;
	}

	public AbstractDescriptor( final AbstractDescriptor pDescriptor ) {
		values.putAll(pDescriptor.values);
		resolver = pDescriptor.resolver;
	}

	protected void parse( final InputStream pInput ) throws IOException, ParseException {
		final BufferedReader br = new BufferedReader(new InputStreamReader(pInput));
		StringBuffer buffer = new StringBuffer();
		String key = null;
		int linenr = 0;
		while(true) {
			final String line = br.readLine();
			
			if (line == null) {
				if (buffer.length() > 0) {
					// flush value of previous key
					set(key, buffer.toString());
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
					set(key, buffer.toString());
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
			
			// continuing old value
			buffer.append('\n').append(line.substring(1));
		}
		br.close();
		
	}
	
	public void set( final String pKey, final String pValue ) {

		if (resolver != null) {
			try {
				values.put(pKey, Utils.replaceVariables(resolver, pValue, "[[", "]]"));
				return;
			} catch (ParseException e) {
				// FIXME maybe throw an Exception?
			}
		}
		
		values.put(pKey, pValue);
	}
	
	public String get( final String pKey ) {
		return (String)values.get(pKey);
	}

	public abstract String[] getMandatoryKeys();
	
	public boolean isValid() {
		return invalidKeys().size() == 0;
	}
	
	public Set invalidKeys() {
		final Set invalid = new HashSet();

		final String[] mk = getMandatoryKeys();
		for (int i = 0; i < mk.length; i++) {
			if (get(mk[i]) == null) {
				invalid.add(mk[i]);
			}
		}
		
		return invalid;
	}
	
	String toString( final String[] pKeys ) {
		final StringBuffer s = new StringBuffer();
		for (int i = 0; i < pKeys.length; i++) {
			final String key = pKeys[i];
			final String value = (String) values.get(key);
			if (value != null) {
				s.append(key).append(":");

				try {
					BufferedReader reader = new BufferedReader(new StringReader(value));
					String line;
					while ((line = reader.readLine()) != null) {
						if (line.length() != 0 && !Character.isWhitespace(line.charAt(0))) {
							s.append(' ');
						}

						s.append(line).append('\n');
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}			
		}
		return s.toString();
	}
}
