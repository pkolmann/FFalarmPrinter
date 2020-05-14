package at.kolmann.java.FFalarmPrinter;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Sides;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

public class EinsatzPrint {
    private Config config;

    public EinsatzPrint(Config config) {
        this.config = config;
    }

    public void process(String filePath) throws IOException, PrintException {
        String myPrinterName = config.getString("printerName");
        if (myPrinterName == null) {
            // No printer specified, ignoring the print request
            return;
        }

        // https://stackoverflow.com/a/18962278
        DocFlavor flavor = DocFlavor.SERVICE_FORMATTED.PAGEABLE;
        PrintRequestAttributeSet patts = new HashPrintRequestAttributeSet();
        patts.add(Sides.DUPLEX);
        PrintService[] ps = PrintServiceLookup.lookupPrintServices(flavor, patts);
        if (ps.length == 0) {
            throw new IllegalStateException("No suitable printer found");
        }

        System.out.println("Available printers: " + Arrays.asList(ps));
        PrintService myPrinter = null;
        for (PrintService printService : ps) {
            if (printService.getName().contains(myPrinterName)) {
                myPrinter = printService;
                break;
            }
        }

        if (myPrinter == null) {
            throw new IllegalStateException("Printer not found");
        }

        System.out.println("Printing PDF to " + myPrinter.getName());
        FileInputStream fis = new FileInputStream(filePath);
        Doc pdfDoc = new SimpleDoc(fis, DocFlavor.INPUT_STREAM.AUTOSENSE, null);
        DocPrintJob printJob = myPrinter.createPrintJob();
        printJob.print(pdfDoc, new HashPrintRequestAttributeSet());
        fis.close();
    }
}
