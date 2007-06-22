package org.vafer.jdeb;

import java.io.IOException;
import java.io.InputStream;

public interface DataConsumer {

	void onEachFile( InputStream input, String filename, String linkname, String user, int uid, String group, int gid, int mode, long size) throws IOException;

}
