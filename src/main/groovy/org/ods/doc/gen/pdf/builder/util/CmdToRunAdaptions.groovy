package org.ods.doc.gen.pdf.builder.util

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Service

@Slf4j
@Service
class CmdToRunAdaptions {

    List<String> modify(String cmd) {
        return [ cmd ]
    }
}
