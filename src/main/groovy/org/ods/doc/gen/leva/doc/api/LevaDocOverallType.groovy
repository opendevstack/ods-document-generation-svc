package org.ods.doc.gen.leva.doc.api

import org.ods.doc.gen.leva.doc.services.LeVADocumentService

import java.util.function.BiFunction

enum LevaDocOverallType {

    OVERALL_DTR(LeVADocumentService::createOverallDTR),
    OVERALL_TIR(LeVADocumentService::createOverallTIR)

    public final BiFunction<LeVADocumentService, Map, String> buildDocument;

    private LevaDocOverallType(BiFunction<LeVADocumentService, Map, String> buildDocument) {
        this.buildDocument = buildDocument;
    }
}