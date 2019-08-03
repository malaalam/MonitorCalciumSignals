package bdv.ui.panel.uicomponents.ProcessTimeFrame;


import Jama.Matrix;
import bdv.ui.panel.BigDataViewerUI;
import ij.IJ;
import io.scif.img.ImgOpener;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.math.linear.BlockRealMatrix;
import org.apache.commons.math.linear.QRDecompositionImpl;
import org.apache.commons.math.linear.RealMatrix;
import org.scijava.command.CommandService;
import org.scijava.event.EventService;
import org.scijava.event.EventSubscriber;
import org.scijava.thread.ThreadService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;

public class RegisterImagesPanel<I extends IntegerType<I>, T extends NumericType<T>, L> extends JPanel {

    private JLabel registerLabel;

    private JComboBox imageOneComboBox;

    private JLabel toLabel;

    private JComboBox imageTwoComboBox;

    private JLabel imageDimensionalityLabel;

    private JComboBox imageDimensionalityCombobox;

    private JLabel loadCorrespondencesLabel;

    private JButton browseCorrespondencesButton;

    private JLabel loadAffineTransformLabel;

    private JButton browseAffineTransformButton;

    private JTextField affineTransformTextField;

    private JLabel saveAffineTransformLabel;

    private JButton saveAffineTransformButton;

    private JTextField correspondencesTextField;

    private static JButton runButton;

    /*File chooser*/
    private final JFileChooser fc = new JFileChooser("/home/manan/Desktop/08_SampleData/02_Images/06_Raghav_Calcium/GCaMP6s_Larva/projections_denoised");


    private EventService es;

    private CommandService cs;

    private ThreadService ts;

    private OpService ops;

    private List<EventSubscriber<?>> subs;

    private BlockRealMatrix sourceMatrix;

    private BlockRealMatrix targetMatrix;

    private Matrix affineTransform;

    private Matrix affineTransformInverse;

    public RegisterImagesPanel(final CommandService cs, final EventService es, final ThreadService ts, final OpService ops, final BigDataViewerUI bdvUI) {
        this.es = es;
        this.cs = cs;
        this.ts = ts;
        this.ops = ops;
        subs = this.es.subscribe(this);
        registerLabel = new JLabel("Register");
        imageOneComboBox = new JComboBox();
        toLabel = new JLabel("to");
        imageTwoComboBox = new JComboBox();
        imageDimensionalityLabel = new JLabel("Dimensions");
        String[] dimensions = {"2-D", "3-D"};
        imageDimensionalityCombobox = new JComboBox(dimensions);
        loadCorrespondencesLabel = new JLabel("Load Correspondences");
        browseCorrespondencesButton = new JButton("Browse");
        loadAffineTransformLabel = new JLabel("Load Affine Transform");
        browseAffineTransformButton = new JButton("Browse");
        affineTransformTextField = new JTextField("", 20);
        saveAffineTransformLabel = new JLabel("Save Affine Transform");
        saveAffineTransformButton = new JButton("Browse");
        setupBrowseCorrespondencesButton();
        setupBrowseAffineTransformButton();
        setupSaveAffineTransformButton();
        correspondencesTextField = new JTextField("", 20);
        runButton = new JButton("Run");
        setupRunButton(bdvUI);
        setupComboBox();
        setupPanel();

        this.add(registerLabel, "wrap");
        this.add(imageOneComboBox, "wrap");
        this.add(toLabel, "wrap");
        this.add(imageTwoComboBox, "wrap");
        this.add(imageDimensionalityLabel);
        this.add(imageDimensionalityCombobox, "wrap");
        this.add(loadCorrespondencesLabel);
        this.add(browseCorrespondencesButton, "wrap");
        this.add(correspondencesTextField, "wrap");
        this.add(loadAffineTransformLabel);
        this.add(browseAffineTransformButton, "wrap");
        this.add(affineTransformTextField, "wrap");
        this.add(saveAffineTransformLabel);
        this.add(saveAffineTransformButton, "wrap");
        this.add(runButton, "wrap");
    }

    private void setupSaveAffineTransformButton() {
        saveAffineTransformButton.setBackground(Color.WHITE);
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Choose Directory");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        saveAffineTransformButton.addActionListener(e -> {
            if (e.getSource() == saveAffineTransformButton) {
                readCorrespondencesCSV(correspondencesTextField.getText());
                calculateAffineTransform();

                int returnVal = fc.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    writeAffineTransformToCSV(file.getAbsolutePath());

                }

            }
        });

    }


    private void calculateAffineTransform() {
        RealMatrix affineTranspose = (RealMatrix) new QRDecompositionImpl(sourceMatrix).getSolver().solve(targetMatrix);

        affineTransform = new Matrix(affineTranspose.getColumnDimension(), affineTranspose.getRowDimension());
        for (int i = 0; i < affineTranspose.getColumnDimension(); i++) {
            for (int j = 0; j < affineTranspose.getRowDimension(); j++) {
                affineTransform.set(i, j, affineTranspose.getEntry(j, i));
            }
        }


    }

    private void setupBrowseAffineTransformButton() {

        browseAffineTransformButton.setBackground(Color.WHITE);
        browseAffineTransformButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == browseAffineTransformButton) {
                    int returnVal = fc.showOpenDialog(null);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File file = fc.getSelectedFile();
                        affineTransformTextField.setText(file.getAbsolutePath());
                        readAffineTransformCSV(file.getAbsolutePath());
                        affineTransformInverse = affineTransform.inverse();
                    }

                }
            }
        });
    }

    private void setupBrowseCorrespondencesButton() {
        browseCorrespondencesButton.setBackground(Color.WHITE);
        browseCorrespondencesButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == browseCorrespondencesButton) {
                    int returnVal = fc.showOpenDialog(null);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File file = fc.getSelectedFile();
                        correspondencesTextField.setText(file.getAbsolutePath());
                        readCorrespondencesCSV(correspondencesTextField.getText());
                        calculateAffineTransform();
                        affineTransformInverse = affineTransform.inverse();
                    }

                }
            }
        });
    }

    private void setupPanel() {

        this.setBackground(Color.white);
        this.setBorder(new TitledBorder(""));
        this.setLayout(new MigLayout("fillx", "", ""));
    }

    private void setupComboBox() {
        imageOneComboBox.setBackground(Color.WHITE);
        imageTwoComboBox.setBackground(Color.WHITE);

    }


    private void setupRunButton(BigDataViewerUI bdvUI) {
        runButton.setBackground(Color.WHITE);
        runButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == runButton) {
                    ts.getExecutorService().submit(new Callable<T>() {

                        @Override
                        public T call() throws Exception {


                            System.out.println("start calculation");
                            @SuppressWarnings("unchecked")
                            Img<UnsignedShortType> source = (Img<UnsignedShortType>)
                                    new ImgOpener().openImgs((String) imageOneComboBox.getSelectedItem()).get(0);
                            Img<UnsignedShortType> target = (Img<UnsignedShortType>)
                                    new ImgOpener().openImgs((String) imageTwoComboBox.getSelectedItem()).get(0);
                            AffineTransform3D affineTransform3D = new AffineTransform3D();
                            affineTransform3D.set(affineTransform.get(0, 0), 0, 0);
                            affineTransform3D.set(affineTransform.get(0, 1), 0, 1);
                            affineTransform3D.set(affineTransform.get(0, 2), 0, 2);
                            affineTransform3D.set(affineTransform.get(0, 3), 0, 3);

                            affineTransform3D.set(affineTransform.get(1, 0), 1, 0);
                            affineTransform3D.set(affineTransform.get(1, 1), 1, 1);
                            affineTransform3D.set(affineTransform.get(1, 2), 1, 2);
                            affineTransform3D.set(affineTransform.get(1, 3), 1, 3);

                            affineTransform3D.set(affineTransform.get(2, 0), 2, 0);
                            affineTransform3D.set(affineTransform.get(2, 1), 2, 1);
                            affineTransform3D.set(affineTransform.get(2, 2), 2, 2);
                            affineTransform3D.set(affineTransform.get(2, 3), 2, 3);
                            RealRandomAccessible<UnsignedShortType>
                                    interpolated = Views.interpolate(Views.extendZero(source), new NLinearInterpolatorFactory<>());
                            RealRandomAccessible<UnsignedShortType>
                                    transformed = RealViews.affine(interpolated, affineTransform3D);
                            RandomAccessibleInterval<UnsignedShortType>
                                    rai = Views.interval(Views.raster(transformed), target);
                            //ImageJFunctions.show(rai).setDisplayRange(0, 1200);


                            //IJ.save(ImageJFunctions.wrap(rai, "transformed"), "/home/manan/Desktop/08_SampleData/02_Images/06_Raghav_Calcium/GCaMP6s_Larva/tifsflipped/CM02/imgTransformed.tif");
                            IJ.save(ImageJFunctions.wrap(rai, "transformed"), "/home/manan/Desktop/imgTransformed.tif");
                            System.out.println("done calculation");
                            return null;


                        }
                    });
                }
            }

        });

    }


    public void addImage(String name) {
        imageOneComboBox.addItem(name);
        imageTwoComboBox.addItem(name);

    }

    private void readAffineTransformCSV(String filename) {
        try {

            if (imageDimensionalityCombobox.getSelectedIndex() == 0) {
                affineTransform = new Matrix(3, 3);
            } else {
                affineTransform = new Matrix(4, 4);
            }
            BufferedReader reader = new BufferedReader(new FileReader(filename));

            String l = reader.readLine();

            int row = 0;
            while (l != null) {
                // Change delimiter accordingly
                //String[] tokens = l.split("\t");
                String[] tokens = l.split(",");
                if (imageDimensionalityCombobox.getSelectedIndex() == 0) {
                    affineTransform.set(row, 0, Float.parseFloat(tokens[0]));
                    affineTransform.set(row, 1, Float.parseFloat(tokens[1]));
                    affineTransform.set(row, 2, Float.parseFloat(tokens[2]));
                } else {
                    affineTransform.set(row, 0, Float.parseFloat(tokens[0]));
                    affineTransform.set(row, 1, Float.parseFloat(tokens[1]));
                    affineTransform.set(row, 2, Float.parseFloat(tokens[2]));
                    affineTransform.set(row, 3, Float.parseFloat(tokens[3]));
                }

                l = reader.readLine();
                row++;
            }
            reader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void readCorrespondencesCSV(String filename) {
        try {
            Path path = Paths.get(filename);
            long lineCount = Files.lines(path).count();
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String l = reader.readLine();
            if (imageDimensionalityCombobox.getSelectedIndex() == 0) {
                sourceMatrix = new BlockRealMatrix((int) lineCount, 3);
                targetMatrix = new BlockRealMatrix((int) lineCount, 3);
            } else {
                sourceMatrix = new BlockRealMatrix((int) lineCount, 4);
                targetMatrix = new BlockRealMatrix((int) lineCount, 4);
            }

            int row = 0;
            while (l != null) {
                String[] tokens = l.split("\t");
                if (imageDimensionalityCombobox.getSelectedIndex() == 0) {
                    sourceMatrix.setEntry(row, 0, Float.parseFloat(tokens[0]));
                    sourceMatrix.setEntry(row, 1, Float.parseFloat(tokens[1]));
                    sourceMatrix.setEntry(row, 2, 1);
                    targetMatrix.setEntry(row, 0, Float.parseFloat(tokens[8]));
                    targetMatrix.setEntry(row, 1, Float.parseFloat(tokens[9]));
                    targetMatrix.setEntry(row, 2, 1);

                } else {
                    sourceMatrix.setEntry(row, 0, Float.parseFloat(tokens[0]));
                    sourceMatrix.setEntry(row, 1, Float.parseFloat(tokens[1]));
                    sourceMatrix.setEntry(row, 2, Float.parseFloat(tokens[2]));
                    sourceMatrix.setEntry(row, 3, 1);
                    targetMatrix.setEntry(row, 0, Float.parseFloat(tokens[8]));
                    targetMatrix.setEntry(row, 1, Float.parseFloat(tokens[9]));
                    targetMatrix.setEntry(row, 2, Float.parseFloat(tokens[10]));
                    targetMatrix.setEntry(row, 3, 1);
                }

                l = reader.readLine();
                row++;
            }
            reader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void writeAffineTransformToCSV(String path) {
        final String COMMA_DELIMITER = ",";
        final String NEW_LINE_SEPARATOR = "\n";
        FileWriter fileWriter = null;

        final String fileName = path + "/AffineTransform_" + java.time.LocalDateTime.now() + ".csv";

        try {
            fileWriter = new FileWriter(fileName);
            for (int i = 0; i < affineTransform.getRowDimension(); i++) {
                for (int j = 0; j < affineTransform.getColumnDimension(); j++) {
                    fileWriter.append(String.valueOf(affineTransform.get(i, j)));
                    fileWriter.append(COMMA_DELIMITER);

                }
                fileWriter.append(NEW_LINE_SEPARATOR);

            }


        } catch (Exception e) {
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


    public String getImageOne() {
        return (String) this.imageOneComboBox.getSelectedItem();

    }

    public String getImageTwo() {
        return (String) this.imageTwoComboBox.getSelectedItem();

    }

}
