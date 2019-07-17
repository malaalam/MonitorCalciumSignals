package bdv.ui.panel;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.io.Opener;
import net.imglib2.type.numeric.RealType;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import selection.Select_Points;


@Plugin(type = Command.class, menuPath = "Plugins > Get Correspondences")
public class GetCorrespondences<T extends RealType<T>> implements Command {

    public static int defaultImg1 = 0;
    public static int defaultImg2 = 1;

    @Override
    public void run() {

        final int[] idList = WindowManager.getIDList();

        if (idList == null || idList.length < 2) {
            IJ.error("You need at least two open images.");
            return;
        }

        final String[] imgList = new String[idList.length];

        for (int i = 0; i < idList.length; ++i)
            imgList[i] = WindowManager.getImage(idList[i]).getTitle();

        if (defaultImg1 >= imgList.length || defaultImg2 >= imgList.length) {
            defaultImg1 = 0;
            defaultImg2 = 1;
        }

        final GenericDialog gd = new GenericDialog("Apply Thin Plate Splines");

        gd.addChoice("First Image", imgList, imgList[defaultImg1]);
        gd.addChoice("Second Image", imgList, imgList[defaultImg2]);
        gd.showDialog();

        if (gd.wasCanceled())
            return;

        final ImagePlus imp1 = WindowManager.getImage(idList[defaultImg1 = gd.getNextChoiceIndex()]);
        final ImagePlus imp2 = WindowManager.getImage(idList[defaultImg2 = gd.getNextChoiceIndex()]);

        new Select_Points(imp1, imp2).run(null);


    }

    public static void main(String[] args) {
        new ImageJ();
        final Opener open = new Opener();
        final ImagePlus imp1=open.openImage("/home/manan/Desktop/08_SampleData/02_Images/06_Raghav_Calcium/GCaMP6s_Larva/tifs/CM02/390.tif");
        final ImagePlus imp2=open.openImage("/home/manan/Desktop/08_SampleData/02_Images/06_Raghav_Calcium/GCaMP6s_Larva/tifs/CM03/390.tif");
        imp1.show();
        imp2.show();
        new Select_Points(imp1, imp2).run(null);


    }
}
