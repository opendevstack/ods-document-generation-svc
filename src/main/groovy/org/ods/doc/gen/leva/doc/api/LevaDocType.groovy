package org.ods.doc.gen.leva.doc.api

import org.ods.doc.gen.leva.doc.services.DocumentHistoryEntry
import org.ods.doc.gen.leva.doc.services.LeVADocumentService

import java.util.function.BiFunction

enum LevaDocType {

    CSD(LeVADocumentService::createCSD),
    DIL(LeVADocumentService::createDIL),
    DTP(LeVADocumentService::createDTP),
    DTR(LeVADocumentService::createDTR),
    RA(LeVADocumentService::createRA),
    CFTP(LeVADocumentService::createCFTP),
    CFTR(LeVADocumentService::createCFTR),
    IVP(LeVADocumentService::createIVP),
    IVR(LeVADocumentService::createIVR),
    SSDS(LeVADocumentService::createSSDS),
    TCP(LeVADocumentService::createTCP),
    TCR(LeVADocumentService::createTCR),
    TIP(LeVADocumentService::createTIP),
    TIR(LeVADocumentService::createTIR),
    TRC(LeVADocumentService::createTRC),
    OVERALL_DTR(LeVADocumentService::createOverallDTR),
    OVERALL_TIR(LeVADocumentService::createOverallTIR)

    public final BiFunction<LeVADocumentService, Map, List<DocumentHistoryEntry> > buildDocument;

    private LevaDocType(BiFunction<LeVADocumentService, Map, List<DocumentHistoryEntry>> buildDocument) {
        this.buildDocument = buildDocument;
    }
}