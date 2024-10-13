import java.nio.file.Files
import org.apache.commons.compress.archivers.ar.ArArchiveEntry
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream

File deb = new File( basedir, 'target/jdeb-it_1.0_all.deb' )
assert deb.exists();
ArArchiveInputStream is = new ArArchiveInputStream(new FileInputStream(deb))
ArArchiveEntry entry
while ((entry = is.getNextEntry()) != null) {
    assert entry.getLastModifiedDate().getTime() == 1724680800000L
}

File changes = new File( basedir, 'target/jdeb-it_1.0_all.changes' )
assert changes.exists();
List<String> lines = Files.readAllLines(changes.toPath())
assert "Date: Mon, 26 Aug 2024 14:00:00 +0000".equals(lines.get(1))
