package org.vafer.jdeb.ar;

/**
 * @author Emmanuel Bourg
 * @version $Revision$, $Date$
 */
interface ArConstants {

	static final byte[] HEADER = "!<arch>\n".getBytes();

	static final byte[] ENTRY_TERMINATOR = "`\012".getBytes();

	static final int FIELD_SIZE_NAME = 16;
	static final int FIELD_SIZE_LASTMODIFIED = 12;
	static final int FIELD_SIZE_UID = 6;
	static final int FIELD_SIZE_GID = 6;
	static final int FIELD_SIZE_MODE = 8;
	static final int FIELD_SIZE_LENGTH = 10;

	static final int HEADER_SIZE =
			FIELD_SIZE_NAME
			+ FIELD_SIZE_LASTMODIFIED
			+ FIELD_SIZE_UID
			+ FIELD_SIZE_GID
			+ FIELD_SIZE_MODE
			+ FIELD_SIZE_LENGTH
			+ ENTRY_TERMINATOR.length;

}
