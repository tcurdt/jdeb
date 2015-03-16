/*
 * Copyright 2015 The jdeb developers.
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

package org.vafer.jdeb;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.util.encoders.Hex;
import org.vafer.jdeb.changes.ChangeSet;
import org.vafer.jdeb.changes.ChangesProvider;
import org.vafer.jdeb.changes.TextfileChangesProvider;
import org.vafer.jdeb.debian.BinaryPackageControlFile;
import org.vafer.jdeb.debian.ChangesFile;
import org.vafer.jdeb.signing.PGPSigner;
import org.vafer.jdeb.utils.PGPSignatureOutputStream;
import org.vafer.jdeb.utils.Utils;
import org.vafer.jdeb.utils.VariableResolver;

/**
 * A generic class for creating Debian archives. Even supports signed changes
 * files.
 */
public class DebMaker {

    /** A console to output log message with */
    private Console console;

    /** The Debian package produced */
    private File deb;

    /** The directory containing the control files to build the package */
    private File control;

    /** The name of the package. Default value if not specified in the control file */
    private String packageName;

    /** The section of the package. Default value if not specified in the control file */
    private String section = "java";

    /** The dependencies of the package. Default value if not specified in the control file */
    private String depends = "default-jre | java6-runtime";

    /** The description of the package. Default value if not specified in the control file */
    private String description;

    /** The homepage of the application. Default value if not specified in the control file */
    private String homepage;

    /** The file containing the PGP keys */
    private File keyring;

    /** The key to use in the keyring */
    private String key;

    /** The passphrase for the key to sign the changes file */
    private String passphrase;

    /** The file to read the changes from */
    private File changesIn;

    /** The file where to write the changes to */
    private File changesOut;

    /** The file where to write the changes of the changes input to */
    private File changesSave;

    /** The compression method used for the data file (none, gzip, bzip2 or xz) */
    private String compression = "gzip";

    /** Whether to sign the package that is created */
    private boolean signPackage;

    /** Defines which utility is used to verify the signed package */
    private String signMethod;

    /** Defines the role to sign with */
    private String signRole;

    private VariableResolver variableResolver;
    private String openReplaceToken;
    private String closeReplaceToken;

    private final Collection<DataProducer> dataProducers = new ArrayList<DataProducer>();

    private final Collection<DataProducer> conffilesProducers = new ArrayList<DataProducer>();


    public DebMaker(Console console, Collection<DataProducer> dataProducers, Collection<DataProducer> conffileProducers) {
        this.console = console;
        if (dataProducers != null) {
            this.dataProducers.addAll(dataProducers);
        }
        if (conffileProducers != null) {
            this.conffilesProducers.addAll(conffileProducers);
        }

        Security.addProvider(new BouncyCastleProvider());
    }

    public void setDeb(File deb) {
        this.deb = deb;
    }

    public void setControl(File control) {
        this.control = control;
    }

    public void setPackage(String packageName) {
        this.packageName = packageName;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public void setDepends(String depends) {
        this.depends = depends;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setHomepage(String homepage) {
        this.homepage = homepage;
    }

    public void setChangesIn(File changes) {
        this.changesIn = changes;
    }

    public void setChangesOut(File changes) {
        this.changesOut = changes;
    }

    public void setChangesSave(File changes) {
        this.changesSave = changes;
    }

    public void setSignPackage(boolean signPackage) {
        this.signPackage = signPackage;
    }

    public void setSignMethod(String signMethod) {
        this.signMethod = signMethod;
    }

    public void setSignRole(String signRole) {
        this.signRole = signRole;
    }

    public void setKeyring(File keyring) {
        this.keyring = keyring;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public void setResolver(VariableResolver variableResolver) {
        this.variableResolver = variableResolver;
    }

    private boolean isWritableFile(File file) {
        return !file.exists() || file.isFile() && file.canWrite();
    }

    /**
     * Validates the input parameters.
     */
    public void validate() throws PackagingException {
        if (control == null || !control.isDirectory()) {
            throw new PackagingException("The 'control' attribute doesn't point to a directory. " + control);
        }

        if (changesIn != null) {

            if (changesIn.exists() && (!changesIn.isFile() || !changesIn.canRead())) {
                throw new PackagingException("The 'changesIn' setting needs to point to a readable file. " + changesIn + " was not found/readable.");
            }

            if (changesOut != null && !isWritableFile(changesOut)) {
                throw new PackagingException("Cannot write the output for 'changesOut' to " + changesOut);
            }

            if (changesSave != null && !isWritableFile(changesSave)) {
                throw new PackagingException("Cannot write the output for 'changesSave' to " + changesSave);
            }

        } else {
            if (changesOut != null || changesSave != null) {
                throw new PackagingException("The 'changesOut' or 'changesSave' settings may only be used when there is a 'changesIn' specified.");
            }
        }

        if (Compression.toEnum(compression) == null) {
            throw new PackagingException("The compression method '" + compression + "' is not supported (expected 'none', 'gzip', 'bzip2' or 'xz')");
        }

        if (deb == null) {
            throw new PackagingException("You need to specify where the deb file is supposed to be created.");
        }
    }

    public void makeDeb() throws PackagingException {
        BinaryPackageControlFile packageControlFile;
        try {
            console.info("Creating debian package: " + deb);

            // If we should sign the package
            boolean doSign = signPackage;

            if (doSign) {

                if (keyring == null || !keyring.exists()) {
                    doSign = false;
                    console.warn("Signing requested, but no keyring supplied");
                }

                if (key == null) {
                    doSign = false;
                    console.warn("Signing requested, but no key supplied");
                }

                if (passphrase == null) {
                    doSign = false;
                    console.warn("Signing requested, but no passphrase supplied");
                }

                FileInputStream keyRingInput = new FileInputStream(keyring);
                PGPSigner signer = null;
                try {
                    signer = new PGPSigner(new FileInputStream(keyring), key, passphrase);
                } finally {
                    keyRingInput.close();
                }

                int digest = PGPUtil.SHA1;

                PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(new BcPGPContentSignerBuilder(signer.getSecretKey().getPublicKey().getAlgorithm(), digest));
                signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, signer.getPrivateKey());

                packageControlFile = createSignedDeb(Compression.toEnum(compression), signatureGenerator, signer);
            } else {
                packageControlFile = createDeb(Compression.toEnum(compression));
            }

        } catch (Exception e) {
            throw new PackagingException("Failed to create debian package " + deb, e);
        }

        makeChangesFiles(packageControlFile);
    }

    private void makeChangesFiles(final BinaryPackageControlFile packageControlFile) throws PackagingException {
        if (changesOut == null) {
            changesOut = new File(deb.getParentFile(), deb.getName().replace(".deb", ".changes"));
        }

        ChangesProvider changesProvider;
        FileOutputStream out = null;

        try {
            console.info("Creating changes file: " + changesOut);

            out = new FileOutputStream(changesOut);

            if (changesIn != null && changesIn.exists()) {
                // read the changes form a textfile provider
                changesProvider = new TextfileChangesProvider(new FileInputStream(changesIn), packageControlFile);
            } else {
                // create an empty changelog
                changesProvider = new ChangesProvider() {
                    @Override
                    public ChangeSet[] getChangesSets() {
                        return new ChangeSet[] {
                                new ChangeSet(packageControlFile.get("Package"),
                                        packageControlFile.get("Version"),
                                        new Date(),
                                        packageControlFile.get("Distribution"),
                                        packageControlFile.get("Urgency"),
                                        packageControlFile.get("Maintainer"),
                                        new String[0])
                        };
                    }
                };
            }

            ChangesFileBuilder builder = new ChangesFileBuilder();
            ChangesFile changesFile = builder.createChanges(packageControlFile, deb, changesProvider);

            if (keyring != null && key != null && passphrase != null) {
                console.info("Signing the changes file with the key " + key);
                PGPSigner signer = new PGPSigner(new FileInputStream(keyring), key, passphrase);
                signer.clearSign(changesFile.toString(), out);
            } else {
                out.write(changesFile.toString().getBytes("UTF-8"));
            }
            out.flush();

        } catch (Exception e) {
            throw new PackagingException("Failed to create the Debian changes file " + changesOut, e);
        } finally {
            IOUtils.closeQuietly(out);
        }

        if (changesSave == null || !(changesProvider instanceof TextfileChangesProvider)) {
            return;
        }

        try {
            console.info("Saving changes to file: " + changesSave);

            ((TextfileChangesProvider) changesProvider).save(new FileOutputStream(changesSave));

        } catch (Exception e) {
            throw new PackagingException("Failed to save debian changes file " + changesSave, e);
        }
    }

    private List<String> populateConffiles(Collection<DataProducer> producers) {
        final List<String> result = new ArrayList<String>();

        if (producers == null || producers.isEmpty()) {
            return result;
        }

        final DataConsumer receiver = new DataConsumer() {
            @Override
            public void onEachFile(InputStream input, TarArchiveEntry entry)  {
                String tempConffileItem = entry.getName();
                if (tempConffileItem.startsWith(".")) {
                    tempConffileItem = tempConffileItem.substring(1);
                }
                console.info("Adding conffile: " + tempConffileItem);
                result.add(tempConffileItem);
            }

            @Override
            public void onEachLink(TarArchiveEntry entry)  {
            }

            @Override
            public void onEachDir(String dirname, String linkname, String user, int uid, String group, int gid, int mode, long size)  {
            }
        };

        try {
            for (DataProducer data : producers) {
                data.produce(receiver);
            }
        } catch(Exception e) {
            //
        }

        return result;
    }

    /**
     * Create the debian archive with from the provided control files and data producers.
     *
     * @param compression   the compression method used for the data file
     * @return BinaryPackageControlFile
     * @throws PackagingException
     */
    public BinaryPackageControlFile createDeb(Compression compression) throws PackagingException {
        return createSignedDeb(compression, null, null);
    }
    /**
     * Create the debian archive with from the provided control files and data producers.
     *
     * @param compression   the compression method used for the data file (gzip, bzip2 or anything else for no compression)
     * @param signatureGenerator   the signature generator
     *
     * @return PackageDescriptor
     * @throws PackagingException
     */
    public BinaryPackageControlFile createSignedDeb(Compression compression, final PGPSignatureGenerator signatureGenerator, PGPSigner signer ) throws PackagingException {
        File tempData = null;
        File tempControl = null;

        try {
            tempData = File.createTempFile("deb", "data");
            tempControl = File.createTempFile("deb", "control");

            console.debug("Building data");
            DataBuilder dataBuilder = new DataBuilder(console);
            StringBuilder md5s = new StringBuilder();
            BigInteger size = dataBuilder.buildData(dataProducers, tempData, md5s, compression);

            console.info("Building conffiles");
            List<String> tempConffiles = populateConffiles(conffilesProducers);

            console.debug("Building control");
            ControlBuilder controlBuilder = new ControlBuilder(console, variableResolver, openReplaceToken, closeReplaceToken);
            BinaryPackageControlFile packageControlFile = controlBuilder.createPackageControlFile(new File(control, "control"), size);
            if (packageControlFile.get("Package") == null) {
                packageControlFile.set("Package", packageName);
            }
            if (packageControlFile.get("Depends") == null) {
                packageControlFile.set("Depends", depends);
            }
            if (packageControlFile.get("Section") == null) {
                packageControlFile.set("Section", section);
            }
            if (packageControlFile.get("Description") == null) {
                packageControlFile.set("Description", description);
            }
            if (packageControlFile.get("Homepage") == null) {
                packageControlFile.set("Homepage", homepage);
            }

            controlBuilder.buildControl(packageControlFile, control.listFiles(), tempConffiles , md5s, tempControl);

            if (!packageControlFile.isValid()) {
                throw new PackagingException("Control file fields are invalid " + packageControlFile.invalidFields() +
                        ". The following fields are mandatory: " + packageControlFile.getMandatoryFields() +
                        ". Please check your pom.xml/build.xml and your control file.");
            }

            deb.getParentFile().mkdirs();

            ArArchiveOutputStream ar = new ArArchiveOutputStream(new FileOutputStream(deb));

            String binaryName = "debian-binary";
            String binaryContent = "2.0\n";
            String controlName = "control.tar.gz";
            String dataName = "data.tar" + compression.getExtension();

            addTo(ar, binaryName, binaryContent);
            addTo(ar, controlName, tempControl);
            addTo(ar, dataName, tempData);

            if (signatureGenerator != null) {
                console.info("Signing package with key " + key);

                if(signRole == null) {
                    signRole = "origin";
                }

                // Use debsig-verify as default
                if(signMethod == null || !"dpkg-sig".equals(signMethod)) {
                    // Sign file to verify with debsig-verify
                    PGPSignatureOutputStream sigStream = new PGPSignatureOutputStream(signatureGenerator);

                    addTo(sigStream, binaryContent);
                    addTo(sigStream, tempControl);
                    addTo(sigStream, tempData);
                    addTo(ar, "_gpg" + signRole, sigStream.generateASCIISignature());

                } else {

                    // Sign file to verify with dpkg-sig --verify
                    final String outputStr =
                            "Version: 4\n" +
                                    "Signer: \n" +
                                    "Date: " + new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.ENGLISH).format(new Date()) + "\n" +
                                    "Role: " + signRole +"\n" +
                                    "Files: \n" +
                                    addFile(binaryName, binaryContent) +
                                    addFile(controlName, tempControl) +
                                    addFile(dataName, tempData);

                    ByteArrayOutputStream message = new ByteArrayOutputStream();
                    signer.clearSign(outputStr, message);

                    addTo(ar, "_gpg" + signRole, message.toString());
                }
            }

            ar.close();

            return packageControlFile;

        } catch (Exception e) {
            throw new PackagingException("Could not create deb package", e);
        } finally {
            if (tempData != null) {
                if (!tempData.delete()) {
                    console.warn("Could not delete the temporary file " + tempData);
                }
            }
            if (tempControl != null) {
                if (!tempControl.delete()) {
                    console.warn("Could not delete the temporary file " + tempControl);
                }
            }
        }
    }

    private String addFile(String name, String input){
        return addLine(md5Hash(input), sha1Hash(input), input.length(), name);
    }

    private String addFile(String name, File input){
        return addLine(md5Hash(input), sha1Hash(input), input.length(), name);
    }

    private String addLine(String md5, String sha1, long size, String name){
        return "\t" + md5 + " " + sha1 + " " + size + " " + name + "\n";
    }

    private String md5Hash(String input){
        return md5Hash(input.getBytes());
    }

    private String md5Hash(File input){
        try {
            return md5Hash(FileUtils.readFileToByteArray(input));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    private String md5Hash(byte input[]){
        //update the input of MD5
        MD5Digest md5 = new MD5Digest();
        md5.update(input, 0, input.length);

        //get the output/ digest size and hash it
        byte[] digest = new byte[md5.getDigestSize()];
        md5.doFinal(digest, 0);

        return new String(Hex.encode(digest));
    }

    private String sha1Hash(String input){
        return sha1Hash(input.getBytes());
    }

    private String sha1Hash(File input){
        try {
            return sha1Hash(FileUtils.readFileToByteArray(input));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    private String sha1Hash(byte input[]){
        try
        {
            //prepare the input
            MessageDigest hash = MessageDigest.getInstance("SHA1");
            hash.update(input);

            //proceed ....
            byte[] digest = hash.digest();

            return new String(Hex.encode(digest));
        }
        catch (NoSuchAlgorithmException e)
        {
            System.err.println("No such algorithm");
            e.printStackTrace();
        }

        return null;
    }

    private void addTo(ArArchiveOutputStream pOutput, String pName, String pContent) throws IOException {
        final byte[] content = pContent.getBytes();
        pOutput.putArchiveEntry(new ArArchiveEntry(pName, content.length));
        pOutput.write(content);
        pOutput.closeArchiveEntry();
    }

    private void addTo(ArArchiveOutputStream pOutput, String pName, File pContent) throws IOException {
        pOutput.putArchiveEntry(new ArArchiveEntry(pName, pContent.length()));

        final InputStream input = new FileInputStream(pContent);
        try {
            Utils.copy(input, pOutput);
        } finally {
            input.close();
        }

        pOutput.closeArchiveEntry();
    }

    private void addTo(final PGPSignatureOutputStream pOutput, final String pContent) throws IOException {
        final byte[] content = pContent.getBytes();
        pOutput.write(content);
    }

    private void addTo(final PGPSignatureOutputStream pOutput, final File pContent) throws IOException {
        final InputStream input = new FileInputStream(pContent);
        try {
            Utils.copy(input, pOutput);
        } finally {
            input.close();
        }
    }

    public void setOpenReplaceToken(String openReplaceToken) {
        this.openReplaceToken = openReplaceToken;
    }

    public void setCloseReplaceToken(String closeReplaceToken) {
        this.closeReplaceToken = closeReplaceToken;
    }
}
