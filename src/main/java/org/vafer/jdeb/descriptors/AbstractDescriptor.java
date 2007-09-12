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
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A descriptor holds the usual key value pairs
 * 
 * @author tcurdt
 */
public abstract class AbstractDescriptor {
	
	private final Map values = new HashMap();
	
	public AbstractDescriptor() {
	}

	public AbstractDescriptor( final AbstractDescriptor pDescriptor ) {
		values.putAll(pDescriptor.values);
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
			
			// continueing old value
			buffer.append('\n').append(line.substring(1));
		}
		br.close();
		
	}
	
	public void set( final String pKey, final String pValue ) {
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
				s.append(key).append(": ").append(value).append('\n');
			}			
		}
		return s.toString();
	}
}
