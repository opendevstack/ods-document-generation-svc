package org.ods.shared.lib.jenkins

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Service

@Slf4j
@Service
class JenkinsService {

    String getCurrentBuildLogAsText () {
        // TODO use Nexus
        return "logs from CI"
    }

    boolean unstashFilesIntoPath(String name, String path, String type) {
        // TODO use Nexus
        return true
    }
}
