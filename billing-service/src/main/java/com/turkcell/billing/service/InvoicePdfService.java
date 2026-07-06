package com.turkcell.billing.service;

import com.turkcell.billing.dto.response.InvoiceLineResponse;
import com.turkcell.billing.dto.response.InvoiceResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * FR-23: fatura PDF olarak uretilir. Gercek bir sablon motoru yerine PDFBox ile basit, duz metin
 * tabanli bir duzen kullanilir - MVP'nin amaci gercek bir fatura tasarim sistemi degil, PDF uretim
 * yetkinligini kanitlamak.
 */
@Service
public class InvoicePdfService {

    private static final float MARGIN = 50f;
    private static final float LINE_HEIGHT = 18f;

    public byte[] generate(InvoiceResponse invoice) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = page.getMediaBox().getHeight() - MARGIN;
                PDType1Font titleFont = PDType1Font.HELVETICA_BOLD;
                PDType1Font bodyFont = PDType1Font.HELVETICA;

                y = writeLine(content, titleFont, 16, MARGIN, y, "TelcoX - Invoice");
                y -= LINE_HEIGHT / 2;
                y = writeLine(content, bodyFont, 10, MARGIN, y, "Invoice Id: " + invoice.getId());
                y = writeLine(content, bodyFont, 10, MARGIN, y, "Customer Id: " + invoice.getCustomerId());
                y = writeLine(content, bodyFont, 10, MARGIN, y, "Period: " + invoice.getPeriodStart() + " - " + invoice.getPeriodEnd());
                y = writeLine(content, bodyFont, 10, MARGIN, y, "Due Date: " + invoice.getDueDate());
                y = writeLine(content, bodyFont, 10, MARGIN, y, "Status: " + invoice.getStatus());
                y -= LINE_HEIGHT;

                y = writeLine(content, titleFont, 11, MARGIN, y, "Line Items");
                if (invoice.getLines() != null) {
                    for (InvoiceLineResponse line : invoice.getLines()) {
                        String text = String.format("%-40s x%-6s @ %-10s = %s",
                                line.getDescription(), line.getQuantity(), line.getUnitPrice(), line.getLineTotal());
                        y = writeLine(content, bodyFont, 9, MARGIN, y, text);
                    }
                }

                y -= LINE_HEIGHT;
                y = writeLine(content, bodyFont, 10, MARGIN, y, "Sub Total: " + invoice.getSubTotal());
                y = writeLine(content, bodyFont, 10, MARGIN, y, "Tax: " + invoice.getTax());
                writeLine(content, titleFont, 12, MARGIN, y, "Grand Total: " + invoice.getGrandTotal());
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to generate invoice PDF", ex);
        }
    }

    private float writeLine(PDPageContentStream content, PDType1Font font, float fontSize,
                             float x, float y, String text) throws IOException {
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
        return y - LINE_HEIGHT;
    }
}
