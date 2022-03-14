package org.ods.doc.gen.pdf.builder.services

import com.github.benmanes.caffeine.cache.Cache
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.ods.doc.gen.pdf.builder.repository.DocumentTemplateFactory
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.nio.file.Path
import java.nio.file.Paths

@Service
class PdfGenerationService {

    private final Cache<String, Path> templatesCache
    private final HtmlToPDFService htmlToPDFService
    private final DocumentTemplateFactory documentTemplateFactory

    @Inject
    PdfGenerationService(HtmlToPDFService htmlToPDFService, DocumentTemplateFactory documentTemplateFactory) {
        this.htmlToPDFService = htmlToPDFService
        this.documentTemplateFactory = documentTemplateFactory
    }

    Path generatePdfFile(Map metadata, Map data, Path tmpDir) {
        copyTemplatesToTempFolder(metadata.version as String, tmpDir)
        Map<String, Path> partials = getPartialTemplates(metadata.type as String, tmpDir)
        Map<String, Path> partialsWithPathOk = generateHtmlFromTemplates(partials, data)
        return htmlToPDFService.convert(tmpDir, partialsWithPathOk.document, data)
    }

    private copyTemplatesToTempFolder(String version, Path tmpDir) {
        File templatesFolder = documentTemplateFactory.get().getTemplatesForVersion(version).toFile()
        FileUtils.copyDirectory(templatesFolder, tmpDir.toFile())
    }

    private Map<String, Path> generateHtmlFromTemplates(Map<String, Path> partials, data) {
        return partials.collectEntries { name, path ->
            def htmlFile = new File(FilenameUtils.removeExtension(path.toString()))
            htmlFile.setText(htmlToPDFService.executeTemplate(path, data))
            return [name, htmlFile.toPath()]
        }
    }

    private Map<String, Path> getPartialTemplates(String type, Path tmpDir) {
        return getPartialTemplates(type, tmpDir) { name, template ->
            return template.replaceAll(System.getProperty("line.separator"), "").replaceAll("\t", "")
        }
    }

    private Map<String, Path> getPartialTemplates(String type, Path tmpDir, Closure visitor) {
        def partials = [
            document: Paths.get(tmpDir.toString(), "templates", "${type}.html.tmpl"),
            header: Paths.get(tmpDir.toString(), "templates", "header.inc.html.tmpl"),
            footer: Paths.get(tmpDir.toString(), "templates", "footer.inc.html.tmpl")
        ]

        partials.each { name, path ->
            File file = getPartialTemplate(path, name)
            def template = file.text
            def templateNew = visitor(name, template)
            if (isTemplateModified(template, templateNew)) {
                file.text = templateNew
            }
        }
        return partials
    }

    private boolean isTemplateModified(String template, templateNew) {
        template != templateNew
    }

    private File getPartialTemplate(path, name) {
        def file = path.toFile()
        if (!file.exists()) {
            throw new FileNotFoundException("could not find required template part '${name}' at '${path}'")
        }
        return file
    }

}
