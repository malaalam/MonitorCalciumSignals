/*-
 * #%L
 * UI for BigDataViewer.
 * %%
 * Copyright (C) 2017 - 2018 Tim-Oliver Buchholz
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv.ui.panel;

import bdv.BigDataViewer;
import bdv.ui.panel.control.BDVController;
import bdv.ui.panel.control.BDVHandlePanel;
import bdv.ui.panel.control.BehaviourTransformEventHandlerSwitchable;
import bdv.ui.panel.lut.ColorTableConverter;
import bdv.ui.panel.projector.AccumulateProjectorAlphaBlendingARGB;
import bdv.ui.panel.uicomponents.*;
import bdv.ui.panel.uicomponents.ProcessTimeFrame.OpenFilePanel;
import bdv.ui.panel.uicomponents.ProcessTimeFrame.RegisterImagesPanel;
import bdv.ui.panel.uicomponents.ProcessTimeFrame.ViewPanel;
import bdv.util.*;
import bdv.viewer.Source;
import bdv.viewer.render.AccumulateProjectorFactory;
import bdv.viewer.render.VolatileProjector;
import gnu.trove.map.hash.TIntIntHashMap;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.NumericType;
import net.miginfocom.swing.MigLayout;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.event.EventService;
import org.scijava.event.EventSubscriber;
import org.scijava.log.DefaultLogger;
import org.scijava.log.LogLevel;
import org.scijava.log.LogSource;
import org.scijava.log.Logger;
import org.scijava.thread.ThreadService;
import org.scijava.ui.swing.console.LoggingPanel;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * UI for the {@link BigDataViewer} mapping all functionality to UI-Components.
 *
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 */
public class BigDataViewerUI<I extends IntegerType<I>, T extends NumericType<T>, L> {

    private static final String CONTROL_CARD_NAME = "Viewer Settings";

    private static final String SELECTION_CARD_NAME = "Selection";

    private static final String LOG_CARD_NAME = "Log";

    private static final String OPEN_FILE_CARD_NAME = "Open File";

    private static final String REGISTER_IMAGES_CARD_NAME="Register Images";

    private static final String VIEW_CARD_NAME="Analyze ";
    /**
     * Splitpane holding the BDV and UI.
     */
    private final JSplitPane splitPane;

    /**
     * Controller which keeps BDV and UI in synch.
     */
    private BDVController<I, T, L> bdv;

    /**
     * Map from source names to {@link SourceProperties}.
     */
    private final Map<String, SourceProperties<T>> sourceLookup = new HashMap<>();

    /**
     * Map from label-source-index to {@link ColorTableConverter}.
     */
    private Map<Integer, ColorTableConverter<L>> converters;

    /* Panel holding the control UI-Components */
    private CardPanel controlsPanel;

    /*
     * The main panel.
     */
    private JPanel panel;


    /* Load Open File Panel */
    private OpenFilePanel<I, T, L> openFilePanel;

    /*Register Images Panel*/
    private RegisterImagesPanel<I, T, L> registerImagesPanel;

    /*View Panel*/
    private ViewPanel<I, T, L> viewPanel;

    /* Panel holding the transformation manipulation options */
    private TransformationPanel<I, T, L> transformationPanel;

    /* Source & Group selection panel */
    private SelectionAndGroupingTabs<I, T, L> selectionAndGrouping;

    /* The event service */
    private EventService es;

    /* The command service */
    private CommandService cs;

    /* The thread service */
    private ThreadService ts;

    /* The Ops service */
    private OpService ops;

    /**
     * List of all subscribers.
     */
    private List<EventSubscriber<?>> subs;


    private LoggingPanel loggingPanel;

    private Logger logger;


    /**
     * BDV {@link AccumulateProjectorFactory} which generates
     * {@link AccumulateProjectorAlphaBlendingARGB} which adds image sources
     * together and blends labeling sources on top with alpha-blending.
     */
    final AccumulateProjectorFactory<ARGBType> myFactory = new AccumulateProjectorFactory<ARGBType>() {

        @Override
        public synchronized AccumulateProjectorAlphaBlendingARGB createAccumulateProjector(
                final ArrayList<VolatileProjector> sourceProjectors, final ArrayList<Source<?>> sources,
                final ArrayList<? extends RandomAccessible<? extends ARGBType>> sourceScreenImages,
                final RandomAccessibleInterval<ARGBType> targetScreenImages, final int numThreads,
                final ExecutorService executorService) {

            // lookup is true if source is a labeling
            final List<Boolean> lookup = new ArrayList<>();
            int startImgs = -1;
            int startLabs = -1;

            for (Source<?> s : sources) {
                try {
                    lookup.add(new Boolean(sourceLookup.get(s.getName()).isLabeling()));
                } catch (Exception e) {
                    for (SourceProperties<T> p : sourceLookup.values()) {
                        System.out.println(p.getSourceName() + ", " + p.isLabeling());
                    }
                }
            }

            final boolean[] labelingLookup = new boolean[lookup.size()];
            for (int i = 0; i < lookup.size(); i++) {
                final boolean b = lookup.get(i).booleanValue();
                if (startImgs < 0 && !b) {
                    startImgs = i;
                }
                if (startLabs < 0 && b) {
                    startLabs = i;
                }
                labelingLookup[i] = b;
            }

            return new AccumulateProjectorAlphaBlendingARGB(sourceProjectors, sourceScreenImages, targetScreenImages,
                    numThreads, executorService, labelingLookup, startImgs, startLabs);
        }

    };

    /**
     * A new BigDataViewer-UI instance.
     *
     * @param frame the parent
     * @param ctx   context
     */
    public BigDataViewerUI(final JFrame frame, final Context ctx, final BdvOptions options) {
        ctx.inject(this);
        this.es = ctx.getService(EventService.class);
        this.cs = ctx.getService(CommandService.class);
        this.ts = ctx.getService(ThreadService.class);
        this.ops = ctx.getService(OpService.class);
        subs = es.subscribe(this);

        final BDVHandlePanel<I, T, L> bdvHandlePanel = createBDVHandlePanel(frame, options);

        selectionAndGrouping = new SelectionAndGroupingTabs<>(es, bdvHandlePanel);

        bdv = new BDVController<>(bdvHandlePanel, selectionAndGrouping, sourceLookup, converters, es);
        ctx.inject(bdv);

        controlsPanel = new CardPanel();


        // Add log card (Card No. 0)
        loggingPanel = new LoggingPanel(ctx);
        loggingPanel.setBackground(Color.WHITE);
        loggingPanel.setTextFilterVisible(true);
        loggingPanel.setPreferredSize(new Dimension(50, 150));
        logger = new DefaultLogger(log -> {
        }, LogSource.newRoot(), LogLevel.INFO);
        logger.addLogListener(loggingPanel);
        logger.info("Hello World!");
        controlsPanel.addNewCard(new JLabel(LOG_CARD_NAME), false, loggingPanel);


        // Add selection card (Card No. 1)
        final JPanel visAndGroup = new JPanel(new MigLayout("fillx, ins 2", "[grow]", ""));
        visAndGroup.setBackground(Color.WHITE);
        visAndGroup.add(selectionAndGrouping, "growx, wrap");
        controlsPanel.addNewCard(new JLabel(SELECTION_CARD_NAME), false, visAndGroup);


        // Add control card (Card NO. 2)
        final JPanel globalControls = new JPanel(new MigLayout("fillx, ins 2", "[grow]", ""));
        transformationPanel = new TransformationPanel<>(es, bdv);
        globalControls.add(transformationPanel, "growx, wrap");
        globalControls.add(new InterpolationModePanel(es, bdvHandlePanel.getViewerPanel()), "growx");
        controlsPanel.addNewCard(new JLabel(CONTROL_CARD_NAME), false, globalControls);

        // Add control card (Card NO. 3)
        openFilePanel = new OpenFilePanel<>(cs, es, ts, ops, this);
        controlsPanel.addNewCard(new JLabel(OPEN_FILE_CARD_NAME), false, openFilePanel);

        // Add card (Card No. 4)
        registerImagesPanel = new RegisterImagesPanel<>(cs, es, ts, ops, this);
        controlsPanel.addNewCard(new JLabel(REGISTER_IMAGES_CARD_NAME), false, registerImagesPanel);

        // Add card (Card No. 5)
        viewPanel = new ViewPanel<>(cs, es, ts, ops, this);
        controlsPanel.addNewCard(new JLabel(VIEW_CARD_NAME), false, viewPanel);

        // put UI-Components and BDV into Splitpane panel
        splitPane = createSplitPane();
        final JScrollPane scrollPane = new JScrollPane(controlsPanel);
        scrollPane.setPreferredSize(
                new Dimension(370, bdv.getBDVHandlePanel().getViewerPanel().getPreferredSize().height));
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        splitPane.setLeftComponent(bdvHandlePanel.getViewerPanel());
        splitPane.setRightComponent(scrollPane);
        splitPane.getLeftComponent().setMinimumSize(new Dimension(20, 20));
        splitPane.getLeftComponent().setPreferredSize(bdv.getBDVHandlePanel().getViewerPanel().getPreferredSize());

        panel = new JPanel();
        panel.setLayout(new MigLayout("fillx, filly, ins 0", "[grow]", "[grow]"));
        panel.add(splitPane, "growx, growy");


    }

    /**
     * Create {@link BDVHandlePanel} to display sources of {@link Dimensionality}.
     *
     * @param frame parent
     * @return bdvHandlePanel
     */
    private BDVHandlePanel<I, T, L> createBDVHandlePanel(final JFrame frame, final BdvOptions options) {
        converters = new HashMap<>();
        return new BDVHandlePanel<>(frame,
                options.numSourceGroups(1)
                        .transformEventHandlerFactory(BehaviourTransformEventHandlerSwitchable.factory())
                        .accumulateProjectorFactory(myFactory).numRenderingThreads(1),
                converters);
    }

    /**
     * Add image to the BDV-UI.
     *
     * @param img            the image to add
     * @param type           information
     * @param name           of the source
     * @param visibility     of the source
     * @param groupNames     groups to which this source belongs
     * @param color          of the source
     * @param transformation initial transformation of the source
     * @param min            display range
     * @param max            display range
     */
    public synchronized void addImage(final RandomAccessibleInterval<T> img, final String type, final String name,
                                      final boolean visibility, final Set<String> groupNames, final Color color,
                                      final AffineTransform3D transformation, final double min, final double max) {
        bdv.addImg(img, type, name, visibility, groupNames, color, transformation, min, max);
        if (bdv.getNumSources() == 1) {
            controlsPanel.setCardActive(SELECTION_CARD_NAME, true);
            controlsPanel.toggleCardFold(SELECTION_CARD_NAME);
        }
    }

    public synchronized void addImage(final RandomAccessibleInterval<T> img, final String name, final Color color) {
        final Set<String> groupNames = new HashSet<>();
        groupNames.add("Images");
        bdv.addImg(img, img.randomAccess().get().getClass().getSimpleName(), name, true, groupNames, color,
                new AffineTransform3D(), Double.NaN, Double.NaN);
    }

    public synchronized BdvOverlaySource addOverlay(BdvOverlay overlay, final String name) {

        BdvOverlaySource overlaySource = BdvFunctions.showOverlay(overlay, name, BdvOptions.options().addTo(bdv.getBDVHandlePanel().getBdvHandle()));
        return overlaySource;
    }

    /**
     * Add labeling to the BDV-UI.
     *
     * @param imgLab         the labeling image
     * @param type           information
     * @param name           of the source
     * @param visibility     of the source
     * @param groupNames     groups to which this source belongs
     * @param transformation initial transformation of the source
     * @param lut            look up table for the labeling
     */
    public synchronized void addLabeling(RandomAccessibleInterval<LabelingType<L>> imgLab, final String type,
                                         final String name, final boolean visibility, final Set<String> groupNames,
                                         final AffineTransform3D transformation, final TIntIntHashMap lut) {
        bdv.addLabeling(imgLab, type, name, visibility, groupNames, transformation, lut);
        if (bdv.getNumSources() == 1) {
            controlsPanel.setCardActive(SELECTION_CARD_NAME, true);
            controlsPanel.toggleCardFold(SELECTION_CARD_NAME);
        }
    }

    /**
     * Remove source with given name.
     * <p>
     * Note: Removes images and labelings.
     *
     * @param sourceName to remove
     */
    public synchronized void removeSource(final String sourceName) {
        bdv.removeSource(sourceName);
        closeControlPanels();
    }

    /**
     * Toggle cards.
     */
    private void closeControlPanels() {
        if (bdv.getNumSources() <= 0) {
            controlsPanel.toggleCardFold(SELECTION_CARD_NAME);
            controlsPanel.setCardActive(SELECTION_CARD_NAME, false);
        }
    }

    /**
     * @return the splitpane with BDV and BDV-UI
     */
    public JPanel getPanel() {
        return this.panel;
    }

    /**
     * Create splitpane.
     *
     * @return splitpane
     */
    private JSplitPane createSplitPane() {
        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setUI(new BasicSplitPaneUI() {
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {

                    /**
                     *
                     */
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void paint(Graphics g) {
                        g.setColor(new Color(238, 238, 238));
                        g.fillRect(0, 0, getSize().width, getSize().height);
                        super.paint(g);
                    }
                };
            }
        });

        splitPane.setBackground(new Color(31, 31, 45));
        splitPane.setDividerLocation(bdv.getBDVHandlePanel().getViewerPanel().getPreferredSize().width);
        splitPane.setResizeWeight(1.0);

        return splitPane;
    }

    /**
     * Remove all sources from BDV.
     */
    public synchronized void removeAll() {
        Set<String> keySet = new HashSet<>(sourceLookup.keySet());
        for (final String source : keySet) {
            removeSource(source);
        }
    }

    /**
     * Switch BDV between 2D and 3D mode.
     *
     * @param twoDimensional BDV mode
     */
    public void switch2D(final boolean twoDimensional) {
        bdv.switch2D(twoDimensional);
    }

    /**
     * Unsubscribe everything from the eventservice.
     */
    public synchronized void unsubscribe() {
        this.es.unsubscribe(subs);
        this.transformationPanel.unsubscribe();
        this.selectionAndGrouping.unsubscribe();
    }

    public void addCard(final JLabel name, final boolean closed, final JComponent component) {
        controlsPanel.addNewCard(name, closed, component);
    }


    public Map<String, JPanel> getCards() {
        return controlsPanel.getCards();
    }

    public BDVHandlePanel<I, T, L> getBDVHandlePanel() {
        return bdv.getBDVHandlePanel();
    }

    public BdvHandle getBDV() {
        return bdv.getBDVHandlePanel().getBdvHandle();
    }

    public Logger getLogger() {
        return logger;
    }

    public RegisterImagesPanel<I, T, L> getRegisterImagesPanel() {
        return registerImagesPanel;
    }



}
