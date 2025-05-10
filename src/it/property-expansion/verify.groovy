import java.nio.file.Files
import java.util.zip.GZIPInputStream
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream
import org.apache.commons.compress.archivers.ar.ArArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream

File deb = new File(basedir, 'target/jdeb-it_1.0_all.deb')
assert deb.exists()

InputStream debInput = new FileInputStream(deb)
ArArchiveInputStream arInput = new ArArchiveInputStream(debInput)

ArArchiveEntry entry
while ((entry = arInput.getNextEntry()) != null) {
    if (entry.getName() == 'control.tar.gz') {
        println "Found control.tar.gz, extracting..."

        // Save control.tar.gz to a temporary file
        File tempControlGz = File.createTempFile("control", ".tar.gz")
        tempControlGz.deleteOnExit()
        tempControlGz.withOutputStream { out ->
            byte[] buffer = new byte[4096]
            int len
            while ((len = arInput.read(buffer)) != -1) {
                out.write(buffer, 0, len)
            }
        }

        // Read the contents of control.tar.gz
        GZIPInputStream gzipIn = new GZIPInputStream(new FileInputStream(tempControlGz))
        TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn)

        def tarEntry
        while ((tarEntry = tarIn.nextEntry) != null) {
            String name = tarEntry.name
            println "control.tar.gz contains: $name"

            if (name == './control' || name == 'control') {
                println "\n=== Content of 'control' file ==="
                BufferedReader reader = new BufferedReader(new InputStreamReader(tarIn, 'UTF-8'))
                String line
                while ((line = reader.readLine()) != null) {
                    println line
                }
                println "=== End of 'control' file ===\n"
            }
        }

        tarIn.close()
        gzipIn.close()
    }
}

arInput.close()
debInput.close()
