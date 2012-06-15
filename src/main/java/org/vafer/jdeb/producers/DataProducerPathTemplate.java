package org.vafer.jdeb.producers;

import java.io.IOException;

import org.apache.tools.tar.TarEntry;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.mapping.Mapper;

public class DataProducerPathTemplate extends AbstractDataProducer implements DataProducer {

    private String[] literalPaths;

    public DataProducerPathTemplate( String[] pLiteralPaths, String[] pIncludes, String[] pExcludes, Mapper[] pMapper ) {
        super(pIncludes, pExcludes, pMapper);
        literalPaths = pLiteralPaths;
    }

    public void produce( DataConsumer pReceiver ) throws IOException {
        for (String literalPath : literalPaths) {
            TarEntry entry = new TarEntry(literalPath);
            entry.setUserId(0);
            entry.setUserName("root");
            entry.setGroupId(0);
            entry.setGroupName("root");
            entry.setMode(TarEntry.DEFAULT_DIR_MODE);

            entry = map(entry);

            entry.setSize(0);

            pReceiver.onEachDir(entry.getName(), entry.getLinkName(), entry.getUserName(), entry.getUserId(), entry.getGroupName(), entry.getGroupId(), entry.getMode(), entry.getSize());
        }
    }

}
