package org.ods.doc.gen.leva.doc.services

import com.github.tomakehurst.wiremock.client.WireMock
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.mockito.Mock
import org.ods.doc.gen.core.ZipFacade
import org.ods.doc.gen.core.test.SpecHelper
import org.ods.doc.gen.external.modules.git.BitbucketTraceabilityUseCase
import org.ods.doc.gen.external.modules.jira.JiraUseCase
import org.ods.doc.gen.external.modules.nexus.NexusService
import org.ods.doc.gen.external.modules.sonar.SonarQubeUseCase
import org.ods.doc.gen.external.modules.xunit.JUnitReportsService
import org.ods.doc.gen.project.data.Project
import org.ods.doc.gen.project.data.ProjectData

import java.nio.file.Paths

class DocGenUseCaseSpec  extends SpecHelper {

    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Mock
    Project project
    @Mock
    ZipFacade util
    @Mock
    DocGenService docGen
    @Mock
    JiraUseCase jiraUseCase
    @Mock
    JUnitReportsService junit
    @Mock
    LeVADocumentChaptersFileService levaFiles
    @Mock
    PDFUtil pdf
    @Mock
    SonarQubeUseCase sq
    @Mock
    BitbucketTraceabilityUseCase bbt

    def setup() {

    }

    NexusService createNexusService(int port, String username, String password) {
        return new NexusService("http://localhost:${port}", username, password)
    }

    Map getArtifactRequestData(String repo, String dir, String name, Map mixins = [:]) {
        def result = [
                data: [
                        repository: "${repo}",
                        directory: "${dir}",
                        name: "${name}",
                ],
                password: "password",
                path: "/repository/${repo}/${dir}/${name}",
                username: "username"
        ]
        return result << mixins
    }

    byte[] getExampleFileBytes() {
        return Paths.get("src/test/resources/LICENSE.zip").toFile().getBytes()
    }

    Map getArtifactResponseData(Map mixins = [:]) {
        def result = [
                status: 200,
                body: getExampleFileBytes(),
        ]

        return result << mixins
    }


    def "Tests downloadComponentPDF"() {
        given: "An url"
        String repo = "leva-documentation"
        String jiraProjectKey = "ordgp-DEV"

        def request = getArtifactRequestData(repo, jiraProjectKey, artifactName)
        def response = getArtifactResponseData()
        def server = createServer(WireMock.&get, request, response)
        def nexusService = createNexusService(server.port(), request.username, request.password)
        temporaryFolder.create()
        String extractionPath = temporaryFolder.getRoot().getAbsolutePath()

        String url = "http://localhost:" + server.port() + "/repository/" +
                request.data.repository + "/" + request.data.directory + "/" + request.data.name

        def docGenUseCase = new LeVADocumentService(project, util, docGen, jiraUseCase, junit, levaFiles, nexusService, pdf, sq, bbt)

        when: "execute"

        docGenUseCase.downloadComponentPDF(jiraProjectKey, "", extractionPath, artifactName as String)

        then: "downloads and unzips"
        Paths.get(temporaryFolder.getRoot().getAbsolutePath(), "LICENSE").toFile().exists()

        where:
        artifactName << [ "CFTP-ORDGP-DEV-2022-01-22_23-59-59.zip", "CSD-ORDGP-DEV-2022-01-22_23-59-59.zip",
                "DIL-ORDGP-DEV-2022-01-22_23-59-59.zip", "DTP-ORDGP-DEV-2022-01-22_23-59-59.zip",
                "IVP-ORDGP-DEV-2022-01-22_23-59-59.zip", "RA-ORDGP-DEV-2022-01-22_23-59-59.zip",
                "TCP-ORDGP-DEV-2022-01-22_23-59-59.zip", "TIP-ORDGP-DEV-2022-01-22_23-59-59.zip",
                "TRC-ORDGP-DEV-2022-01-22_23-59-59.zip",
        ]
    }
}
