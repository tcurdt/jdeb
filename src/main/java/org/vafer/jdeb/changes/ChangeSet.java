package org.vafer.jdeb.changes;


public final class ChangeSet {

	private final String packageName;
	private final String version;
	private final String date;
	private final String distribution;
	private final String urgency;
	private final String changedBy;
	private final String[] changes;
	
	public ChangeSet( String pPackageName, String pVersion, String pDate, String pDistribution, String pUrgency, String pChangedBy, final String[] pChanges ) {
		changes = pChanges;
		packageName = pPackageName;
		version = pVersion;
		date = pDate;
		distribution = pDistribution;
		urgency = pUrgency;
		changedBy = pChangedBy;
	}
	/*
     package (version) distribution(s); urgency=urgency
     	    [optional blank line(s), stripped]
       * change details
         more change details
     	    [blank line(s), included in output of dpkg-parsechangelog]
       * even more change details
     	    [optional blank line(s), stripped]
      -- maintainer name <email address>[two spaces]  date
    */
	
	public String getPackage() {
		return packageName;
	}
	
	public String getVersion() {
		return version;
	}
	
	public String getDate() {
		return date;
	}
	
	public String getDistribution() {
		return distribution;
	}
	
	public String getUrgency() {
		return urgency;
	}
	
	public String getChangedBy() {
		return changedBy;
	}
	
	public String[] getChanges() {
		return changes;
	}
	
	public String toString() {
		final StringBuffer sb = new StringBuffer();
		sb.append(" ").append(getPackage()).append(" (").append(getVersion()).append(") ");
		sb.append(getDistribution()).append("; urgency=").append(getUrgency()).append("\n");
		for (int i = 0; i < changes.length; i++) {
			sb.append(" * ").append(changes[i]).append("\n");
		}
		sb.append("-- ").append(getChangedBy()).append("  ").append(getDate()).append("\n");
		return sb.toString();
	}
}
