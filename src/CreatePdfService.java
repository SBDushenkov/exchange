package ru.dushenkov;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.io.IOException;
import java.io.OutputStream;

public class CreatePdfService {

    private static final int FONT_SIZE = 8;
    private static final float PAGE_MARGIN = 20f;
    private static final float MAX_CELL_WIDTH = 200f;
    private static final float CELL_MARGIN = 5f;

    void createPdf(Object[][] data, String header, OutputStream outputStream) {

        Font font = getDefaultFont();

        String[][] strData = toStringData(data);
        float[] colWidths = getColumnWidths(strData);

        PdfPTable table = new PdfPTable(colWidths.length);

        table.setTotalWidth(colWidths);
        table.setLockedWidth(true);

        for (int i = 0; i < strData.length; i++) {
            for (int j = 0; j < strData[i].length; j++) {
                PdfPCell cell = new PdfPCell(
                        new Phrase(strData[i][j], font)
                );
                if (colWidths[j] < MAX_CELL_WIDTH) {
                    cell.setNoWrap(true);
                }
                table.addCell(cell);
            }
        }

        table.setHorizontalAlignment(Element.ALIGN_LEFT);

        float tableWidth = 0;
        for (float w : colWidths) {
            tableWidth += w;
        }
        float pageWidth = tableWidth + 2 * PAGE_MARGIN;

        Rectangle rect;
        if (pageWidth < PageSize.A4.getWidth()) {
            rect = PageSize.A4;
        } else if (pageWidth < PageSize.A4.rotate().getWidth()) {
            rect = PageSize.A4.rotate();
        } else {
            rect = new Rectangle(pageWidth, pageWidth * PageSize.A4.getWidth() / PageSize.A4.getHeight());
        }

        Document document = new Document(rect, PAGE_MARGIN, PAGE_MARGIN, PAGE_MARGIN * 2, PAGE_MARGIN);
        PdfWriter pdfWriter = PdfWriter.getInstance(document, outputStream);
        pdfWriter.setPageEvent(new PageNumberEvent(header));
        document.open();

        document.add(table);
        document.close();
    }

    private String[][] toStringData(Object[][] data) {
        String[][] strData = new String[data.length][data[0].length];
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                strData[i][j] = String.valueOf(data[i][j]);
            }
        }
        return strData;
    }

    private float[] getColumnWidths(String[][] data) {
        float[] colWidths = new float[data[0].length];
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                float width = getDefaultFont().getBaseFont().getWidthPoint(data[i][j], FONT_SIZE) + CELL_MARGIN;
                if (width > colWidths[j]) {
                    colWidths[j] = width;
                }
            }
        }
        for (int j = 0; j < colWidths.length; j++) {
            if (colWidths[j] > MAX_CELL_WIDTH) {
                colWidths[j] = MAX_CELL_WIDTH;
            }
        }
        return colWidths;
    }

    private Font getDefaultFont() {
        String fontPath = getClass().getClassLoader().getResource("font/DejaVuSansMono.ttf").getPath();
        try {
            return new Font(
                    BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED),
                    FONT_SIZE
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private class PageNumberEvent extends PdfPageEventHelper {
        public String header;

        PageNumberEvent(String header) {
            this.header = header;
        }
        private PdfTemplate totalPageTemplate;

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            totalPageTemplate = writer.getDirectContent().createTemplate(30, 16);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            int pageNumber = writer.getPageNumber();
            String text = pageNumber + " /";
            float x = (document.right() + document.left()) / 2;
            float y = document.top() + 10;
            ColumnText.showTextAligned(
                    writer.getDirectContent(),
                    Element.ALIGN_RIGHT,
                    new Phrase(text, getDefaultFont()),
                    x, y, 0
            );
            ColumnText.showTextAligned(
                    writer.getDirectContent(),
                    Element.ALIGN_LEFT,
                    new Phrase(header, getDefaultFont()),
                    document.left(), y, 0
            );
            writer.getDirectContent().addTemplate(totalPageTemplate, x + 15, y);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            int totalPages = writer.getPageNumber() - 1;
            BaseFont baseFont = getDefaultFont().getBaseFont();
            totalPageTemplate.beginText();
            totalPageTemplate.setFontAndSize(baseFont, FONT_SIZE);
            totalPageTemplate.setTextMatrix(0, 0);
            totalPageTemplate.showText(String.valueOf(totalPages));
            totalPageTemplate.endText();
        }
    }
}

