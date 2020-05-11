package at.kolmann.java.FFalarmPrinter;

import com.google.maps.ImageResult;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.Distance;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;

public class EinsatzPDF {
    private static Font catFont = new Font(Font.FontFamily.TIMES_ROMAN, 18, Font.BOLD);
    private static Font redFont = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.NORMAL, BaseColor.RED);
    private static Font subFont = new Font(Font.FontFamily.TIMES_ROMAN, 16, Font.BOLD);
    private static Font smallBold = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD);
    private static Font small = new Font(Font.FontFamily.TIMES_ROMAN, 12);
    private Config config;

    public EinsatzPDF(Config config) { this.config = config; }

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



            PdfPTable table = new PdfPTable(new float[] { 3, 1});
            PdfPCell cell = new PdfPCell(new Phrase("FF Pitten Einsatzplan - " + einsatzID, catFont));
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setVerticalAlignment(Element.ALIGN_JUSTIFIED_ALL);
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
                    Image logoImage = Image.getInstance(imageFileString);
                    logoImage.setAlignment(Element.ALIGN_RIGHT);
                    logoImage.scaleAbsolute(50,50);
                    cell = new PdfPCell(logoImage);
                    cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    cell.setBorder(Rectangle.NO_BORDER);
                    table.addCell(cell);
                }
            } else {
                cell = new PdfPCell(new Phrase(""));
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
            }
            document.add(table);

            // Einsatz-ID
            table = new PdfPTable(new float[] { 1, 3});
            cell = new PdfPCell(new Paragraph("Einsatz-ID:", smallBold));
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(cell);
            cell = new PdfPCell(new Paragraph(einsatzID, small));
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);

            // Alarmstufe
            if (einsatz.getString("Alarmstufe") != null) {
                cell = new PdfPCell(new Paragraph("Alarmstufe:", smallBold));
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(cell);

                cell = new PdfPCell(new Paragraph(einsatz.getString("Alarmstufe"), small));
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
            }

            // Meldebild
            if (einsatz.getString("Meldebild") != null) {
                cell = new PdfPCell(new Paragraph("Meldebild:", smallBold));
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(cell);
                cell = new PdfPCell(new Paragraph(einsatz.getString("Meldebild"), small));
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
            }

            // Einsatzadresse
            cell = new PdfPCell(new Paragraph("Adresse:", smallBold));
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
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

            cell = new PdfPCell(new Paragraph(einsatzAdresse.toString(), small));
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);

            // Melder
            if (einsatz.getString("Melder") != null) {
                cell = new PdfPCell(new Paragraph("Melder:", smallBold));
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(cell);

                cell = new PdfPCell(new Paragraph(einsatz.getString("Melder"), small));
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
            }

            // Bemerkung
            if (einsatz.getString("Bemerkung") != null) {
                cell = new PdfPCell(new Paragraph("Bemerkung:", smallBold));
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(cell);

                cell = new PdfPCell(new Paragraph(einsatz.getString("Bemerkung"), small));
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
            }

            // Einsatzbeginn
            if (einsatz.getString("EinsatzErzeugt") != null) {
                cell = new PdfPCell(new Paragraph("Einsatzbeginn:", smallBold));
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(cell);

                cell = new PdfPCell(new Paragraph(einsatz.getString("EinsatzErzeugt")
                        .replace('T', ' '), small));
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
            }

            // Wegstrecke
            if (route != null) {
                long totalDistance = 0;
                long totalDurtion = 0;

                for (DirectionsLeg routeLeg : route.legs) {
                    totalDistance += routeLeg.distance.inMeters;
                    if (routeLeg.duration != null) {
                        totalDurtion += routeLeg.duration.inSeconds;
                    }
                }

                // Wegstrecke
                cell = new PdfPCell(new Paragraph("Wegstrecke:", smallBold));
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(cell);

                DecimalFormat df = new DecimalFormat("#.#");
                cell = new PdfPCell(new Paragraph(df.format(totalDistance / 1000.0f) + " km", small));
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);

                // Wegzeit
                cell = new PdfPCell(new Paragraph("Wegzeit:", smallBold));
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
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
                cell = new PdfPCell(new Paragraph(totals.toString(), small));
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
            }
            document.add(table);

            document.add(new Paragraph(" "));

            // Add Map image
            Image mapImage = Image.getInstance(einsatzMap.imageData);
            System.out.println("Document width: " + document.getPageSize().getWidth());
            System.out.println("Image width: " + mapImage.getWidth());
            float scaleFactor = (document.getPageSize().getWidth() / mapImage.getWidth()) * 75;
            System.out.println("Scale factor: " + scaleFactor);
            mapImage.scalePercent(scaleFactor);
            mapImage.setAlignment(Element.ALIGN_CENTER);
            document.add(mapImage);

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
