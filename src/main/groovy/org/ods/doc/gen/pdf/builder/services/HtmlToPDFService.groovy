package org.ods.doc.gen.pdf.builder.services

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.io.FileTemplateLoader
import groovy.util.logging.Slf4j
import org.apache.commons.io.output.TeeOutputStream
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import org.ods.doc.gen.pdf.builder.util.OSService
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.nio.file.Files
import java.nio.file.Path

@Slf4j
@Service
class HtmlToPDFService {

    @Inject
    private OSService OSService;

    String executeTemplate(Path path, Object data) {
        def loader = new FileTemplateLoader("", "")
        return new Handlebars(loader).compile(path.toString()).apply(data)
    }

    Path convert(Path documentHtmlFile, Map data = null) {
        Path documentPDFFile = Files.createTempFile("document", ".pdf")
        List cmd = generateCmd(data, documentHtmlFile, documentPDFFile)
        executeCmd(documentHtmlFile, cmd)
        fixDestinations(documentPDFFile.toFile())
        return documentPDFFile
    }

    private List<String> generateCmd(Map data, Path documentHtmlFile, Path documentPDFFile) {
        def cmd = [getServiceName(), "--encoding", "UTF-8", "--no-outline", "--print-media-type"]
        cmd << "--enable-local-file-access"
        cmd.addAll(["-T", "40", "-R", "25", "-B", "25", "-L", "25"])
        cmd.addAll(controlSize())
        cmd.addAll(addHeader(data))
        cmd.addAll(["--footer-center", "'Page [page] of [topage]'", "--footer-font-size", "10", "--footer-font-name", "Arial"])
        setOrientation(data, cmd)
        cmd << documentHtmlFile.toFile().absolutePath
        cmd << documentPDFFile.toFile().absolutePath
        return cmd
    }

    private List controlSize(){
        return ["--dpi", "75",
                "--image-dpi", "600",
                "--minimum-font-size", "10"]
    }

    private String getServiceName() {
        return "wkhtmltopdf" + OSService.getOSApplicationsExtension();
    }

    private void setOrientation(Map data, ArrayList<String> cmd) {
        if (data?.metadata?.orientation) {
            cmd.addAll(["--orientation", data.metadata.orientation])
        }
    }

    private List<String> addHeader(Map data) {
        List<String> cmd = []
        if (data?.metadata?.header) {
            if (data.metadata.header.size() > 1) {
                cmd.addAll(["--header-center", "${data.metadata.header[0]}\n${data.metadata.header[1]}"])
            } else {
                cmd.addAll(["--header-center", data.metadata.header[0]] as String)
            }
            cmd.addAll(["--header-font-size", "10", "--header-spacing", "10", "--header-font-name", "Arial"])
        }
        return cmd
    }

    private void executeCmd(documentHtmlFile, List<String> cmd) {
        log.info "executing cmd: ${cmd}"
        def proc = cmd.execute()
        Path tempFilePath = Files.createTempFile("shell", ".bin")
        File tempFile = tempFilePath.toFile()
        FileOutputStream tempFileOutputStream = new FileOutputStream(tempFile)
        def errOutputStream = new TeeOutputStream(tempFileOutputStream, System.err)
        try {
            proc.waitForProcessOutput(System.out, errOutputStream)
        } finally {
            tempFileOutputStream.close()
        }

        if (proc.exitValue() != 0) {
            String errorDesc =   "${documentHtmlFile} failed: code:${proc.exitValue()}\r Description:${tempFile.text}"
            log.error errorDesc
            throw new IllegalStateException(errorDesc)
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
     * @param file a PDF file.
     */
    private void fixDestinations(File file) {
        def doc = PDDocument.load(file)
        fixDestinations(doc)
        doc.save(file)
        doc.close()
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
    private  void fixDestinations(PDDocument doc) {
        def pages = doc.pages as List // Accessing pages by index is slow. This will make it fast.
        def catalog = doc.documentCatalog
        fixNamedDestinations(catalog, pages)
        fixOutline(catalog, pages)
        fixExplicitDestinations(pages)
    }

    private  fixNamedDestinations(catalog, pages) {
        fixStringDestinations(catalog.names?.dests, pages)
        fixNameDestinations(catalog.dests, pages)
    }

    private  fixStringDestinations(node, pages) {
        if (node) {
            node.names?.each { name, dest -> fixDestination(dest, pages) }
            node.kids?.each { fixStringDestinations(it, pages) }
        }
    }

    private  fixNameDestinations(dests, pages) {
        dests?.COSObject?.keySet()*.name.each { name ->
            def dest = dests.getDestination(name)
            if (dest in PDPageDestination) {
                fixDestination(dest, pages)
            }
        }
    }

    private  fixOutline(catalog, pages) {
        def outline = catalog.documentOutline
        if (outline != null) {
            fixOutlineNode(outline, pages)
        }
    }

    private  fixOutlineNode(node, pages) {
        node.children().each { item ->
            fixDestinationOrAction(item, pages)
            fixOutlineNode(item, pages)
        }
    }

    private  fixExplicitDestinations(pages) {
        pages.each { page ->
            page.getAnnotations { it.subtype == PDAnnotationLink.SUB_TYPE }.each { link ->
                fixDestinationOrAction(link, pages)
            }
        }
    }

    private  fixDestinationOrAction(item, pages) {
        def dest = item.destination
        if (dest == null && item.action?.subType == PDActionGoTo.SUB_TYPE) {
            dest = item.action.destination
        }
        if (dest in PDPageDestination) {
            fixDestination(dest, pages)
        }
    }

    private  fixDestination(dest, pages) {
        def pageNum = dest.pageNumber
        if (pageNum != -1) {
            dest.setPage(pages[pageNum])
        }
    }
}
