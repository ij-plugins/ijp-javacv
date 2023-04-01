/*
 * Image/J Plugins
 * Copyright (C) 2002-2023 Jarek Sacha
 * Author's email: jpsacha at gmail dot com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Latest release available at http://sourceforge.net/projects/ij-plugins/
 */

import ij.IJ;
import ij_plugins.javacv.IJOpenCVConverters;
import org.bytedeco.opencv.opencv_mcc.CCheckerDetector;

import static ij_plugins.javacv.mcc.Utils.toColorProcessor;
import static ij_plugins.javacv.mcc.Utils.toPolygonRoi;
import static org.bytedeco.opencv.global.opencv_mcc.MCC24;

public class ColorChecker_Detector {

    public static void main(String[] args) {

        // Load image
        var imp = IJ.openImage("data/IMG_0903_025p-crop.png");

        if (imp != null) {
            imp.show();

            // ColorChecker detector requires RGB image as input
            var cp = toColorProcessor(imp);

            // Convert RGB image to OpenCV representation
            var mat = IJOpenCVConverters.toMat(cp);

            // Create chart detector
            var detector = CCheckerDetector.create();

            // Detect chart and display chart outline as ROI
            if (detector.process(mat, MCC24)) {
                var checker = detector.getBestColorChecker();
                imp.setRoi(toPolygonRoi(checker.getBox()));
            } else
                IJ.showMessage("ColorChecker not detected");
        } else
            IJ.noImage();
    }
}
