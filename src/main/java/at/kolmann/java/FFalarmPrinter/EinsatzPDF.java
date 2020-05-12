package at.kolmann.java.FFalarmPrinter;

import com.google.maps.ImageResult;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfString;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.layout.LayoutArea;
import com.itextpdf.layout.layout.LayoutContext;
import com.itextpdf.layout.layout.LayoutResult;
import com.itextpdf.layout.property.*;
import com.itextpdf.layout.renderer.IRenderer;
import org.json.JSONObject;

import javax.lang.model.element.Element;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import static com.google.maps.StaticMapsRequest.Markers.MarkersSize.small;

public class EinsatzPDF {
//    private static Font catFont = new Font(Font.FontFamily.TIMES_ROMAN, 18, Font.BOLD);
//    private static Font redFont = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.NORMAL, BaseColor.RED);
//    private static Font subFont = new Font(Font.FontFamily.TIMES_ROMAN, 16, Font.BOLD);
//    private static Font smallBold = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD);
//    private static Font small = new Font(Font.FontFamily.TIMES_ROMAN, 12);
    private Config config;
    protected PdfFont bold;

    public EinsatzPDF(Config config) { this.config = config; }

    public void saveEinsatzPDF(
            String fileName,
            String einsatzID,
            JSONObject einsatz,
            DirectionsRoute route,
            ImageResult einsatzMap)
    {
        try {
            bold = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
            System.out.println("Saving PDF to " + fileName);
            // https://www.vogella.com/tutorials/JavaPDF/article.html
            // https://www.mikesdotnetting.com/article/82/itextsharp-adding-text-with-chunks-phrases-and-paragraphs
            PdfDocument pdfDocument = new PdfDocument(new PdfWriter(fileName));
            pdfDocument.getCatalog().put(PdfName.Title, new PdfString("FF Alarm " + einsatzID));
            Document document = new Document(pdfDocument, new PageSize(PageSize.A4));
            document.setFont(PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN));
            document.setFontSize(12);

            Table table = new Table(UnitValue.createPercentArray(new float[] {3, 1})).useAllAvailableWidth();
            Cell cell = new Cell();
            cell.add(new Paragraph("FF Pitten Einsatzplan - " + einsatzID));
            cell.setFontSize(22);
            cell.setTextAlignment(TextAlignment.CENTER);
            cell.setBorder(Border.NO_BORDER);
            cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
            table.addCell(cell);

            if (config.getString("FeuerwehrLogo") != null) {
                String imageFileString = config.getString("FeuerwehrLogo");
                File imageFile = new File(imageFileString);
                if (!imageFile.isAbsolute()) {
                    imageFileString = System.getProperty("user.dir") + File.separator +
                            config.getString("FeuerwehrLogo");
                    imageFile = new File(imageFileString);
                }

                if (imageFile.exists()) {
                    ImageData imageData = ImageDataFactory.create(imageFileString);
                    Image logoImage = new Image(imageData);
                    logoImage.scaleAbsolute(50,50);
                    cell = new Cell();
                    cell.add(logoImage);
                    cell.setHorizontalAlignment(HorizontalAlignment.RIGHT);
                    cell.setBorder(Border.NO_BORDER);
                    table.addCell(cell);
                }
            } else {
                cell = new Cell();
                cell.add(new Paragraph(""));
                cell.setBorder(Border.NO_BORDER);
                table.addCell(cell);
            }
            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            // Einsatz-ID
            table = new Table(UnitValue.createPercentArray(new float[] { 1, 3})).useAllAvailableWidth();
            cell = new Cell();
            cell.add(new Paragraph("Einsatz-ID:"));
            cell.setFont(bold);
            cell.setBorder(Border.NO_BORDER);
            cell.setHorizontalAlignment(HorizontalAlignment.RIGHT);
            table.addCell(cell);
            cell = new Cell();
            cell.add(new Paragraph(einsatzID));
            cell.setBorder(Border.NO_BORDER);
            table.addCell(cell);

            // Alarmstufe
            if (einsatz.getString("Alarmstufe") != null) {
                cell = new Cell();
                cell.add(new Paragraph("Alarmstufe:"));
                cell.setFont(bold);
                cell.setBorder(Border.NO_BORDER);
                cell.setHorizontalAlignment(HorizontalAlignment.RIGHT);
                table.addCell(cell);

                cell = new Cell();
                cell.add(new Paragraph(einsatz.getString("Alarmstufe")));
                cell.setBorder(Border.NO_BORDER);
                table.addCell(cell);
            }

            // Meldebild
            if (einsatz.getString("Meldebild") != null) {
                cell = new Cell();
                cell.add(new Paragraph("Meldebild:"));
                cell.setFont(bold);
                cell.setBorder(Border.NO_BORDER);
                cell.setHorizontalAlignment(HorizontalAlignment.RIGHT);
                table.addCell(cell);
                cell = new Cell();
                cell.add(new Paragraph(einsatz.getString("Meldebild")));
                cell.setBorder(Border.NO_BORDER);
                table.addCell(cell);
            }

            // Einsatzadresse
            cell = new Cell();
            cell.add(new Paragraph("Adresse:"));
            cell.setFont(bold);
            cell.setBorder(Border.NO_BORDER);
            cell.setHorizontalAlignment(HorizontalAlignment.RIGHT);
            table.addCell(cell);

            StringBuilder einsatzAdresse = new StringBuilder();
            if (einsatz.has("Strasse")) {
                einsatzAdresse.append(einsatz.getString("Strasse"));
                if (einsatz.has("Nummer1")) {
                    einsatzAdresse.append(" ");
                    einsatzAdresse.append(einsatz.getString("Nummer1"));
                }
            }
            if (einsatz.has("Plz")) {
                einsatzAdresse.append(System.lineSeparator());
                einsatzAdresse.append(einsatz.getString("Plz"));
                if (einsatz.has("Ort")) {
                    einsatzAdresse.append(" ");
                    einsatzAdresse.append(einsatz.getString("Ort"));
                }
            }

            cell = new Cell();
            cell.add(new Paragraph(einsatzAdresse.toString()));
            cell.setBorder(Border.NO_BORDER);
            table.addCell(cell);

            // Melder
            if (einsatz.getString("Melder") != null) {
                cell = new Cell();
                cell.add(new Paragraph("Melder:"));
                cell.setFont(bold);
                cell.setBorder(Border.NO_BORDER);
                cell.setHorizontalAlignment(HorizontalAlignment.RIGHT);
                table.addCell(cell);

                cell = new Cell();
                cell.add(new Paragraph(einsatz.getString("Melder")));
                cell.setBorder(Border.NO_BORDER);
                table.addCell(cell);
            }

            // Bemerkung
            if (einsatz.getString("Bemerkung") != null) {
                cell = new Cell();
                cell.add(new Paragraph("Bemerkung:"));
                cell.setFont(bold);
                cell.setBorder(Border.NO_BORDER);
                cell.setHorizontalAlignment(HorizontalAlignment.RIGHT);
                table.addCell(cell);

                cell = new Cell();
                cell.add(new Paragraph(einsatz.getString("Bemerkung")));
                cell.setBorder(Border.NO_BORDER);
                table.addCell(cell);
            }

            // Einsatzbeginn
            if (einsatz.getString("EinsatzErzeugt") != null) {
                cell = new Cell();
                cell.add(new Paragraph("Einsatzbeginn:"));
                cell.setFont(bold);
                cell.setBorder(Border.NO_BORDER);
                cell.setHorizontalAlignment(HorizontalAlignment.RIGHT);
                table.addCell(cell);

                cell = new Cell();
                cell.add(new Paragraph(einsatz.getString("EinsatzErzeugt")
                        .replace('T', ' ')));
                cell.setBorder(Border.NO_BORDER);
                table.addCell(cell);
            }

            // Wegstrecke
            long totalDistance = 0;
            long totalDurtion = 0;

            if (route != null) {
                for (DirectionsLeg routeLeg : route.legs) {
                    totalDistance += routeLeg.distance.inMeters;
                    if (routeLeg.duration != null) {
                        totalDurtion += routeLeg.duration.inSeconds;
                    }
                }

                // Wegstrecke
                cell = new Cell();
                cell.add(new Paragraph("Wegstrecke:"));
                cell.setFont(bold);
                cell.setBorder(Border.NO_BORDER);
                cell.setHorizontalAlignment(HorizontalAlignment.RIGHT);
                table.addCell(cell);

                DecimalFormat df = new DecimalFormat("#.#");
                cell = new Cell();
                cell.add(new Paragraph(df.format(totalDistance / 1000.0f) + " km"));
                cell.setBorder(Border.NO_BORDER);
                table.addCell(cell);

                // Wegzeit
                cell = new Cell();
                cell.add(new Paragraph("Wegzeit:"));
                cell.setFont(bold);
                cell.setBorder(Border.NO_BORDER);
                cell.setHorizontalAlignment(HorizontalAlignment.RIGHT);
                table.addCell(cell);

                StringBuilder totals = new StringBuilder();
                if (totalDurtion < 60 ) {
                    totals.append("weniger als eine Mintute");
                } else if (totalDurtion < 3600) {
                    totals.append(String.format("etwa %d Minute", totalDurtion/60));
                    if (totalDurtion >= 120) {
                        totals.append("n");
                    }
                } else if (totalDistance >= 3600) {
                    totals.append(String.format("etwa %d Stunde", totalDurtion / 3600));
                    if (totalDurtion >= 7200) {
                        totals.append("n");
                    }
                    long minsRemaining = (totalDurtion - totalDurtion / 3600) / 60;

                    totals.append(String.format(" und %d Minute", minsRemaining));
                    if (minsRemaining > 1) {
                        totals.append("n");
                    }
                }
                cell = new Cell();
                cell.add(new Paragraph(totals.toString()));
                cell.setBorder(Border.NO_BORDER);
                table.addCell(cell);
            }
            document.add(table);

            Paragraph p = new Paragraph(" ");
            document.add(p);
            IRenderer pRenderer = p.createRendererSubTree().setParent(document.getRenderer());
            LayoutResult pLayoutResult = pRenderer.layout(new LayoutContext(new LayoutArea(0, new Rectangle(595-72, 842-72))));

            float y = pLayoutResult.getOccupiedArea().getBBox().getY();
            float x = pLayoutResult.getOccupiedArea().getBBox().getX();

            if (route != null) {
                // Add Map image
                ImageData imageData = ImageDataFactory.create(einsatzMap.imageData);
                Image mapImage = new Image(imageData);
                mapImage.setAutoScale(true);
                mapImage.setHorizontalAlignment(HorizontalAlignment.CENTER);
                document.add(mapImage);
            }

            if (totalDistance >= 10000) {
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

            document.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void addEmptyLine(Paragraph paragraph, int number) {
        for (int i = 0; i < number; i++) {
            paragraph.add(new Paragraph(" "));
        }
    }
}
