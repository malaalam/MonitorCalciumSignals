package bdv.ui.panel;

import Jama.Matrix;
import ij.IJ;
import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;
import org.scijava.Context;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;


public class ApplyAffineTransform<T extends NumericType<T>> {

    static private Img<UnsignedShortType> performAffine(Dataset imgSource, Dataset imgTarget, Matrix affineInv) {

        System.out.println("start calculation");
        RandomAccess<RealType<?>> sourceRandomAccess = Views.extendZero((RandomAccessibleInterval) imgSource).randomAccess();
        ImgFactory imgFactory = new ArrayImgFactory<>(new UnsignedShortType());
        Img<UnsignedShortType> target = imgFactory.create(imgTarget);
        Cursor<UnsignedShortType> targetCursor = target.localizingCursor();

        int xTarget = 0;
        int yTarget = 0;
        int zTarget = 0;
        Matrix targetPixel = new Matrix(4, 1);
        Matrix sourcePixel = new Matrix(4, 1);
        while (targetCursor.hasNext()) {

            targetCursor.fwd();
            xTarget = targetCursor.getIntPosition(0);
            yTarget = targetCursor.getIntPosition(1);
            zTarget = targetCursor.getIntPosition(2);
            targetPixel.set(0, 0, xTarget);
            targetPixel.set(1, 0, yTarget);
            targetPixel.set(2, 0, zTarget);
            targetPixel.set(3, 0, 1);
            sourcePixel = affineInv.times(targetPixel);
            int[] pos = new int[3];
            pos[0] = (int) sourcePixel.get(0, 0);
            pos[1] = (int) sourcePixel.get(1, 0);
            pos[2] = (int) sourcePixel.get(2, 0);
            sourceRandomAccess.setPosition(pos);
            targetCursor.get().set((int) sourceRandomAccess.get().getRealDouble());
        }
        return target;


    }

    public static void main(String... args) {


        Context context = new Context();
        DatasetIOService ioService = context.service(DatasetIOService.class);


        try {

            Instant start = Instant.now();
            Dataset imgsource = ioService.open("/home/manan/Desktop/08_SampleData/02_Images/06_Raghav_Calcium/GCaMP6s_Larva/tifs/CM02/390.tif");
            Dataset imgtarget = ioService.open("/home/manan/Desktop/08_SampleData/02_Images/06_Raghav_Calcium/GCaMP6s_Larva/tifs/CM03/390.tif");
            double[][] vals = {{1, 0, 0, 0}, {0, 1, 0, 0}, {0, 0, 1, 0}, {0, 0, 0, 1}};
            Matrix affineInv = new Matrix(vals);

            Img<UnsignedShortType> imgTransformed = (Img<UnsignedShortType>) performAffine(imgsource, imgtarget, affineInv);
            IJ.save(ImageJFunctions.wrap(imgTransformed, "transformed"), "/home/manan/Desktop/imgSourceTransformed.tif");
            Instant finish = Instant.now();

            long timeElapsed = Duration.between(start, finish).toMillis();  //in millis
            System.out.println(timeElapsed);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}