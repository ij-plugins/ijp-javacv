/*
 * Copyright (c) 2011-2022 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package net.sf.ij_plugins.javacv

import ij.gui.Roi
import ij.plugin.PlugIn
import ij.process.ImageProcessor
import ij.{IJ, ImagePlus}
import net.sf.ij_plugins.javacv.util.OpenCVUtils.toMat8U
import org.bytedeco.opencv.global.opencv_imgproc.*
import org.bytedeco.opencv.opencv_core.{Mat, Rect}

class GrabCutPlugIn extends PlugIn {

  private val Title = "Grab Cut Segmentation"

//  private var impOpt: Option[ImagePlus] = None

//  override def setup(arg: String, imp: ImagePlus): Int = {
//    impOpt = Option(imp)
//    PlugInFilter.DOES_8G + PlugInFilter.DOES_16 + PlugInFilter.DOES_32 + PlugInFilter.DOES_RGB
//      + PlugInFilter.ROI_REQUIRED
//  }

  override def run(arg: String): Unit = {
    Option(IJ.getImage) match {
      case Some(imp) =>
        Option(imp.getRoi) match {
          case Some(roi) =>
            val dst = run(imp.getProcessor, roi)
            new ImagePlus(s"${imp.getShortTitle}+GrabCut", dst).show()
          case None =>
            IJ.error(Title, "ROI enclosing the object is required.")
        }
      case None =>
        IJ.noImage()
    }
  }

  private def run(ip: ImageProcessor, roi: Roi): ImageProcessor = {

    val image = IJOpenCVConverters.toMat(ip)

    // Define bounding rectangle, pixels outside this rectangle will be labeled as background.
//    val rectangle = new Rect(10, 100, 380, 180)
    val bounds    = roi.getBounds
    val rectangle = new Rect(bounds.x, bounds.y, bounds.width, bounds.height)

    val result    = new Mat()
    val iterCount = 5
    val mode      = GC_INIT_WITH_RECT

    // Need to allocate arrays for temporary data
    val bgdModel = new Mat()
    val fgdModel = new Mat()

    // GrabCut segmentation
    grabCut(image, result, rectangle, bgdModel, fgdModel, iterCount, mode)

    // Prepare image for display: extract foreground
    threshold(result, result, GC_PR_FGD - 0.5, GC_PR_FGD + 0.5, THRESH_BINARY)

    val r2 = toMat8U(result)

    val dst = IJOpenCVConverters.toImageProcessor(r2)
    dst
  }
}
