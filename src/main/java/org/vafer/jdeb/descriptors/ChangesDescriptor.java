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

import org.vafer.jdeb.changes.ChangeSet;

/**
 * Reflecting a changes file
 * 
 * @author tcurdt
 */
public final class ChangesDescriptor extends AbstractDescriptor {

	private final static String[] keys = {
		"Format",
		"Date",
		"Source",
		"Binary",
		"Architecture",
		"Version",
		"Distribution",
		"Urgency",
		"Maintainer",
		"Changed-By",
		"Description",
		"Changes",
		"Closes",
		"Files"
	};
	
	private final static String[] mandatoryKeys = {
		"Format",
		"Date",
		"Source",
		"Binary",
		"Architecture",
		"Version",
		"Distribution",
		"Urgency",
		"Maintainer",
		"Description",
		"Changes",
		"Files"
	};

	private final ChangeSet[] changeSets;
	
	public ChangesDescriptor( final AbstractDescriptor pDescriptor, final ChangeSet[] pChangeSets ) {
		super(pDescriptor);
		changeSets = pChangeSets;

		final ChangeSet lastestChangeSet = changeSets[0];
		
		set("Urgency", lastestChangeSet.getUrgency());
		set("Changed-By", lastestChangeSet.getChangedBy());

		final StringBuffer sb = new StringBuffer("\n");

		for (int i = 0; i < 1; i++) {
			final ChangeSet changeSet = changeSets[i];
			sb.append(changeSet.toString());			
		}

		set("Changes", sb.toString());
	}
	
	public boolean isValid() {
		for (int i = 0; i < mandatoryKeys.length; i++) {
			if (get(mandatoryKeys[i]) == null) {
				return false;
			}
		}
		
		return true;
	}
	
	public String toString() {
		return toString(keys);
	}
}
