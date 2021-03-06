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
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfString;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.element.List;
import com.itextpdf.layout.property.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EinsatzPDF {
    private final Config config;
    protected PdfFont bold;

    public EinsatzPDF(Config config) { this.config = config; }

    public void saveEinsatz(
            String fileName,
            String einsatzID,
            JSONObject einsatz,
            JSONArray disponierteFF,
            String einsatzAdresse,
            DirectionsRoute route,
            byte[] einsatzMap)
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

            Table headerTable = new Table(UnitValue.createPercentArray(new float[] {3, 1})).useAllAvailableWidth();
            Cell cell = new Cell();
            cell.add(new Paragraph("FF Pitten Einsatzplan - " + einsatzID));
            cell.setFontSize(22);
            cell.setTextAlignment(TextAlignment.CENTER);
            cell.setBorder(Border.NO_BORDER);
            cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
            headerTable.addCell(cell);

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
                    logoImage.setHorizontalAlignment(HorizontalAlignment.RIGHT);
                    cell = new Cell();
                    cell.add(logoImage);
                    cell.setHorizontalAlignment(HorizontalAlignment.RIGHT);
                    cell.setBorder(Border.NO_BORDER);
                    headerTable.addCell(cell);
                }
            } else {
                cell = new Cell();
                cell.add(new Paragraph(""));
                cell.setBorder(Border.NO_BORDER);
                headerTable.addCell(cell);
            }
            document.add(headerTable);
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            // Einsatz-ID
            Table table = new Table(UnitValue.createPercentArray(new float[] { 1, 3})).useAllAvailableWidth();
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

            cell = new Cell();
            cell.add(new Paragraph(einsatzAdresse));
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
                Paragraph melder = new Paragraph(einsatz.getString("Melder"));
                if (einsatz.has("MelderTelefon") && !einsatz.getString("MelderTelefon").equals("")) {
                    melder.add(" (" + einsatz.getString("MelderTelefon") + ")");
                }
                cell.add(melder);
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
            // Dispositionen
            if (disponierteFF != null) {
                cell = new Cell();
                cell.add(new Paragraph("Alarmierte Feuerwehren:"));
                cell.setFont(bold);
                cell.setBorder(Border.NO_BORDER);
                cell.setHorizontalAlignment(HorizontalAlignment.RIGHT);
                table.addCell(cell);

                cell = new Cell();
                List dispoList = new List();
                Calendar myCal = Calendar.getInstance();
                String today = String.format(("%tY-%<tm-%<tdT"), myCal);

                for (int i = 0; i < disponierteFF.length(); i++) {
                    JSONObject currentDispo = disponierteFF.getJSONObject(i);
                    if (currentDispo.has("EinTime")) {
                        // Ignore already returned ones
                        continue;
                    }
                    Paragraph dispoPara = new Paragraph();
                    dispoPara.add(currentDispo.getString("Name"));
                    dispoPara.add(" (Dispo: ");
                    dispoPara.add(currentDispo.getString("DispoTime")
                            .replace(today, "")
                            .replace('T', ' ')
                    );
                    if (currentDispo.has("AlarmTime")) {
                        dispoPara.add(", Alarm: ");
                        dispoPara.add(currentDispo.getString("AlarmTime")
                                .replace(today, "")
                                .replace('T', ' ')
                        );
                    }
                    if (currentDispo.has("AusTime")) {
                        dispoPara.add(", Aus: ");
                        dispoPara.add(currentDispo.getString("AusTime")
                                .replace(today, "")
                                .replace('T', ' ')
                        );
                    }
                    dispoPara.add(")");

                    ListItem item = new ListItem();
                    if (config.has("FeuerwehrName") &&
                            currentDispo.getString("Name").equals(config.getString("FeuerwehrName"))) {
                        dispoPara.setBold();
                    }
                    item.add(dispoPara);
                    dispoList.add(item);
                }
                cell.add(dispoList);
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

            if (route != null) {
                // Add Map image
                ImageData imageData = ImageDataFactory.create(einsatzMap);
                Image mapImage = new Image(imageData);
                mapImage.setAutoScale(true);
                mapImage.setHorizontalAlignment(HorizontalAlignment.CENTER);
                document.add(mapImage);
            }

            if (totalDistance >= 10000) {
                document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
                document.add(headerTable);
                document.add(new Paragraph(" "));
                document.add(new Paragraph(" "));

                Table routeTable = new Table(UnitValue.createPercentArray(new float[] {5, 1})).useAllAvailableWidth();

                for (DirectionsLeg routeLeg : route.legs) {
                    for (DirectionsStep step : routeLeg.steps) {
                        cell = new Cell();
                        Paragraph instructions = new Paragraph();
                        Pattern patter = Pattern.compile("(<[^>]+>)");
                        Matcher matcher = patter.matcher(step.htmlInstructions);

                        int pos = 0;
                        boolean isBold = false;
                        boolean lastCharIsSpace = false;
                        int boldStart = 0;
                        while (matcher.find()) {
                            if (pos < matcher.start() && !isBold) {
                                if (!lastCharIsSpace && !step.htmlInstructions.startsWith(")", pos)) {
                                    instructions.add(new Text(" "));
                                }
                                instructions.add(new Text(step.htmlInstructions.substring(pos, matcher.start())));
                                pos = matcher.start();
                                lastCharIsSpace = checkIfLastCharIsSpace(step.htmlInstructions, pos);
                            }

                            String marker = matcher.group();
                            if (marker.equals("<b>")) {
                                isBold = true;
                                boldStart = matcher.end();
                                pos = matcher.end();
                            } else if (marker.equals("</b>")) {
                                if (!lastCharIsSpace && !step.htmlInstructions.startsWith(")", pos)) {
                                    instructions.add(new Text(" "));
                                }
                                Text boldText = new Text(step.htmlInstructions.substring(boldStart, matcher.start()));
                                boldText.setFont(bold);
                                instructions.add(boldText);
                                pos = matcher.end();
                                lastCharIsSpace = checkIfLastCharIsSpace(step.htmlInstructions, pos);
                                isBold = false;
                                boldStart = 0;
                            } else {
                                pos = matcher.end();
                            }
                        }

                        if (pos < step.htmlInstructions.length()) {
                            if (!lastCharIsSpace && !step.htmlInstructions.startsWith(")", pos)) {
                                instructions.add(new Text(" "));
                            }
                            instructions.add(new Text(step.htmlInstructions.substring(pos)));
                        }

                        cell.add(instructions);
                        cell.setBorder(Border.NO_BORDER);
                        routeTable.addCell(cell);

                        cell = new Cell();
                        cell.add(new Paragraph(step.distance.humanReadable)
                                .setHorizontalAlignment(HorizontalAlignment.RIGHT)
                        );
                        cell.setBorder(Border.NO_BORDER);
                        cell.setHorizontalAlignment(HorizontalAlignment.RIGHT);
                        routeTable.addCell(cell);
                    }

                }

                document.add(routeTable);
            }

            document.close();
            pdfDocument.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void addEmptyLine(Paragraph paragraph, int number) {
        for (int i = 0; i < number; i++) {
            paragraph.add(new Paragraph(" "));
        }
    }

    private static boolean checkIfLastCharIsSpace(String string, int pos) {
        return string.startsWith(" ", pos - 1) ||
               string.startsWith("(", pos - 1) ||
               string.startsWith(")", pos - 1);
    }
}
