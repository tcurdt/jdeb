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

package org.vafer.jdeb.signing;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.vafer.jdeb.ArchiveVisitor;
import org.vafer.jdeb.ArchiveWalker;
import org.vafer.jdeb.Compression;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.DebMaker;
import org.vafer.jdeb.NullConsole;
import org.vafer.jdeb.debian.BinaryPackageControlFile;
import org.vafer.jdeb.producers.DataProducerArchive;
import org.vafer.jdeb.producers.DataProducerDirectory;
import org.vafer.jdeb.producers.DataProducerLink;

public class DebMakerTestCase extends TestCase {

    public void testCreation() throws Exception {

        File control = new File(getClass().getResource("../deb/control/control").toURI());
        File archive1 = new File(getClass().getResource("../deb/data.tgz").toURI());
        File archive2 = new File(getClass().getResource("../deb/data.tar.bz2").toURI());
        File archive3 = new File(getClass().getResource("../deb/data.zip").toURI());
        File directory = new File(getClass().getResource("../deb/data").toURI());
        
        final InputStream ring = getClass().getClassLoader().getResourceAsStream("org/vafer/gpg/secring.gpg");

        DataProducer[] data = new DataProducer[] {
            new DataProducerArchive(archive1, null, null, null),
            new DataProducerArchive(archive2, null, null, null),
            new DataProducerArchive(archive3, null, null, null),
            new DataProducerDirectory(directory, null, new String[] { "**/.svn/**" }, null),
            new DataProducerLink("/link/path-element.ext", "/link/target-element.ext", true, null, null, null)
        };

        int digest = PGPUtil.SHA1;
        PGPSigner signer = new PGPSigner(ring, "2E074D8F", "test");
        PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(new BcPGPContentSignerBuilder(signer.getSecretKey().getPublicKey().getAlgorithm(), digest));
        signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, signer.getPrivateKey());
        
        for(int i = 0; i <=1; i++){
	        File deb = File.createTempFile("jdeb", ".deb");
	
	        DebMaker maker = new DebMaker(new NullConsole(), Arrays.asList(data), null);
	        maker.setControl(new File(getClass().getResource("../deb/control").toURI()));
	        maker.setDeb(deb);
	        
	        if(i==0)
	        	maker.setSignMethod("debsig-verify");
	        else
	        	maker.setSignMethod("dpkg-sig");
	        
	        BinaryPackageControlFile packageControlFile = maker.createSignedDeb(Compression.GZIP, signatureGenerator, signer);
	        
	        assertTrue(packageControlFile.isValid());
	
	        final Map<String, TarArchiveEntry> filesInDeb = new HashMap<String, TarArchiveEntry>();
	        
	        ArchiveWalker.walkData(deb, new ArchiveVisitor<TarArchiveEntry>() {
	            public void visit(TarArchiveEntry entry, byte[] content) throws IOException {
	                filesInDeb.put(entry.getName(), entry);
	            }
	        }, Compression.GZIP);
	        
	        assertTrue("_gpgorigin wasn't found in the package", ArchiveWalker.arArchiveContains(deb, "_gpgorigin"));
	        assertTrue("debian-binary wasn't found in the package", ArchiveWalker.arArchiveContains(deb, "debian-binary"));
	        assertTrue("control.tar.gz wasn't found in the package", ArchiveWalker.arArchiveContains(deb, "control.tar.gz"));
	        assertTrue("testfile wasn't found in the package", filesInDeb.containsKey("./test/testfile"));
	        assertTrue("testfile2 wasn't found in the package", filesInDeb.containsKey("./test/testfile2"));
	        assertTrue("testfile3 wasn't found in the package", filesInDeb.containsKey("./test/testfile3"));
	        assertTrue("testfile4 wasn't found in the package", filesInDeb.containsKey("./test/testfile4"));
	        assertTrue("/link/path-element.ext wasn't found in the package", filesInDeb.containsKey("./link/path-element.ext"));
	        assertEquals("/link/path-element.ext has wrong link target", "/link/target-element.ext", filesInDeb.get("./link/path-element.ext").getLinkName());
	        
	        if(i==0){
	        	FileUtils.copyFile(deb, new File("./target/test_debsig-verify.deb"));
	        }else{
	        	FileUtils.copyFile(deb, new File("./target/test_dpkg-sig.deb"));
	        }
	        
	        assertTrue("Cannot delete the file " + deb, deb.delete());
        }
    }
}
