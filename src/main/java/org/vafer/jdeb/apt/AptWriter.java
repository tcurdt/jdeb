/*
 * Copyright 2014 The jdeb developers.
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
package org.vafer.jdeb.apt;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.CanReadFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.vafer.jdeb.Console;
import org.vafer.jdeb.debian.BinaryPackageControlFile;
import org.vafer.jdeb.debian.BinaryPackagePackagesFile;
import org.vafer.jdeb.debian.ComponentReleaseFile;
import org.vafer.jdeb.debian.DistributionReleaseFile;

/**
 * An APT repository writer
 * 
 * <p>
 * This class takes all files from the source directory and converts it to an APT repository in another directory. The target directory should be empty or not existing since it will overwrite everything with the name state from the source
 * directory.
 * </p>
 * <p>
 * Here is what this class can do:
 * <ul>
 * <li>Copy all source files to a "pool"</li>
 * <li>Extract the metadata and write Packages files</li>
 * <li>Create Release files for components and distributions</li>
 * <li>Create checksum for all files</li>
 * </ul>
 * </p>
 * <p>
 * At the moment this class is still missing some functionality:
 * <ul>
 * <li>Signing is not implemented</li>
 * <li>Compression of index files is not implemented</li>
 * <li>And maybe a few other things</li>
 * </ul>
 * </p>
 * 
 * @author Jens Reimann
 * 
 */
public class AptWriter {

    private AptConfiguration configuration;
    private File pool;
    private File dists;

    private interface Digester {
        public Digest create();

        public String getName();
    }

    private static class SimpleDigester implements Digester {

        private String name;
        private Class<? extends Digest> clazz;

        public SimpleDigester( String name, Class<? extends Digest> clazz ) {
            this.name = name;
            this.clazz = clazz;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Digest create() {
            try {
                return clazz.newInstance();
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    private List<Digester> digesters = new LinkedList<AptWriter.Digester>();

    private static final DateFormat DF;

    private Map<AptComponent, Map<String, List<BinaryPackagePackagesFile>>> files = new HashMap<AptComponent, Map<String, List<BinaryPackagePackagesFile>>>();
    private Console console;

    static {
        DF = new SimpleDateFormat("EEE, dd MMM YYYY HH:mm:ss z", Locale.US);
        DF.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public AptWriter( AptConfiguration configuration, Console console ) {
        this.console = console;
        this.configuration = configuration.clone();
        this.digesters.add(new SimpleDigester("MD5Sum", MD5Digest.class));
        this.digesters.add(new SimpleDigester("SHA1", SHA1Digest.class));
        this.digesters.add(new SimpleDigester("SHA256", SHA256Digest.class));
    }

    public void build() throws Exception {
        if (configuration.getTargetFolder().exists())
            throw new IllegalStateException("The target path must not exists: " + configuration.getTargetFolder());

        if (!configuration.getSourceFolder().isDirectory())
            throw new IllegalStateException("The source path must exists and must be a directory: " + configuration.getTargetFolder());

        configuration.validate();

        configuration.getTargetFolder().mkdirs();

        this.pool = new File(configuration.getTargetFolder(), "pool");
        this.dists = new File(configuration.getTargetFolder(), "dists");

        pool.mkdirs();
        dists.mkdirs();

        FileFilter debFilter = new AndFileFilter( //
                Arrays.asList( //
                        CanReadFileFilter.CAN_READ, //
                        FileFileFilter.FILE, //
                        new SuffixFileFilter(".deb") //
                ) //
        );

        for (File packageFile : configuration.getSourceFolder().listFiles(debFilter)) {
            processPackageFile(packageFile);
        }

        writePackageLists();
    }

    private void writePackageLists() throws IOException {
        for (AptDistribution dist : configuration.getDistributions()) {
            for (AptComponent comp : dist.getComponents()) {
                Map<String, List<BinaryPackagePackagesFile>> fileList = files.get(comp);
                for (Map.Entry<String, List<BinaryPackagePackagesFile>> entry : fileList.entrySet()) {
                    writePackageList(dist, comp, entry.getKey(), entry.getValue());
                }
            }
            writeRelease(dist);
        }
    }

    private void writeRelease( AptDistribution dist ) throws IOException {
        File dir = new File(dists, dist.getName());

        DistributionReleaseFile rf = new DistributionReleaseFile();
        rf.set("Codename", dist.getName());
        rf.set("Origin", dist.getOrigin());
        rf.set("Label", dist.getLabel());
        rf.set("Description", dist.getDescription());
        rf.set("Components", join(dist.getComponents()));
        rf.set("Architectures", join(configuration.getArchitectures()));
        rf.set("Date", DF.format(new Date()));

        for (Digester d : digesters) {
            rf.set(d.getName(), digestPackageLists(rf, d, dist));
        }

        FileOutputStream os = new FileOutputStream(new File(dir, "Release"));
        try {
            os.write(rf.toString().getBytes("UTF-8"));
        } finally {
            os.close();
        }
    }

    private String digestPackageLists( DistributionReleaseFile rf, Digester d, AptDistribution dist ) throws IOException {

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        File distDir = new File(dists, dist.getName()).getCanonicalFile();

        pw.println(); // start with a newline

        for (AptComponent comp : dist.getComponents()) {
            for (String arch : configuration.getArchitectures()) {
                File dir = new File(dists, dist.getName());
                dir = new File(dir, comp.getName());
                dir = new File(dir, "binary-" + arch);

                digestPackageList(pw, d, distDir, new File(dir, "Packages").getCanonicalFile());
                digestPackageList(pw, d, distDir, new File(dir, "Release").getCanonicalFile());
            }
        }

        pw.close();

        return sw.toString();
    }

    private void digestPackageList( PrintWriter pw, Digester d, File distDir, File file ) throws IOException {
        if (!file.exists())
            return;

        String relativeDir = file.getAbsolutePath().substring(distDir.getAbsolutePath().length() + 1); // +1 for the leading/trailing slash

        long size = file.length();
        pw.format(" %s %20s %s", digest(file, d.create()), size, relativeDir);
        pw.println();
    }

    private String join( Collection<?> items ) {
        if (items == null)
            return null;

        StringBuilder sb = new StringBuilder();

        boolean first = true;
        for (Object item : items) {
            if (first)
                first = false;
            else
                sb.append(' ');
            sb.append(item);
        }

        return sb.toString();
    }

    private static void writeField( PrintStream ps, String fieldName, String data, boolean optional ) {
        if (data == null || data.isEmpty()) {
            if (optional)
                return;
            else
                throw new IllegalArgumentException("'" + fieldName + "' must not be null or empty");
        }
        ps.print(fieldName);
        ps.print(": ");
        ps.println(data);
    }

    private void writePackageList( AptDistribution distribution, AptComponent component, String architecture, List<BinaryPackagePackagesFile> files ) throws IOException {
        File dir = new File(this.dists, distribution.getName());

        dir = new File(dir, component.getName());
        dir = new File(dir, "binary-" + architecture);
        dir.mkdirs();

        // Packages

        File packagesFile = new File(dir, "Packages");

        console.info("Writing: " + packagesFile);

        PrintStream ps1 = new PrintStream(packagesFile);
        try {
            for (BinaryPackagePackagesFile cf : files) {
                ps1.println(cf.toString());
            }
        } finally {
            ps1.close();
        }

        // Release

        File releaseFile = new File(dir, "Release");

        console.info("Writing: " + releaseFile);

        ComponentReleaseFile crf = new ComponentReleaseFile();
        crf.set("Component", component.getName());
        crf.set("Architecture", architecture);
        crf.set("Label", component.getLabel());
        crf.set("Origin", component.getDistribution().getOrigin());

        FileOutputStream os = new FileOutputStream(releaseFile);
        try {
            os.write(crf.toString().getBytes("UTF-8"));
        } finally {
            os.close();
        }
    }

    protected void processPackageFile( File packageFile ) throws Exception {
        BinaryPackagePackagesFile cf = readArtifact(packageFile);

        AptComponent component = findComponent(cf);
        if (component == null)
            return; // skip

        console.debug("Processing: " + cf);

        copyArtifact(component, packageFile, cf);

        String arch = cf.get("Architecture");
        if ("all".equals(arch)) {
            for (String ae : configuration.getArchitectures()) {
                registerPackage(component, ae, cf);
            }
        } else {
            if (configuration.getArchitectures().contains(arch))
                registerPackage(component, arch, cf);
        }
    }

    /**
     * Get the component that this package is assigned to
     * <p>
     * Note: This method is called twice at the moment. It must return the same result for the same package data.
     * </p>
     * 
     * @param cf
     *            the package file data, may be <code>null</code>
     * @return the component or <code>null</code> if the package should be ignored
     */
    protected AptComponent findComponent( BinaryPackagePackagesFile cf ) {
        if (cf == null)
            return null;

        // at the moment we allow only one distribution and one component
        // you may override this behavior right here
        return configuration.getDistributions().iterator().next().getComponents().iterator().next();
    }

    private void registerPackage( AptComponent component, String architecture, BinaryPackagePackagesFile cf ) {
        Map<String, List<BinaryPackagePackagesFile>> fileList = files.get(component);
        if (fileList == null) {
            fileList = new HashMap<String, List<BinaryPackagePackagesFile>>();
            files.put(component, fileList);
        }

        List<BinaryPackagePackagesFile> arch = fileList.get(architecture);
        if (arch == null) {
            arch = new LinkedList<BinaryPackagePackagesFile>();
            fileList.put(architecture, arch);
        }
        arch.add(cf);
    }

    private BinaryPackagePackagesFile readArtifact( File packageFile ) throws Exception {
        ArArchiveInputStream in = new ArArchiveInputStream(new FileInputStream(packageFile));
        try {
            ArchiveEntry ar;
            while ((ar = in.getNextEntry()) != null) {
                if (!ar.getName().equals("control.tar.gz"))
                    continue;

                TarArchiveInputStream inputStream = new TarArchiveInputStream(new GZIPInputStream(in));

                try {

                    TarArchiveEntry te;
                    while ((te = inputStream.getNextTarEntry()) != null) {
                        if (!te.getName().equals("./control"))
                            continue;
                        return convert(new BinaryPackageControlFile(inputStream), packageFile);
                    }

                } finally {
                    inputStream.close();
                }
            }
        } finally {
            IOUtils.closeQuietly(in);
        }
        return null;
    }

    private BinaryPackagePackagesFile convert( BinaryPackageControlFile cf, File packageFile ) throws Exception {
        BinaryPackagePackagesFile pf = new BinaryPackagePackagesFile(cf.toString());

        for (Digester d : digesters) {
            pf.set(d.getName(), digest(packageFile, d.create()));
        }

        AptComponent component = findComponent(pf);
        if (component == null)
            return null;

        File targetFile = makeTargetFile(component, packageFile, cf.get("Package"));
        String filename = targetFile.toString().substring(configuration.getTargetFolder().toString().length() + 1);

        pf.set("Filename", filename);

        return pf;
    }

    public static String digest( File file, Digest digest ) throws IOException {
        InputStream in = null;
        try {
            byte[] buffer = new byte[4096];

            in = new FileInputStream(file);
            int rc;
            while ((rc = in.read(buffer)) > 0) {
                digest.update(buffer, 0, rc);
            }
            byte[] dv = new byte[digest.getDigestSize()];
            digest.doFinal(dv, 0);
            StringBuilder sb = new StringBuilder();
            for (byte b : dv) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    private void copyArtifact( AptComponent component, File packageFile, BinaryPackagePackagesFile cf ) throws IOException {
        String name = cf.get("Package");

        File targetFile = makeTargetFile(component, packageFile, name);
        console.info("Copy artifact: " + targetFile);
        targetFile.mkdirs();
        Files.copy(packageFile.toPath(), targetFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
    }

    private File makeTargetFile( AptComponent component, File packageFile, String packageName ) {
        File targetFile = new File(this.pool, component.getName());
        targetFile = new File(targetFile, packageName.substring(0, 1));
        targetFile = new File(targetFile, packageName);
        targetFile = new File(targetFile, packageFile.getName());
        return targetFile.getAbsoluteFile();
    }
}
