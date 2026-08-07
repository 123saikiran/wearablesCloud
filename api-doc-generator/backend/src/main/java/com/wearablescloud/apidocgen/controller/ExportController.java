package com.wearablescloud.apidocgen.controller;

import com.wearablescloud.apidocgen.dto.PostmanExportResponse;
import com.wearablescloud.apidocgen.exception.DocsNotGeneratedException;
import com.wearablescloud.apidocgen.model.GeneratedEndpointDoc;
import com.wearablescloud.apidocgen.model.ParsedSpec;
import com.wearablescloud.apidocgen.service.MarkdownRenderService;
import com.wearablescloud.apidocgen.service.PdfExportService;
import com.wearablescloud.apidocgen.service.PostmanCollectionResult;
import com.wearablescloud.apidocgen.service.PostmanCollectionService;
import com.wearablescloud.apidocgen.service.SpecRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/specs/{specId}/export")
public class ExportController {

    private final SpecRegistry specRegistry;
    private final MarkdownRenderService markdownRenderService;
    private final PdfExportService pdfExportService;
    private final PostmanCollectionService postmanCollectionService;

    public ExportController(SpecRegistry specRegistry,
                             MarkdownRenderService markdownRenderService,
                             PdfExportService pdfExportService,
                             PostmanCollectionService postmanCollectionService) {
        this.specRegistry = specRegistry;
        this.markdownRenderService = markdownRenderService;
        this.pdfExportService = pdfExportService;
        this.postmanCollectionService = postmanCollectionService;
    }

    @GetMapping("/markdown")
    public ResponseEntity<String> exportMarkdown(@PathVariable UUID specId) {
        String markdown = renderMarkdown(specId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/markdown"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"api-docs.md\"")
                .body(markdown);
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable UUID specId) {
        String markdown = renderMarkdown(specId);
        byte[] pdf = pdfExportService.toPdf(markdown);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"api-docs.pdf\"")
                .body(pdf);
    }

    @GetMapping("/postman")
    public PostmanExportResponse exportPostman(@PathVariable UUID specId) {
        ParsedSpec spec = specRegistry.get(specId);
        PostmanCollectionResult result = postmanCollectionService.build(spec);
        return new PostmanExportResponse(result.collection(), result.summary());
    }

    private String renderMarkdown(UUID specId) {
        ParsedSpec spec = specRegistry.get(specId);
        Map<String, GeneratedEndpointDoc> docs = specRegistry.getGeneratedDocs(specId);
        if (docs.size() < spec.endpoints().size()) {
            throw new DocsNotGeneratedException(
                    "Not all endpoints have generated documentation yet - call generate-all first");
        }
        String authExplanation = specRegistry.getAuthExplanationOrNull(specId);
        return markdownRenderService.render(spec, docs, authExplanation);
    }
}
