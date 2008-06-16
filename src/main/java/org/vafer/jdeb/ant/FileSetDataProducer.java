/*
 * Copyright 2008 The Apache Software Foundation.
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
package org.vafer.jdeb.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.Tar;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.tar.TarEntry;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;

/**
 * DataProducer providing data from an Ant fileset. TarFileSets are also
 * supported with their permissions.
 *
 * @author Emmanuel Bourg
 * @version $Revision$, $Date$
 */
public class FileSetDataProducer implements DataProducer {

	private FileSet fileset;

	public FileSetDataProducer(FileSet fileset) {
		this.fileset = fileset;
	}

	public void produce(DataConsumer receiver) throws IOException {
		String user = "root";
		int uid = 0;
		String group = "root";
		int gid = 0;
		int filemode = TarEntry.DEFAULT_FILE_MODE;
		int dirmode = TarEntry.DEFAULT_DIR_MODE;
		String prefix = "";

		if (fileset instanceof Tar.TarFileSet) {
			Tar.TarFileSet tarfileset = (Tar.TarFileSet) fileset;
			user = tarfileset.getUserName();
			uid = tarfileset.getUid();
			group = tarfileset.getGroup();
			gid = tarfileset.getGid();
			filemode = tarfileset.getMode();
			dirmode = tarfileset.getDirMode();
			prefix = tarfileset.getPrefix();
		}

		DirectoryScanner scanner = fileset.getDirectoryScanner(fileset.getProject());
		scanner.scan();

		File basedir = scanner.getBasedir();

		String[] directories = scanner.getIncludedDirectories();
		for (int i = 0; i < directories.length; i++) {
			String name = directories[i];

			receiver.onEachDir(prefix + "/" + name, null, user, uid, group, gid, dirmode, 0);
		}

		String[] files = scanner.getIncludedFiles();
		for (int i = 0; i < files.length; i++) {
			String name = files[i];
			File file = new File(basedir, name);

			receiver.onEachFile(new FileInputStream(file), prefix + "/" + name, null, user, uid, group, gid,filemode, file.length());
		}
	}
}
