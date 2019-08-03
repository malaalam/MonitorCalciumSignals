package bdv.ui.panel.uicomponents.ProcessTimeFrame;

import bdv.ui.panel.BigDataViewerUI;
import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.io.comparator.NameFileComparator;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.event.EventService;
import org.scijava.event.EventSubscriber;
import org.scijava.thread.ThreadService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class ViewPanel<I extends IntegerType<I>, T extends NumericType<T>, L> extends JPanel {

    private JLabel xLabel;
    private JTextField xTextField;
    private JLabel yLabel;
    private JTextField yTextField;
    private JLabel zLabel;
    private JTextField zTextField;
    private JLabel widthLabel;
    private JTextField widthTextField;
    private JLabel heightLabel;
    private JTextField heightTextField;
    private JLabel depthLabel;
    private JTextField depthTextField;
    private JButton analyzeButton;

    private EventService es;

    private CommandService cs;

    private ThreadService ts;

    private OpService ops;

    private List<EventSubscriber<?>> subs;

    /*Browse button*/
    private JButton browse;

    /*Open File button*/
    private JButton open;

    /*Text Area for filename*/
    private JTextField textField;


    /*File chooser*/
    private final JFileChooser fc = new JFileChooser("/home/manan/Desktop/03_Datasets/KellerLab/Data/CM03/");


    public ViewPanel(final CommandService cs, final EventService es, final ThreadService ts, final OpService ops, final BigDataViewerUI bdvUI) {
        this.es = es;
        this.cs = cs;
        this.ts = ts;
        this.ops = ops;
        subs = this.es.subscribe(this);
        browse = new JButton("Browse");
        textField = new JTextField(" ", 30);
        xLabel = new JLabel("X -position (Top-Left)");
        xTextField = new JTextField("200", 3);
        yLabel = new JLabel("Y -position (Top-Left)");
        yTextField = new JTextField("200", 3);
        zLabel = new JLabel("Z -position (Top-Left)");
        zTextField = new JTextField("40", 3);
        widthLabel = new JLabel("Width");
        widthTextField = new JTextField("200", 3);
        heightLabel = new JLabel("Height");
        heightTextField = new JTextField("200", 3);
        depthLabel = new JLabel("Depth");
        depthTextField = new JTextField("10", 3);
        analyzeButton = new JButton("Analyze");

        setupBrowseButton();
        setupAnalyzeButton(bdvUI);

        setupPanel();
        this.add(browse);
        this.add(textField, "wrap");
        this.add(xLabel);
        this.add(xTextField, "wrap");
        this.add(yLabel);
        this.add(yTextField, "wrap");
        this.add(zLabel);
        this.add(zTextField, "wrap");
        this.add(widthLabel);
        this.add(widthTextField, "wrap");
        this.add(heightLabel);
        this.add(heightTextField, "wrap");
        this.add(depthLabel);
        this.add(depthTextField, "wrap");
        this.add(analyzeButton, "wrap");

    }

    private void setupPanel() {

        this.setBackground(java.awt.Color.WHITE);
        this.setBorder(new TitledBorder(""));
        this.setLayout(new MigLayout("fillx", "", ""));
    }

    private void setupBrowseButton() {
        browse.setBackground(Color.WHITE);
        browse.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == browse) {
                    int returnVal = fc.showOpenDialog(null);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File file = fc.getCurrentDirectory();
                        textField.setText(file.getAbsolutePath());
                    }

                }
            }
        });
    }

    private void setupAnalyzeButton(BigDataViewerUI bdvUI) {
        analyzeButton.setBackground(java.awt.Color.WHITE);
        analyzeButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == analyzeButton) {
                    Context context = new Context();
                    DatasetIOService ioService = context.service(DatasetIOService.class);


                    File dir = new File(textField.getText());
                    File[] directoryListing = dir.listFiles();
                    Arrays.sort(directoryListing, NameFileComparator.NAME_COMPARATOR);

                    double[] intensityDenoised = new double[directoryListing.length];
                    int counter = 0;
                    for (File child : directoryListing) {

                        try {
                            Dataset imgSourceDenoised = ioService.open(child.getAbsolutePath());
                            // Chop a view
                            IntervalView<RealType<?>> viewDenoised = Views.offsetInterval(imgSourceDenoised, new long[]{Integer.parseInt(xTextField.getText()), Integer.parseInt(yTextField.getText()), Integer.parseInt(zTextField.getText())}, new long[]{Integer.parseInt(widthTextField.getText()), Integer.parseInt(heightTextField.getText()), Integer.parseInt(depthTextField.getText())});
                            // Sum pixel intensity within view
                            intensityDenoised[counter] = sumView(viewDenoised);
                            System.out.println(intensityDenoised[counter]);
                            // Write to csv
                            writeCSV(intensityDenoised);
                            counter++;
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }


                }
            }
        });

    }

    private void writeCSV(double[] intensityDenoised) {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter("/home/manan/Desktop/results.csv");
            final String DELIMITER = " ";
            final String NEW_LINE_SEPARATOR = "\n";
            for (int i = 0; i < intensityDenoised.length; i++) {
                fileWriter.append(String.valueOf(intensityDenoised[i]));
                fileWriter.append(NEW_LINE_SEPARATOR);


            }


        } catch (IOException e) {
            System.out.println("Error in CSVFileWriter !!!");
            e.printStackTrace();
        } finally {

            try {
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                System.out.println("Error while flushing/closing fileWriter !!!");
                e.printStackTrace();
            }
        }

    }


    private double sumView(IntervalView<RealType<?>> view) {
        Cursor<RealType<?>> viewCursor = view.localizingCursor();
        int[] pos = new int[3];
        RandomAccess<RealType<?>> viewRandomAccess = view.randomAccess();
        double sum = 0;
        while (viewCursor.hasNext()) {
            viewCursor.fwd();
            pos[0] = viewCursor.getIntPosition(0);
            pos[1] = viewCursor.getIntPosition(1);
            pos[2] = viewCursor.getIntPosition(2);
            viewRandomAccess.setPosition(pos);
            sum += viewRandomAccess.get().getRealDouble();
        }
        return sum;
    }


}