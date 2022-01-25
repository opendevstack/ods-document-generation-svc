package org.ods.shared.lib.git

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Service

@SuppressWarnings('MethodCount')
@Slf4j
@Service
class GitService {

    public final static String ODS_GIT_BRANCH_PREFIX = 'release/'

    static String getReleaseBranch(String version) {
        "${ODS_GIT_BRANCH_PREFIX}${version}"
    }

}
