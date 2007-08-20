package org.vafer.jdeb.changes;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.vafer.jdeb.descriptors.PackageDescriptor;

public final class TextfileChangesProvider implements ChangesProvider {

	private final ChangeSet[] changeSets;
	
	public TextfileChangesProvider( final InputStream pInput, final PackageDescriptor pDescriptor ) throws IOException, ParseException {		
				
		final BufferedReader reader = new BufferedReader(new InputStreamReader(pInput));

		final SimpleDateFormat tdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");

		String packageName = pDescriptor.get("Package");
		String version = pDescriptor.get("Version");
		Date date = tdf.parse(pDescriptor.get("Date"));
		String distribution = pDescriptor.get("Distribution");
		String urgency = pDescriptor.get("Urgency");
		String changedBy = pDescriptor.get("Maintainer");
		final Collection changesColl = new ArrayList();
		final Collection changeSetColl = new ArrayList();

		final DateFormat sdf = ChangeSet.createDateForma();
		
		while(true) {
			final String line = reader.readLine();
			if (line == null) {
				final String[] changes = (String[]) changesColl.toArray(new String[changesColl.size()]);
				final ChangeSet changeSet = new ChangeSet(packageName, version, date, distribution, urgency, changedBy, changes);
				changeSetColl.add(changeSet);
				break;
			}
			
			if (line.startsWith("release ")) {

				if (changesColl.size() > 0) {
					final String[] changes = (String[]) changesColl.toArray(new String[changesColl.size()]);
					final ChangeSet changeSet = new ChangeSet(packageName, version, date, distribution, urgency, changedBy, changes);
					changeSetColl.add(changeSet);
					changesColl.clear();
				}
				
				final String[] tokens = line.substring("release ".length()).split(",");
				for (int i = 0; i < tokens.length; i++) {
					final String token = tokens[i].trim();
					final String[] lr = token.split("=");
					final String key = lr[0];
					final String value = lr[1];
					
					if ("urgency".equals(key)) {
						urgency = value;
					} else if ("by".equals(key)) {
						changedBy = value;
					} else if ("date".equals(key)) {
						date = sdf.parse(value);
					} else if ("version".equals(key)) {
						version = value;
					}
				}				
			}
			
			if (line.startsWith(" * ")) {
				changesColl.add(line.substring(" * ".length()));
			}
			
		}
		
		reader.close();
		
		changeSets = (ChangeSet[]) changeSetColl.toArray(new ChangeSet[changeSetColl.size()]);		
	}
	
	public void save( final OutputStream pOutput ) throws IOException {
		final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(pOutput));
		
		final DateFormat df = ChangeSet.createDateForma();
		
		for (int i = 0; i < changeSets.length; i++) {
			final ChangeSet changeSet = changeSets[i];
			
			writer.write("release ");
			writer.write("date=" + df.format(changeSet.getDate()) + ",");
			writer.write("version=" + changeSet.getVersion() + ",");
			writer.write("urgency=" + changeSet.getUrgency() + ",");
			writer.write("by=" + changeSet.getChangedBy());
			writer.write("\n");

			final String[] changes = changeSet.getChanges();
			for (int j = 0; j < changes.length; j++) {
				writer.write(" * ");
				writer.write(changes[j]);
				writer.write("\n");
			}
		}
		
		writer.close();
	}
	
	public ChangeSet[] getChangesSets() {
		return changeSets;
	}

}
