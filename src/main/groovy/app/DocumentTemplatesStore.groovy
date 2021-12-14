package app

import java.nio.file.Path

interface DocumentTemplatesStore {

    // Get document templates of a specific version into a target directory
    Path getTemplatesForVersion(String version, Path targetDir)
    
    // Get a URI to download document templates of a specific version
    URI getZipArchiveDownloadURI(String version)
    
    boolean isApplicableToSystemConfig ()
}
