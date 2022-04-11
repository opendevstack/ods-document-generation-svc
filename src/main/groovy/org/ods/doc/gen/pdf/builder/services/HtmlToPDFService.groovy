package org.ods.doc.gen.pdf.builder.services

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.io.FileTemplateLoader
import groovy.util.logging.Slf4j
import org.apache.pdfbox.io.MemoryUsageSetting
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDDocumentNameDestinationDictionary
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.common.PDNameTreeNode
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.nio.file.Path
import java.nio.file.Paths

@Slf4j
@Service
class HtmlToPDFService {

    private final WkhtmltopdfService wkhtmltopdfService

    @Inject
    HtmlToPDFService(WkhtmltopdfService wkhtmltopdfService){
        this.wkhtmltopdfService = wkhtmltopdfService
    }

    String executeTemplate(Path path, Object data) {
        def loader = new FileTemplateLoader("", "")
        return new Handlebars(loader).compile(path.toString()).apply(data)
    }

    Path convert(Path tmpDir, Path documentHtmlFile, Map data = null) {
        Path documentPDFFile = Paths.get(tmpDir.toString(), "document.pdf")
        def cmd = generateCmd(data)
        cmd << documentHtmlFile.toFile().absolutePath
        cmd << documentPDFFile.toFile().absolutePath
        wkhtmltopdfService.executeCmd(tmpDir, documentHtmlFile, cmd)
        fixDestinations(documentPDFFile.toFile())
        return documentPDFFile
    }

    private List<String> generateCmd(Map data) {
        def cmd = ["wkhtmltopdf", "--encoding", "UTF-8", "--no-outline", "--print-media-type"]
        cmd << "--enable-local-file-access"
        cmd.addAll(["-T", "40", "-R", "25", "-B", "25", "-L", "25"])

        if (data?.metadata?.header) {
            if (data.metadata.header.size() > 1) {
                cmd.addAll(["--header-center", """${data.metadata.header[0]}
${data.metadata.header[1]}"""])
            } else {
                cmd.addAll(["--header-center", data.metadata.header[0]])
            }

            cmd.addAll(["--header-font-size", "10", "--header-spacing", "10"])
        }

        cmd.addAll(["--footer-center", "'Page [page] of [topage]'", "--footer-font-size", "10"])

        if (data?.metadata?.orientation) {
            cmd.addAll(["--orientation", data.metadata.orientation])
        }

        return cmd
    }

    private static final long MAX_MEMORY_TO_FIX_DESTINATIONS = 8192L

    /**
     * Fixes malformed PDF documents which use page numbers in local destinations, referencing the same document.
     * Page numbers should be used only for references to external documents.
     * These local destinations must use indirect page object references.
     * Note that these malformed references are not correctly renumbered when merging documents.
     * This method finds these malformed references and replaces the page numbers by the corresponding
     * page object references.
     * If the document is not malformed, this method will leave it unchanged.
     *
     * @param pdf a PDF file.
     */
    private void fixDestinations(File pdf) {
        def memoryUsageSetting = MemoryUsageSetting.setupMixed(MAX_MEMORY_TO_FIX_DESTINATIONS)
        PDDocument.load(pdf, memoryUsageSetting).withCloseable { doc ->
            fixDestinations(doc)
            doc.save(pdf)
        }
    }

    /**
     * Fixes malformed PDF documents which use page numbers in local destinations, referencing the same document.
     * Page numbers should be used only for references to external documents.
     * These local destinations must use indirect page object references.
     * Note that these malformed references are not correctly renumbered when merging documents.
     * This method finds these malformed references and replaces the page numbers by the corresponding
     * page object references.
     * If the document is not malformed, this method will leave it unchanged.
     *
     * @param doc a PDF document.
     */
    private void fixDestinations(PDDocument doc) {
        def pages = doc.pages as List // Accessing pages by index is slow. This will make it fast.
        fixExplicitDestinations(pages)
        def catalog = doc.documentCatalog
        fixNamedDestinations(catalog, pages)
        fixOutline(catalog, pages)
    }

    private fixExplicitDestinations(pages) {
        pages.each { page ->
            page.getAnnotations { it instanceof PDAnnotationLink }.each { link ->
                fixDestinationOrAction(link, pages)
            }
        }
    }

    private fixNamedDestinations(catalog, pages) {
        fixStringDestinations(catalog.names?.dests, pages)
        fixNameDestinations(catalog.dests, pages)
    }

    private fixOutline(catalog, pages) {
        def outline = catalog.documentOutline
        if (outline != null) {
            fixOutlineNode(outline, pages)
        }
    }

    private fixStringDestinations(PDNameTreeNode<PDPageDestination> node, pages) {
        if (node) {
            node.names?.each { name, dest -> fixDestination(dest, pages) }
            node.kids?.each { fixStringDestinations(it, pages) }
        }
    }

    private fixNameDestinations(PDDocumentNameDestinationDictionary dests, pages) {
        dests?.COSObject?.keySet()*.name.each { name ->
            def dest = dests.getDestination(name)
            if (dest instanceof PDPageDestination) {
                fixDestination(dest, pages)
            }
        }
    }

    private fixOutlineNode(PDOutlineNode node, pages) {
        node.children().each { item ->
            fixDestinationOrAction(item, pages)
            fixOutlineNode(item, pages)
        }
    }

    private fixDestinationOrAction(item, pages) {
        def dest = item.destination
        if (dest == null) {
            def action = item.action
            if (action instanceof PDActionGoTo) {
                dest = action.destination
            }
        }
        if (dest instanceof PDPageDestination) {
            fixDestination(dest, pages)
        }
    }

    private fixDestination(PDPageDestination dest, List<PDPage> pages) {
        def pageNum = dest.pageNumber
        if (pageNum != -1) {
            dest.setPage(pages[pageNum])
        }
    }

}
