package org.ods.doc.gen.external.modules.nexus

import groovy.util.logging.Slf4j
import kong.unirest.HttpResponse
import kong.unirest.Unirest
import net.lingala.zip4j.ZipFile
import org.apache.http.client.utils.URIBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.nio.file.Files
import java.nio.file.Paths

@Slf4j
@Service
class NexusService {

    static final String NEXUS_REPOSITORY = "leva-documentation"
    private static final String URL_PATH = "service/rest/v1/components?repository={repository}"
    private static final String RAW = 'raw'
    private static final String UNABLE_STORE = 'Error: unable to store artifact. '

    URI baseURL

    final String username
    final String password

    @Inject
    NexusService(@Value('${nexus.url}') String baseURL,
                 @Value('${nexus.username}') String username,
                 @Value('${nexus.password}') String password) {
        log.info("NexusService - url:[${baseURL}], username:[${username}]")
        checkParams(baseURL, username, password)
        try {
            this.baseURL = new URIBuilder(baseURL).build()
        } catch (e) {
            throw new IllegalArgumentException(
                "Error: unable to connect to Nexus. '${baseURL}' is not a valid URI."
            ).initCause(e)
        }

        this.username = username
        this.password = password
    }

    URI storeArtifact(String directory, String name, String artifactPath, String contentType) {
        Map nexusParams = [
            'raw.directory': directory,
            'raw.asset1.filename': name,
        ]

        return storeWithRaw(NEXUS_REPOSITORY, artifactPath, contentType, RAW, nexusParams)
    }

    private URI storeWithRaw(String repo,
                                      String artifactPath,
                                      String contentType,
                                      String repoType,
                                      Map nexusParams) {
        File artifactFile = Paths.get(artifactPath).toFile()
        String targetUrl = "${this.baseURL}/${URL_PATH}"
        log.info("Nexus store artifact:[${artifactFile}] - repo: [${repo}], url:[${targetUrl}] ")

        byte[] artifact = artifactFile.getBytes()
        def restCall = Unirest
                .post(targetUrl)
                .routeParam('repository', repo)
                .basicAuth(this.username, this.password)
                .field(getField(repoType), new ByteArrayInputStream(artifact), contentType)
        nexusParams.each { key, value -> restCall = restCall.field(key, value) }

        def response = restCall.asString()
        response.ifSuccess {
            if (response.getStatus() != 204) {
                throw new RuntimeException(errorMsg(response, repo))
            }
        }

        response.ifFailure {
            throw new RuntimeException(errorMsg(response, repo))
        }

        if (repoType == RAW) {
            String url = "/repository/${repo}/${nexusParams['raw.directory']}/${nexusParams['raw.asset1.filename']}"
            return this.baseURL.resolve(url)
        }
        return this.baseURL.resolve("/repository/${repo}")
    }

    private String errorMsg(HttpResponse<String> response, String repo) {
        String message = UNABLE_STORE +
                "Nexus responded with code: '${response.getStatus()}' and message: '${response.getBody()}'."
        if (response.getStatus() == 404) {
            message = "Error: unable to store artifact. Nexus could not be found at: '${this.baseURL}' - repo: ${repo}."
        }
        return message
    }

    private String getField(String repoType) {
        repoType == RAW || repoType == 'maven2' ? "${repoType}.asset1" : "${repoType}.asset"
    }

    Map<URI, File> retrieveArtifact(String nexusRepository, String nexusDirectory, String name, String extractionPath) {
        String urlToDownload = getURL(nexusRepository, nexusDirectory, name)
        HttpResponse<File> response = downloadToPath(urlToDownload, extractionPath, name)
        return [
            uri: this.baseURL.resolve("/repository/${nexusRepository}/${nexusDirectory}/${name}"),
            content: response.getBody(),
        ]
    }

    private String getURL(String nexusRepository, String nexusDirectory, String name) {
        return "/repository/${nexusRepository}/${nexusDirectory}/${name}"
    }

    private HttpResponse<File> downloadToPath(String urlToDownload, String extractionPath, String name) {
        deleteIfAlreadyExist(extractionPath, name)
        String fullUrlToDownload = new URI(baseURL.toString() + "/"+ urlToDownload).normalize().toString()
        log.info("downloadToPath::${fullUrlToDownload}")
        def restCall = Unirest.get(fullUrlToDownload).basicAuth(this.username, this.password)
        HttpResponse<File> response = restCall.asFile("${extractionPath}/${name}")
        response.ifFailure {
            def message = 'Error: unable to get artifact. ' +
                    "Nexus responded with code: '${response.getStatus()}' and message: '${response.getBody()}'." +
                    " The url called was: ${fullUrlToDownload}"
            if (response.getStatus() == 404) {
                message = "Error: unable to get artifact. Nexus could not be found at: '${fullUrlToDownload}'."
            }
            if (response.getStatus() != 200) {
                throw new RuntimeException(message)
            }
        }

        return response
    }

    void downloadAndExtractZip(String jiraProjectKey, String version, String extractionPath, String artifactName) {
        String nexusDirectory = "${jiraProjectKey}-${version}"
        def urlToDownload = getURL(NEXUS_REPOSITORY, nexusDirectory, artifactName)
        downloadAndExtractZip(urlToDownload, extractionPath)
    }

    void downloadAndExtractZip(String urlToDownload, String extractionPath) {
        log.debug("downloadAndExtractZip: urlToDownload:${urlToDownload}, extractionPath:${extractionPath}")
        String artifactName = urlToDownload.split("/").last()
        downloadToPath(urlToDownload, extractionPath, artifactName)
        extractZip(extractionPath, artifactName)
    }

    private void extractZip(String extractionPath, String artifactName) {
        ZipFile zipFile = new ZipFile(Paths.get(extractionPath, artifactName).toString())
        Files.createDirectories(Paths.get(extractionPath))
        zipFile.extractAll(extractionPath)
    }

    private void deleteIfAlreadyExist(String extractionPath, String name) {
        File artifactExists = new File("${extractionPath}/${name}")
        if (artifactExists) {
            artifactExists.delete()
        }
    }

    private void checkParams(String baseURL, String username, String password) {
        if (!baseURL?.trim() || baseURL == "null") {
            throw new IllegalArgumentException("Error: unable to connect to Nexus. 'baseURL' is undefined.")
        }

        if (!username?.trim()) {
            throw new IllegalArgumentException("Error: unable to connect to Nexus. 'username' is undefined.")
        }

        if (!password?.trim()) {
            throw new IllegalArgumentException("Error: unable to connect to Nexus. 'password' is undefined.")
        }
    }

}
