package bdv.ui.panel.uicomponents.ProcessTimeFrame;

import bdv.ui.panel.BigDataViewerUI;
import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import net.miginfocom.swing.MigLayout;
import org.jzy3d.chart.Chart;
import org.jzy3d.chart2d.Chart2d;
import org.jzy3d.plot2d.primitives.Serie2d;
import org.jzy3d.plot3d.primitives.ConcurrentLineStrip;
import org.jzy3d.plot3d.primitives.axes.layout.IAxeLayout;
import org.jzy3d.plot3d.primitives.axes.layout.renderers.PitchTickRenderer;
import org.jzy3d.ui.LookAndFeel;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.event.EventService;
import org.scijava.event.EventSubscriber;
import org.scijava.thread.ThreadService;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;



public class ViewPanel<I extends IntegerType<I>, T extends NumericType<T>, L> extends JPanel {

    private JLabel xLabel;
    private JTextField xTextField;
    private JLabel yLabel;
    private JTextField yTextField;
    private JLabel widthLabel;
    private JTextField widthTextField;
    private JLabel heightLabel;
    private JTextField heightTextField;

    private JButton analyzeButton;

    private EventService es;

    private CommandService cs;

    private ThreadService ts;

    private OpService ops;

    private List<EventSubscriber<?>> subs;


    public ViewPanel(final CommandService cs, final EventService es, final ThreadService ts, final OpService ops, final BigDataViewerUI bdvUI) {
        this.es = es;
        this.cs = cs;
        this.ts = ts;
        this.ops = ops;
        subs = this.es.subscribe(this);
        xLabel = new JLabel("X -position (Top-Left)");
        xTextField = new JTextField("200", 3);
        yLabel = new JLabel("Y -position (Top-Left)");
        yTextField = new JTextField("200", 3);
        widthLabel = new JLabel("Width");
        widthTextField = new JTextField("200", 3);
        heightLabel = new JLabel("Height");
        heightTextField = new JTextField("200", 3);
        analyzeButton = new JButton("Analyze");

        setupAnalyzeButton2(bdvUI);

        setupPanel();
        this.add(xLabel);
        this.add(xTextField, "wrap");
        this.add(yLabel);
        this.add(yTextField, "wrap");
        this.add(widthLabel);
        this.add(widthTextField, "wrap");
        this.add(heightLabel);
        this.add(heightTextField, "wrap");
        this.add(analyzeButton);

    }

    private void setupPanel() {

        this.setBackground(Color.white);
        this.setBorder(new TitledBorder(""));
        this.setLayout(new MigLayout("fillx", "", ""));
    }

    private void setupAnalyzeButton2(BigDataViewerUI bdvUI) {
        analyzeButton.setBackground(Color.WHITE);
        analyzeButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == analyzeButton) {
                    Context context = new Context();
                    DatasetIOService ioService = context.service(DatasetIOService.class);

                    try {


                        Dataset imgTarget = ioService.open("/home/manan/Desktop/08_SampleData/02_Images/06_Raghav_Calcium/GCaMP6s_Larva/projections_denoised/02_Final/01.tif");
                        Dataset imgSourceDenoised = ioService.open("/home/manan/Desktop/08_SampleData/02_Images/06_Raghav_Calcium/GCaMP6s_Larva/projections_denoised/02_Final/02_denoisedRegistered.tif");
                        Dataset imgSource = ioService.open("/home/manan/Desktop/08_SampleData/02_Images/06_Raghav_Calcium/GCaMP6s_Larva/projections_denoised/02_Final/03_noisyRegistered.tif");

                        RandomAccessibleInterval<RealType<?>> viewOne = Views.offsetInterval(imgTarget, new long[]{Integer.parseInt(xTextField.getText()), Integer.parseInt(yTextField.getText()), 0}, new long[]{Integer.parseInt(widthTextField.getText()), Integer.parseInt(heightTextField.getText()), imgTarget.dimension(2) - 1});
                        RandomAccessibleInterval<RealType<?>> viewTwo = Views.offsetInterval(imgSourceDenoised, new long[]{Integer.parseInt(xTextField.getText()), Integer.parseInt(yTextField.getText()), 0}, new long[]{Integer.parseInt(widthTextField.getText()), Integer.parseInt(heightTextField.getText()), imgSourceDenoised.dimension(2) - 1});
                        RandomAccessibleInterval<RealType<?>> viewThree = Views.offsetInterval(imgSource, new long[]{Integer.parseInt(xTextField.getText()), Integer.parseInt(yTextField.getText()), 0}, new long[]{Integer.parseInt(widthTextField.getText()), Integer.parseInt(heightTextField.getText()), imgSource.dimension(2) - 1});

                        Img<DoubleType> imageOne = createDIByI(viewOne);
                        Img<DoubleType> imageTwo = createDIByI(viewTwo);
                        Img<DoubleType> imageThree = createDIByI(viewThree);


                        double[] dIbyI_ImageOne = sumEachSlice(imageOne);
                        double[] dIbyI_ImageTwo = sumEachSlice(imageTwo);
                        double[] dIbyI_ImageThree = sumEachSlice(imageThree);

                        class holdOnChart {
                            public Chart2d imageOneChart;
                            public Chart2d imageTwoChart;
                            public Chart2d imageThreeChart;
                            public Serie2d serieImageOne;
                            public Serie2d serieImageTwo;
                            public Serie2d serieImageThree;
                            public ConcurrentLineStrip pitchLineStrip;
                            public ConcurrentLineStrip amplitudeLineStrip;

                            public holdOnChart(float timeMax, int freqMax) {
                                imageOneChart = new Chart2d();
                                imageOneChart.asTimeChart(timeMax, 0, freqMax, "Time", "Target");

                                IAxeLayout axe = imageOneChart.getAxeLayout();

                                axe.setYTickRenderer(new PitchTickRenderer());

                                serieImageOne = imageOneChart.getSerie("Target", Serie2d.Type.LINE);
                                serieImageOne.setColor(org.jzy3d.colors.Color.BLUE);
                                pitchLineStrip = (ConcurrentLineStrip) serieImageOne.getDrawable();

                                imageTwoChart = new Chart2d();
                                imageTwoChart.asTimeChart(timeMax, 0, freqMax, "Time", "Denoised Registered");
                                serieImageTwo = imageTwoChart.getSerie("DenoisedRegistered", Serie2d.Type.LINE);
                                serieImageTwo.setColor(org.jzy3d.colors.Color.RED);
                                amplitudeLineStrip = (ConcurrentLineStrip) serieImageTwo.getDrawable();

                                imageThreeChart = new Chart2d();
                                imageThreeChart.asTimeChart(timeMax, 0, freqMax, "Time", "Noisy Registered");
                                serieImageThree = imageThreeChart.getSerie("NoisyRegistered", Serie2d.Type.LINE);
                                serieImageThree.setColor(org.jzy3d.colors.Color.RED);
                                amplitudeLineStrip = (ConcurrentLineStrip) serieImageThree.getDrawable();

                            }

                            public List<Chart> getCharts() {
                                List<Chart> charts = new ArrayList<Chart>();
                                charts.add(imageOneChart);
                                charts.add(imageTwoChart);
                                charts.add(imageThreeChart);
                                return charts;
                            }
                        }
                        class TimeChartWindow extends JFrame {
                            private static final long serialVersionUID = 7519209038396190502L;

                            public TimeChartWindow(List<Chart> charts) throws IOException {
                                LookAndFeel.apply();
                                String lines = "[300px]";
                                String columns = "[500px,grow]";
                                setLayout(new MigLayout("", columns, lines));
                                int k = 0;
                                for (Chart c : charts) {
                                    addChart(c, k++);
                                }
                                windowExitListener();
                                this.pack();
                                show();
                                setVisible(true);
                            }

                            public void addChart(Chart chart, int id) {
                                Component canvas = (java.awt.Component) chart.getCanvas();

                                JPanel chartPanel = new JPanel(new BorderLayout());

                                Border b = BorderFactory.createLineBorder(java.awt.Color.black);
                                chartPanel.setBorder(b);
                                chartPanel.add(canvas, BorderLayout.CENTER);
                                add(chartPanel, "cell 0 " + id + ", grow");
                            }

                            public void windowExitListener() {
                                addWindowListener(new WindowAdapter() {
                                    @Override
                                    public void windowClosing(WindowEvent e) {
                                        TimeChartWindow.this.dispose();
                                        System.exit(0);
                                    }
                                });
                            }
                        }


                        holdOnChart log = new holdOnChart(799, 20000);
                        new TimeChartWindow(log.getCharts());
                        for (int i=0; i<dIbyI_ImageOne.length; i++){
                            log.serieImageOne.add(i, dIbyI_ImageOne[i]);
                            log.serieImageTwo.add(i, dIbyI_ImageTwo[i]);
                            log.serieImageThree.add(i, dIbyI_ImageThree[i]);
                        }




                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                }
            }
        });

    }


    private double[] sumEachSlice(Img<DoubleType> image) {
        int dim = 2;
        int[] pix = new int[2];
        RandomAccessibleInterval<DoubleType> slice;
        double[] dIbyI = new double[(int) image.dimension(dim)];
        for (long pos = 0; pos < image.dimension(dim); ++pos) {
            slice = Views.hyperSlice(image, dim, pos);
            Cursor<DoubleType> sliceCursor = ((IntervalView<DoubleType>) slice).localizingCursor();
            RandomAccess<DoubleType> ra = slice.randomAccess();
            while (sliceCursor.hasNext()) {
                sliceCursor.fwd();
                pix[0] = sliceCursor.getIntPosition(0);
                pix[1] = sliceCursor.getIntPosition(1);

                ra.setPosition(pix);
                dIbyI[(int) pos]=dIbyI[(int) pos] + ra.get().getRealDouble();
            }

        }
        return dIbyI;

    }

    private Img<DoubleType> createDIByI(RandomAccessibleInterval<RealType<?>> view) {

        Img<DoubleType> image = ArrayImgs.doubles(view.dimension(0), view.dimension(1), view.dimension(2));
        IterableInterval<RealType<?>> iterableTarget = Views.iterable(view);
        RandomAccess<RealType<?>> ra = view.randomAccess();
        RandomAccess<DoubleType> raImage = image.randomAccess();
        Cursor<RealType<?>> targetCursor = iterableTarget.localizingCursor();
        int[] pos = new int[3];
        int[] posBack = new int[3];
        int[] posForward = new int[3];
        double dIByI;
        int intensityCurrent;
        int intensityBack;
        int intensityForward;
        while (targetCursor.hasNext()) {
            try {
                targetCursor.fwd();
                int x = targetCursor.getIntPosition(0);
                int y = targetCursor.getIntPosition(1);
                int t = targetCursor.getIntPosition(2);
                pos[0] = x;
                pos[1] = y;
                pos[2] = t;
                posBack[0] = x;
                posBack[1] = y;
                if (t == 0) {
                    posBack[2] = t; // zero gradient at t=0
                } else {
                    posBack[2] = t - 1;
                }
                posForward[0] = x;
                posForward[1] = y;
                if (t == image.dimension(2) - 1) {
                    posForward[2] = t; // zero gradient at t=798
                } else {
                    posForward[2] = t + 1;
                }
                ra.setPosition(pos);
                intensityCurrent = (int) ra.get().getRealDouble();
                ra.setPosition(posBack);
                intensityBack = (int) ra.get().getRealDouble();
                ra.setPosition(posForward);
                intensityForward = (int) ra.get().getRealDouble();
                dIByI=(double)(intensityForward-intensityBack)/intensityCurrent;
                raImage.setPosition(pos);
                raImage.get().setReal(dIByI);


            } catch (Exception e) {
                System.out.println("x = " + pos[0]);
                System.out.println("y = " + pos[1]);
                System.out.println("t = " + pos[2]);
            }

        }
        return image;


    }


}