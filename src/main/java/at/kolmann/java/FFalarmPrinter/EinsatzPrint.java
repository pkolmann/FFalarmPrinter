package at.kolmann.java.FFalarmPrinter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;

public class EinsatzPrint {
    private final Config config;
    private final LastEinsatzStore lastEinsatzStore;

    public EinsatzPrint(Config config, LastEinsatzStore lastEinsatzStore) {
        this.config = config;
        this.lastEinsatzStore = lastEinsatzStore;
    }

    public void process(String einsatzID, JSONObject einsatz, String filePath) throws IOException, PrinterException {
        // Don't print, if this Einsatz has been processed before
        if (lastEinsatzStore.contains(einsatzID)) {
            return;
        }

        String myPrinterName = config.getString("printerName");
        if (myPrinterName != null && myPrinterName.toLowerCase().equals("none")) {
            // No printing wanted!
            return;
        }

        // https://stackoverflow.com/a/18962278
        // https://bfo.com/blog/2012/02/15/using_java_to_print_pdf_documents/
        DocFlavor flavor = DocFlavor.SERVICE_FORMATTED.PAGEABLE;
        PrintRequestAttributeSet patts = new HashPrintRequestAttributeSet();
        PrintService[] ps = PrintServiceLookup.lookupPrintServices(flavor, patts);
        if (ps.length == 0) {
            throw new PrinterException("No suitable printer found");
        }

        if (myPrinterName == null) {
            System.out.println("Available printers: ");

            for (PrintService printer : ps) {
                System.out.println("  * " + printer.getName());
            }
            System.out.println();
            System.out.println("Specify 'printerName' in your config file to get automatic printouts.");
            System.out.println();
            System.out.println("If you don't want to see this list, specify printer 'none'.");
            System.out.println();

            // No printer specified, ignoring the print request
            return;
        }

        PrintService myPrinter = null;
        for (PrintService printService : ps) {
            if (printService.getName().toLowerCase().contains(myPrinterName.toLowerCase())) {
                myPrinter = printService;
                break;
            }
        }

        if (myPrinter == null) {
            throw new PrinterException("Printer not found");
        }

        if (config.has("printerIgnores")) {
            JSONArray ignores = (JSONArray) config.get("printerIgnores");
            for (int i = 0; i < ignores.length(); i++) {
                if (
                        einsatz.has("Alarmstufe")
                                && einsatz.getString("Alarmstufe").contains((String) ignores.get(i))
                ) {
                    System.out.println("Ignoring printing because 'Alarmstufe' contains '" + ignores.get(i) + "'");
                    return;
                }

                if (
                        einsatz.has("Meldebild")
                                && einsatz.getString("Meldebild").contains((String) ignores.get(i))
                ) {
                    System.out.println("Ignoring printing because 'Meldebild' contains '" + ignores.get(i) + "'");
                    return;
                }
            }
        }

        System.out.println("Printing PDF " + filePath + " to " + myPrinter.getName());
        PDDocument pdf = PDDocument.load(new File(filePath));
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintService(myPrinter);
        job.setPageable(new PDFPageable(pdf));
        job.print();
        pdf.close();
    }
}
