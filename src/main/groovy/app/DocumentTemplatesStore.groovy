package app

import java.nio.file.Path
import java.net.URI

interface DocumentTemplatesStore {

    // Get document templates of a specific version into a target directory
    public Path getTemplatesForVersion(String version, Path targetDir)
    
    // Get a URI to download document templates of a specific version
    def URI getZipArchiveDownloadURI(String version)
    
    boolean isApplicableToSystemConfig ()
}
