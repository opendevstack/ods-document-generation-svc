package org.ods.doc.gen.external.modules.nexus

import groovy.util.logging.Slf4j
import kong.unirest.HttpResponse
import kong.unirest.Unirest
import net.lingala.zip4j.ZipFile
import org.apache.http.client.utils.URIBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.nio.file.Paths

@Slf4j
@Service
class NexusService {

    URI baseURL

    final String username
    final String password

    @Inject
    NexusService(@Value('${nexus.url}') String baseURL,
                 @Value('${nexus.username}') String username,
                 @Value('${nexus.password}') String password) {
        if (!baseURL?.trim()) {
            throw new IllegalArgumentException("Error: unable to connect to Nexus. 'baseURL' is undefined.")
        }

        if (!username?.trim()) {
            throw new IllegalArgumentException("Error: unable to connect to Nexus. 'username' is undefined.")
        }

        if (!password?.trim()) {
            throw new IllegalArgumentException("Error: unable to connect to Nexus. 'password' is undefined.")
        }

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

    URI storeArtifact(String repository, String directory, String name, byte[] artifact, String contentType) {
        Map nexusParams = [
            'raw.directory': directory,
            'raw.asset1.filename': name,
        ]

        return storeComplextArtifact(repository, artifact, contentType, 'raw', nexusParams)
    }

    URI storeArtifactFromFile(
        String repository,
        String directory,
        String name,
        File artifact,
        String contentType) {
        return storeArtifact(repository, directory, name, artifact.getBytes(), contentType)
    }

    @SuppressWarnings('LineLength')
    URI storeComplextArtifact(String repository, byte[] artifact, String contentType, String repositoryType, Map nexusParams = [ : ]) {
        def restCall = Unirest.post("${this.baseURL}/service/rest/v1/components?repository={repository}")
            .routeParam('repository', repository)
            .basicAuth(this.username, this.password)

        nexusParams.each { key, value ->
            restCall = restCall.field(key, value)
        }

        restCall = restCall.field(
            repositoryType == 'raw' || repositoryType == 'maven2' ? "${repositoryType}.asset1" : "${repositoryType}.asset",
            new ByteArrayInputStream(artifact), contentType)

        def response = restCall.asString()
        response.ifSuccess {
            if (response.getStatus() != 204) {
                throw new RuntimeException(
                    'Error: unable to store artifact. ' +
                        "Nexus responded with code: '${response.getStatus()}' and message: '${response.getBody()}'."
                )
            }
        }

        response.ifFailure {
            def message = 'Error: unable to store artifact. ' +
                "Nexus responded with code: '${response.getStatus()}' and message: '${response.getBody()}'."

            if (response.getStatus() == 404) {
                message = "Error: unable to store artifact. Nexus could not be found at: '${this.baseURL}' with repo: ${repository}."
            }

            throw new RuntimeException(message)
        }

        if (repositoryType == 'raw') {
            return this.baseURL.resolve("/repository/${repository}/${nexusParams['raw.directory']}/" +
              "${nexusParams['raw.asset1.filename']}")
        }
        return this.baseURL.resolve("/repository/${repository}")
    }

    @SuppressWarnings(['JavaIoPackageAccess'])
    Map<URI, File> retrieveArtifact(String nexusRepository, String nexusDirectory, String name, String extractionPath) {
        String urlToDownload = "${this.baseURL}/repository/${nexusRepository}/${nexusDirectory}/${name}"
        HttpResponse<File> response = downloadToPath(urlToDownload, name, extractionPath)
        return [
            uri: this.baseURL.resolve("/repository/${nexusRepository}/${nexusDirectory}/${name}"),
            content: response.getBody(),
        ]
    }

    HttpResponse<File> downloadToPath(String urlToDownload, String name, String extractionPath) {
        deleteIfAlreadyExist(extractionPath, name)

        def restCall = Unirest.get("${urlToDownload}").basicAuth(this.username, this.password)
        HttpResponse<File> response = restCall.asFile("${extractionPath}/${name}")
        response.ifFailure {
            def message = 'Error: unable to get artifact. ' +
                    "Nexus responded with code: '${response.getStatus()}' and message: '${response.getBody()}'." +
                    " The url called was: ${urlToDownload}"
            if (response.getStatus() == 404) {
                message = "Error: unable to get artifact. Nexus could not be found at: '${urlToDownload}'."
            }
            if (response.getStatus() != 200) {
                throw new RuntimeException(message)
            }
        }

        return response
    }

    void
    downloadAndExtractZip(String urlToDownload, String extractionPath){
        String artifactName = new URL(urlToDownload).getFile().split("/").last()
        downloadToPath(urlToDownload, artifactName, extractionPath)
        ZipFile zipFile = new ZipFile(Paths.get(extractionPath, artifactName).toString())
        zipFile.extractAll(extractionPath)
    }

    private void deleteIfAlreadyExist(String extractionPath, String name) {
        File artifactExists = new File("${extractionPath}/${name}")
        if (artifactExists) {
            artifactExists.delete()
        }
    }

}
