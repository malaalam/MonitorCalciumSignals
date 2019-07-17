package bdv.ui.panel.uicomponents.ProcessTimeFrame;

import bdv.ui.panel.BigDataViewerUI;
import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.ops.OpService;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.NumericType;
import net.miginfocom.swing.MigLayout;
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
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

public class OpenFilePanel<I extends IntegerType<I>, T extends NumericType<T>, L> extends JPanel {

    /*Browse button*/
    private JButton browse;

    /*Open File button*/
    private JButton open;

    /*Text Area for filename*/
    private JTextField textField;

    /*File chooser*/
    private final JFileChooser fc = new JFileChooser("/home/manan/Desktop/08_SampleData/02_Images/06_Raghav_Calcium/GCaMP6s_Larva/");

    private EventService es;

    private CommandService cs;

    private ThreadService ts;

    private OpService ops;

    private List<EventSubscriber<?>> subs;


    public OpenFilePanel(final CommandService cs, final EventService es, final ThreadService ts, final OpService ops, final BigDataViewerUI bdvUI) {
        this.es = es;
        this.cs = cs;
        this.ts = ts;
        this.ops = ops;
        subs = this.es.subscribe(this);
        textField = new JTextField("", 20);
        browse = new JButton("Browse");
        setupBrowseButton();
        open = new JButton("Open");
        setupOpenButton(bdvUI);
        setupPanel();
        this.add(textField);
        this.add(browse, "wrap");
        this.add(open, "wrap");


    }



    private void setupBrowseButton() {
        browse.setBackground(Color.WHITE);
        browse.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == browse) {
                    int returnVal = fc.showOpenDialog(null);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File file = fc.getSelectedFile();
                        textField.setText(file.getAbsolutePath());
                    }

                }
            }
        });
    }


    private void setupOpenButton(final BigDataViewerUI bdvui) {
        open.setBackground(Color.WHITE);
        open.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == open) {
                    openImage(bdvui);
                    bdvui.getRegisterImagesPanel().addImage(String.valueOf(textField.getText()));

                }
            }


        });

    }

    private void openImage(final BigDataViewerUI bdvUI) {
        open.setEnabled(false);
        Context context = new Context();
        DatasetIOService ioService = context.service(DatasetIOService.class);
        try {
            Dataset img = ioService.open(textField.getText());
            bdvUI.addImage(img, textField.getText(), Color.white);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            open.setEnabled(true);
        }
    }


    private void setupPanel() {
        this.setBackground(Color.white);
        this.setBorder(new TitledBorder(""));
        this.setLayout(new MigLayout("fillx", "", ""));
    }



}

