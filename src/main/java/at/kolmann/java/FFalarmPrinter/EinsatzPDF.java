package at.kolmann.java.FFalarmPrinter;

import com.google.maps.ImageResult;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfDocument;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class EinsatzPDF {
    private static Font catFont = new Font(Font.FontFamily.TIMES_ROMAN, 18, Font.BOLD);
    private static Font redFont = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.NORMAL, BaseColor.RED);
    private static Font subFont = new Font(Font.FontFamily.TIMES_ROMAN, 16, Font.BOLD);
    private static Font smallBold = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD);
    private static Font small = new Font(Font.FontFamily.TIMES_ROMAN, 12);

    public void saveEinsatzPDF(
            String fileName,
            String einsatzID,
            JSONObject einsatz,
            DirectionsRoute route,
            ImageResult einsatzMap)
    {
        try {
            System.out.println("Saving PDF to " + fileName);
            // https://www.vogella.com/tutorials/JavaPDF/article.html
            // https://www.mikesdotnetting.com/article/82/itextsharp-adding-text-with-chunks-phrases-and-paragraphs
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(fileName));
            document.open();

            // Add Metadata
            document.addTitle("FF Alarm " + einsatzID);

            Paragraph paragraph = new Paragraph();
            addEmptyLine(paragraph, 1);
            Paragraph title = new Paragraph("FF Pitten Einsatzplan - " + einsatzID, catFont);
            title.setAlignment(Element.ALIGN_CENTER);
            paragraph.add(title);
            addEmptyLine(paragraph, 4);
            document.add(paragraph);

            // Einsatz-ID
            PdfPTable table = new PdfPTable(new float[] { 1, 3});
            PdfPCell cell = new PdfPCell(new Paragraph("Einsatz-ID:", smallBold));
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);
            cell = new PdfPCell(new Paragraph(einsatzID, small));
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);

            // Alarmstufe
            if (einsatz.getString("Alarmstufe") != null) {
                cell = new PdfPCell(new Paragraph("Alarmstufe:", smallBold));
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);

                cell = new PdfPCell(new Paragraph(einsatz.getString("Alarmstufe"), small));
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
            }

            // Meldebild
            if (einsatz.getString("Meldebild") != null) {
                cell = new PdfPCell(new Paragraph("Meldebild:", smallBold));
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
                cell = new PdfPCell(new Paragraph(einsatz.getString("Meldebild"), small));
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
            }

            // Einsatzadresse
            cell = new PdfPCell(new Paragraph("Adresse:", smallBold));
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);

            StringBuilder einsatzAdresse1 = new StringBuilder();
            StringBuilder einsatzAdresse2 = new StringBuilder();
            if (einsatz.has("Strasse")) {
                einsatzAdresse1.append(einsatz.getString("Strasse"));
                if (einsatz.has("Nummer1")) {
                    einsatzAdresse1.append(" ");
                    einsatzAdresse1.append(einsatz.getString("Nummer1"));
                }
            }
            if (einsatz.has("Plz")) {
                einsatzAdresse2.append(einsatz.getString("Plz"));
                if (einsatz.has("Ort")) {
                    einsatzAdresse2.append(" ");
                    einsatzAdresse2.append(einsatz.getString("Ort"));
                }
            }

            cell = null;
            if (einsatzAdresse1.length() > 0) {
                cell = new PdfPCell(new Paragraph(einsatzAdresse1.toString(), small));
            }
            if (einsatzAdresse2.length() > 0) {
                if (cell == null) {
                    cell = new PdfPCell(new Paragraph(einsatzAdresse2.toString(), small));
                } else {
                    cell.addElement(new Paragraph(einsatzAdresse2.toString(), small));
                }
            }
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);

            // Melder
            if (einsatz.getString("Melder") != null) {
                cell = new PdfPCell(new Paragraph("Melder:", smallBold));
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);

                cell = new PdfPCell(new Paragraph(einsatz.getString("Melder"), small));
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
            }

            // Bemerkung
            if (einsatz.getString("Bemerkung") != null) {
                cell = new PdfPCell(new Paragraph("Bemerkung:", smallBold));
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);

                cell = new PdfPCell(new Paragraph(einsatz.getString("Bemerkung"), small));
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
            }

            // Einsatzbeginn
            if (einsatz.getString("EinsatzErzeugt") != null) {
                cell = new PdfPCell(new Paragraph("Einsatzbeginn:", smallBold));
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);

                cell = new PdfPCell(new Paragraph(einsatz.getString("EinsatzErzeugt"), small));
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
            }

            // Wegstrecke
            if (einsatz.getString("Melder") != null) {
                cell = new PdfPCell(new Paragraph("Wegstrecke:", smallBold));
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);

                cell = new PdfPCell(new Paragraph(einsatz.getString("Melder"), small));
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
            }



            document.add(table);



            document.close();

            if (route != null) {
                System.out.println(route.summary);
                System.out.println(route.copyrights);
                System.out.println(route.toString());

                for (DirectionsLeg routeLeg : route.legs) {
                    System.out.println("Leg:");
                    System.out.println(routeLeg.toString());
                    System.out.println("Start: " + routeLeg.startAddress);
                    System.out.println("End: " + routeLeg.endAddress);
                    System.out.println("Distance: " + routeLeg.distance.humanReadable);

                    if (routeLeg.duration != null) {
                        System.out.println("duration: " + routeLeg.duration.humanReadable);
                    }
                    if (routeLeg.durationInTraffic != null) {
                        System.out.println("durationInTraffic: " + routeLeg.durationInTraffic.humanReadable);
                    }
                    System.out.println("");
                    System.out.println("Steps:");
                    for (DirectionsStep step : routeLeg.steps) {
                        System.out.println("    htmlInstructions: " + step.htmlInstructions);
                        System.out.println("    distance: " + step.distance.humanReadable);
                        System.out.println("    duration: " + step.duration.humanReadable);
                        System.out.println("    travelMode: " + step.travelMode.toString());

                        System.out.println("++++++");
                    }

                    System.out.println("######");
                }
                System.out.println("-----");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    private static void addEmptyLine(Paragraph paragraph, int number) {
        for (int i = 0; i < number; i++) {
            paragraph.add(new Paragraph(" "));
        }
    }
}
