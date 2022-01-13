package org.ods.shared.lib.api

import org.ods.shared.lib.leva.doc.LeVADocumentUseCase

import java.util.function.BiFunction

enum LevaDocType {

    CSD(LeVADocumentUseCase::createCSD),
    DIL(LeVADocumentUseCase::createDIL),
    DTP(LeVADocumentUseCase::createDTP),
    DTR(LeVADocumentUseCase::createDTR),
    RA(LeVADocumentUseCase::createRA),
    CFTP(LeVADocumentUseCase::createCFTP),
    CFTR(LeVADocumentUseCase::createCFTR),
    IVP(LeVADocumentUseCase::createIVP),
    IVR(LeVADocumentUseCase::createIVR),
    SSDS(LeVADocumentUseCase::createSSDS),
    TCP(LeVADocumentUseCase::createTCP),
    TCR(LeVADocumentUseCase::createTCR),
    TIP(LeVADocumentUseCase::createTIP),
    TIR(LeVADocumentUseCase::createTIR),
    TRC(LeVADocumentUseCase::createTRC),
    OVERALL_DTR(LeVADocumentUseCase::createOverallDTR),
    OVERALL_TIR(LeVADocumentUseCase::createOverallTIR)

    public final BiFunction<LeVADocumentUseCase, Map, String> buildDocument;

    private LevaDocType(BiFunction<LeVADocumentUseCase, Map, String> buildDocument) {
        this.buildDocument = buildDocument;
    }
}