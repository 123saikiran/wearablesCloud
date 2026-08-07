package com.wearablescloud.apidocgen.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.wearablescloud.apidocgen.exception.SpecParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Markdown -> HTML (flexmark) -> PDF (openhtmltopdf) pipeline. openhtmltopdf requires
 * well-formed XHTML, so flexmark's HTML output is normalized through Jsoup first.
 */
@Service
public class PdfExportService {

    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;

    public PdfExportService() {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.of(TablesExtension.create()));
        this.markdownParser = Parser.builder(options).build();
        this.htmlRenderer = HtmlRenderer.builder(options).build();
    }

    public byte[] toPdf(String markdown) {
        Node document = markdownParser.parse(markdown);
        String bodyHtml = htmlRenderer.render(document);

        String fullHtml = """
                <html>
                <head>
                <style>
                  body { font-family: sans-serif; font-size: 11px; line-height: 1.4; }
                  h1, h2, h3 { color: #1a1a2e; }
                  code, pre { font-family: monospace; background: #f4f4f4; }
                  pre { padding: 8px; white-space: pre-wrap; }
                  table { border-collapse: collapse; width: 100%%; }
                  th, td { border: 1px solid #ccc; padding: 4px 8px; }
                </style>
                </head>
                <body>
                %s
                </body>
                </html>
                """.formatted(bodyHtml);

        String xhtml = toXhtml(fullHtml);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(xhtml, null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new SpecParseException("Failed to render PDF: " + e.getMessage(), e);
        }
    }

    private String toXhtml(String html) {
        Document document = Jsoup.parse(html);
        document.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.xhtml)
                .prettyPrint(false);
        return document.html();
    }
}
