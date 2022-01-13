package org.ods.shared.lib.orchestration.util

import org.ods.shared.lib.orchestration.usecase.LeVADocumentUseCase
import org.ods.shared.lib.orchestration.usecase.leva.Constants

enum Environment {

    D, Q, P

    static String getEnvironment(documentType){
        return values().collect { it.toString() }.find { env ->
            ENVIRONMENT_TYPE[env].containsKey(documentType)
        }
    }

    static String getPreviousCreationEnvironment(String documentType, String environment) {
        def previousEnvironment = null
        values()*.toString()
                .takeWhile { it != environment }
                .each { env ->
                    if (ENVIRONMENT_TYPE[env].containsKey(documentType)) {
                        previousEnvironment = env
                    }
                }
        return previousEnvironment ?: environment
    }

    // Document types per environment token and label to track with Jira
    @SuppressWarnings('NonFinalPublicField')
    public final static Map ENVIRONMENT_TYPE = [
            "D": [
                    (Constants.DocumentType.CSD as String)        : ["${Constants.DocumentType.CSD}"],
                    (Constants.DocumentType.SSDS as String)       : ["${Constants.DocumentType.SSDS}"],
                    (Constants.DocumentType.RA as String)         : ["${Constants.DocumentType.RA}"],
                    (Constants.DocumentType.TIP as String)        : ["${Constants.DocumentType.TIP}_Q",
                                                                               "${Constants.DocumentType.TIP}_P"],
                    (Constants.DocumentType.TIR as String)        : ["${Constants.DocumentType.TIR}"],
                    (Constants.DocumentType.OVERALL_TIR as String): ["${Constants.DocumentType.TIR}"],
                    (Constants.DocumentType.IVP as String)        : ["${Constants.DocumentType.IVP}_Q",
                                                                               "${Constants.DocumentType.IVP}_P"],
                    (Constants.DocumentType.CFTP as String)       : ["${Constants.DocumentType.CFTP}"],
                    (Constants.DocumentType.TCP as String)        : ["${Constants.DocumentType.TCP}"],
                    (Constants.DocumentType.DTP as String)    : ["${Constants.DocumentType.DTP}"],
                    (Constants.DocumentType.DTR as String)    : ["${Constants.DocumentType.DTR}"],
                    (Constants.DocumentType.OVERALL_DTR as String)    : ["${Constants.DocumentType.DTR}"],
            ],
            "Q": [
                    (Constants.DocumentType.TIR as String)    : ["${Constants.DocumentType.TIR}_Q"],
                    (Constants.DocumentType.OVERALL_TIR as String)    : ["${Constants.DocumentType.TIR}_Q"],
                    (Constants.DocumentType.IVR as String)    : ["${Constants.DocumentType.IVR}_Q"],
                    (Constants.DocumentType.OVERALL_IVR as String)    : ["${Constants.DocumentType.IVR}_Q"],
                    (Constants.DocumentType.CFTR as String)   : ["${Constants.DocumentType.CFTR}"],
                    (Constants.DocumentType.TCR as String)    : ["${Constants.DocumentType.TCR}"],
                    (Constants.DocumentType.TRC as String)    : ["${Constants.DocumentType.TRC}"],
                    (Constants.DocumentType.DIL as String)    : ["${Constants.DocumentType.DIL}_Q"]
            ],
            "P": [
                    (Constants.DocumentType.TIR as String)    : ["${Constants.DocumentType.TIR}_P"],
                    (Constants.DocumentType.OVERALL_TIR as String)    : ["${Constants.DocumentType.TIR}_P"],
                    (Constants.DocumentType.IVR as String)    : ["${Constants.DocumentType.IVR}_P"],
                    (Constants.DocumentType.OVERALL_IVR as String)    : ["${Constants.DocumentType.IVR}_P"],
                    (Constants.DocumentType.DIL as String)    : ["${Constants.DocumentType.DIL}_P"]
            ],
    ]
}
