package org.vafer.jdeb.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.selectors.SelectorUtils;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;

public final class DataProducerArchive extends PatternSet implements DataProducer {

	private final File archive;
	
	public DataProducerArchive( final File pArchive ) {
		archive = pArchive;
	}
		
	public void produce( final DataConsumer receiver ) {
		
		String[] excludes = getExcludePatterns(getProject());
		excludes = (excludes!=null) ? excludes : new String[0];
		
		String[] includes = getIncludePatterns(getProject());
		includes = (includes!=null) ? includes : new String[] { "**" };
		

		TarInputStream archiveInputStream = null;
		try {
			archiveInputStream = new TarInputStream(new GZIPInputStream(new FileInputStream(archive)));

			while(true) {
				final TarEntry entry = archiveInputStream.getNextEntry();
				
				if (entry == null) {
					break;
				}

				final String name = entry.getName();
				
				if (!isIncluded(name, includes)) {
					continue;					
				}
				
				if (isExcluded(name, excludes)) {
					continue;
				}
				
				InputStream inputStream = archiveInputStream;
				
				if (entry.isDirectory()) {
					inputStream = null;							
				}
				
				receiver.onEachFile(inputStream, entry.getName(), entry.getLinkName(), entry.getUserName(), entry.getUserId(), entry.getGroupName(), entry.getGroupId(), entry.getMode(), entry.getSize());						
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (archiveInputStream != null) {
				try {
					archiveInputStream.close();
				} catch (IOException e) {
				}
			}
		}		
	}			

	
    private boolean isIncluded( String name, String[] includes ) {
        for (int i = 0; i < includes.length; i++) {
            if (SelectorUtils.matchPath(includes[i], name)) {
                return true;
            }
        }
        return false;
    }

    
    private boolean isExcluded( String name, String[] excludes ) {
        for (int i = 0; i < excludes.length; i++) {
            if (SelectorUtils.matchPath(excludes[i], name)) {            
                return true;
            }
        }
        return false;
    }
	
}
