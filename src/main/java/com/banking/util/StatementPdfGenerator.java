package com.banking.util;

import com.banking.dto.TransactionDtos;
import com.banking.enums.TransactionType;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@Component
public class StatementPdfGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy")
            .withZone(ZoneId.of("Asia/Kolkata"));
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")
            .withZone(ZoneId.of("Asia/Kolkata"));
    private static final Set<TransactionType> CREDIT_TYPES = Set.of(
            TransactionType.DEPOSIT, TransactionType.TRANSFER_CREDIT, TransactionType.LOAN_DISBURSEMENT);

    public byte[] generate(TransactionDtos.StatementResponse statement) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 42, 42);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new Color(11, 31, 58));
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(90, 105, 120));
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(11, 31, 58));
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(40, 50, 60));

            Paragraph brand = new Paragraph("NORTHSTAR BANK", titleFont);
            brand.setSpacingAfter(2);
            document.add(brand);
            document.add(new Paragraph(statement.bankName() + "  |  Account statement", subtitleFont));
            document.add(new Paragraph("Generated on " + DATE_TIME.format(java.time.Instant.now())
                    + "  |  This is a system-generated statement.", subtitleFont));

            Paragraph spacer = new Paragraph(" ");
            spacer.setSpacingAfter(14);
            document.add(spacer);

            PdfPTable meta = new PdfPTable(new float[]{1.2f, 2.2f, 1.2f, 2.2f});
            meta.setWidthPercentage(100);
            addMeta(meta, "Account holder", statement.accountHolder(), sectionFont, cellFont);
            addMeta(meta, "Account number", statement.accountNumber(), sectionFont, cellFont);
            addMeta(meta, "Statement period", DATE.format(statement.from()) + " - " + DATE.format(statement.to()), sectionFont, cellFont);
            addMeta(meta, "Opening balance", "INR " + money(statement.openingBalance()), sectionFont, cellFont);
            addMeta(meta, "Total credits", "INR " + money(statement.totalCredits()), sectionFont, cellFont);
            addMeta(meta, "Total debits", "INR " + money(statement.totalDebits()), sectionFont, cellFont);
            addMeta(meta, "Closing balance", "INR " + money(statement.closingBalance()), sectionFont, cellFont);
            addMeta(meta, "Transactions", String.valueOf(statement.transactions().size()), sectionFont, cellFont);
            document.add(meta);

            Paragraph spacer2 = new Paragraph(" ");
            spacer2.setSpacingAfter(14);
            document.add(spacer2);

            PdfPTable table = new PdfPTable(new float[]{1.25f, 1.65f, 1.35f, 2.2f, 1.1f, 1.1f, 1.2f});
            table.setWidthPercentage(100);
            table.setHeaderRows(1);
            String[] headers = {"Date", "Reference", "Type", "Description", "Debit", "Credit", "Balance"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new Color(11, 31, 58));
                cell.setPadding(7);
                cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                table.addCell(cell);
            }
            for (TransactionDtos.TransactionResponse t : statement.transactions()) {
                boolean credit = CREDIT_TYPES.contains(t.type());
                table.addCell(cell(DATE_TIME.format(t.createdAt()), cellFont));
                table.addCell(cell(t.reference(), cellFont));
                table.addCell(cell(t.type().name().replace('_', ' '), cellFont));
                table.addCell(cell(t.description() == null ? "-" : t.description(), cellFont));
                table.addCell(cell(credit ? "-" : money(t.amount()), cellFont));
                table.addCell(cell(credit ? money(t.amount()) : "-", cellFont));
                table.addCell(cell(money(t.balanceAfter()), cellFont));
            }
            document.add(table);

            Paragraph footer = new Paragraph(
                    "\nNorthstar Bank Ltd.  |  support@northstarbank.com  |  1800-123-4567\n"
                            + "This statement is generated from the banking management system and is valid without signature.",
                    subtitleFont);
            footer.setSpacingBefore(18);
            document.add(footer);
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate statement PDF", ex);
        }
    }

    private void addMeta(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(0);
        labelCell.setPadding(4);
        table.addCell(labelCell);
        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(0);
        valueCell.setPadding(4);
        table.addCell(valueCell);
    }

    private PdfPCell cell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        cell.setBorderColor(new Color(226, 232, 240));
        return cell;
    }

    private String money(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
