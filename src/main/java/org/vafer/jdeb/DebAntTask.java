package org.vafer.jdeb;

import java.io.File;
import java.util.Enumeration;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.PatternSet.NameEntry;
import org.apache.tools.ant.types.selectors.AndSelector;
import org.apache.tools.ant.types.selectors.ContainsRegexpSelector;
import org.apache.tools.ant.types.selectors.ContainsSelector;
import org.apache.tools.ant.types.selectors.DateSelector;
import org.apache.tools.ant.types.selectors.DependSelector;
import org.apache.tools.ant.types.selectors.DepthSelector;
import org.apache.tools.ant.types.selectors.DifferentSelector;
import org.apache.tools.ant.types.selectors.ExtendSelector;
import org.apache.tools.ant.types.selectors.FileSelector;
import org.apache.tools.ant.types.selectors.FilenameSelector;
import org.apache.tools.ant.types.selectors.MajoritySelector;
import org.apache.tools.ant.types.selectors.NoneSelector;
import org.apache.tools.ant.types.selectors.NotSelector;
import org.apache.tools.ant.types.selectors.OrSelector;
import org.apache.tools.ant.types.selectors.PresentSelector;
import org.apache.tools.ant.types.selectors.SelectSelector;
import org.apache.tools.ant.types.selectors.SizeSelector;
import org.apache.tools.ant.types.selectors.TypeSelector;
import org.apache.tools.ant.types.selectors.modifiedselector.ModifiedSelector;
import org.vafer.jdeb.ant.Data;

/**
 * @deprecated
 * @author tcurdt
 */
public final class DebAntTask extends MatchingTask {

	private org.vafer.jdeb.ant.DebAntTask task = new org.vafer.jdeb.ant.DebAntTask();

	public DebAntTask() {
		System.err.print("ATTENTION: you are using the wrong task. Please use " + org.vafer.jdeb.ant.DebAntTask.class.getName());
	}

	public void add(FileSelector arg0) {
		task.add(arg0);
	}

	public void addAnd(AndSelector arg0) {
		task.addAnd(arg0);
	}

	public void addContains(ContainsSelector arg0) {
		task.addContains(arg0);
	}

	public void addContainsRegexp(ContainsRegexpSelector arg0) {
		task.addContainsRegexp(arg0);
	}

	public void addCustom(ExtendSelector arg0) {
		task.addCustom(arg0);
	}

	public void addData(Data data) {
		task.addData(data);
	}

	public void addDate(DateSelector arg0) {
		task.addDate(arg0);
	}

	public void addDepend(DependSelector arg0) {
		task.addDepend(arg0);
	}

	public void addDepth(DepthSelector arg0) {
		task.addDepth(arg0);
	}

	public void addDifferent(DifferentSelector arg0) {
		task.addDifferent(arg0);
	}

	public void addFilename(FilenameSelector arg0) {
		task.addFilename(arg0);
	}

	public void addMajority(MajoritySelector arg0) {
		task.addMajority(arg0);
	}

	public void addModified(ModifiedSelector arg0) {
		task.addModified(arg0);
	}

	public void addNone(NoneSelector arg0) {
		task.addNone(arg0);
	}

	public void addNot(NotSelector arg0) {
		task.addNot(arg0);
	}

	public void addOr(OrSelector arg0) {
		task.addOr(arg0);
	}

	public void addPresent(PresentSelector arg0) {
		task.addPresent(arg0);
	}

	public void addSelector(SelectSelector arg0) {
		task.addSelector(arg0);
	}

	public void addSize(SizeSelector arg0) {
		task.addSize(arg0);
	}

	public void addType(TypeSelector arg0) {
		task.addType(arg0);
	}

	public void appendSelector(FileSelector arg0) {
		task.appendSelector(arg0);
	}

	public NameEntry createExclude() {
		return task.createExclude();
	}

	public NameEntry createExcludesFile() {
		return task.createExcludesFile();
	}

	public NameEntry createInclude() {
		return task.createInclude();
	}

	public NameEntry createIncludesFile() {
		return task.createIncludesFile();
	}

	public PatternSet createPatternSet() {
		return task.createPatternSet();
	}

	public boolean equals(Object obj) {
		return task.equals(obj);
	}

	public void execute() {
		task.execute();
	}

	public String getDescription() {
		return task.getDescription();
	}

	public Location getLocation() {
		return task.getLocation();
	}

	public Target getOwningTarget() {
		return task.getOwningTarget();
	}

	public Project getProject() {
		return task.getProject();
	}

	public RuntimeConfigurable getRuntimeConfigurableWrapper() {
		return task.getRuntimeConfigurableWrapper();
	}

	public FileSelector[] getSelectors(Project arg0) {
		return task.getSelectors(arg0);
	}

	public String getTaskName() {
		return task.getTaskName();
	}

	public String getTaskType() {
		return task.getTaskType();
	}

	public int hashCode() {
		return task.hashCode();
	}

	public boolean hasSelectors() {
		return task.hasSelectors();
	}

	public void init() throws BuildException {
		task.init();
	}

	public void log(String arg0, int arg1) {
		task.log(arg0, arg1);
	}

	public void log(String arg0) {
		task.log(arg0);
	}

	public void maybeConfigure() throws BuildException {
		task.maybeConfigure();
	}

	public void reconfigure() {
		task.reconfigure();
	}

	public int selectorCount() {
		return task.selectorCount();
	}

	public Enumeration selectorElements() {
		return task.selectorElements();
	}

	public void setCaseSensitive(boolean arg0) {
		task.setCaseSensitive(arg0);
	}

	public void setChanges(File changes) {
		task.setChanges(changes);
	}

	public void setControl(File control) {
		task.setControl(control);
	}

	public void setDefaultexcludes(boolean arg0) {
		task.setDefaultexcludes(arg0);
	}

	public void setDescription(String arg0) {
		task.setDescription(arg0);
	}

	public void setDestfile(File deb) {
		task.setDestfile(deb);
	}

	public void setExcludes(String arg0) {
		task.setExcludes(arg0);
	}

	public void setExcludesfile(File arg0) {
		task.setExcludesfile(arg0);
	}

	public void setFollowSymlinks(boolean arg0) {
		task.setFollowSymlinks(arg0);
	}

	public void setIncludes(String arg0) {
		task.setIncludes(arg0);
	}

	public void setIncludesfile(File arg0) {
		task.setIncludesfile(arg0);
	}

	public void setKey(String key) {
		task.setKey(key);
	}

	public void setKeyring(File keyring) {
		task.setKeyring(keyring);
	}

	public void setLocation(Location arg0) {
		task.setLocation(arg0);
	}

	public void setOwningTarget(Target arg0) {
		task.setOwningTarget(arg0);
	}

	public void setPassphrase(String passphrase) {
		task.setPassphrase(passphrase);
	}

	public void setProject(Project arg0) {
		task.setProject(arg0);
	}

	public void setRuntimeConfigurableWrapper(RuntimeConfigurable arg0) {
		task.setRuntimeConfigurableWrapper(arg0);
	}

	public void setTaskName(String arg0) {
		task.setTaskName(arg0);
	}

	public void setTaskType(String arg0) {
		task.setTaskType(arg0);
	}

	public String toString() {
		return task.toString();
	}

	public void XsetIgnore(String arg0) {
		task.XsetIgnore(arg0);
	}

	public void XsetItems(String arg0) {
		task.XsetItems(arg0);
	} 

}
