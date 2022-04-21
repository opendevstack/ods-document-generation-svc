package org.ods.doc.gen.pdf.builder.repository

import org.springframework.stereotype.Repository

import javax.inject.Inject

@Repository
class DocumentTemplateFactory {

    private final List<DocumentTemplatesRepository> docTemplates

    @Inject
    DocumentTemplateFactory(List<DocumentTemplatesRepository> docTempls){
        this.docTemplates = docTempls
    }

    DocumentTemplatesRepository get(){
        return docTemplates.get(0).isApplicableToSystemConfig() ? docTemplates.get(0) : docTemplates.get(1)
    }
}
