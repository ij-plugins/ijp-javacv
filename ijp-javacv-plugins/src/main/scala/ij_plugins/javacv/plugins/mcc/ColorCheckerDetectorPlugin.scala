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

package ij_plugins.javacv.plugins.mcc

import ij.gui.{GenericDialog, PolygonRoi, Roi}
import ij.plugin.PlugIn
import ij.process.ColorProcessor
import ij.{IJ, ImagePlus}
import ij_plugins.javacv.IJOpenCVConverters
import ij_plugins.javacv.mcc.Utils.scalar
import ij_plugins.javacv.plugins.mcc.Utils.toColorProcessor
import ij_plugins.javacv.util.{IJPUtils, OpenCVUtils}
import org.bytedeco.opencv.opencv_core.Point2fVector
import org.bytedeco.opencv.opencv_mcc.{CCheckerDetector, CCheckerDraw, DetectorParameters}

object ColorCheckerDetectorPlugin {

  private var config = Config()

  private case class Config(
    chartType: ChartType = ChartType.MCC24,
    numberOfCharts: Int = 1,
    detectorParams: DetectorParameters = DetectorParameters.create()
  )

}

class ColorCheckerDetectorPlugin extends PlugIn {
  import ColorCheckerDetectorPlugin.*

  private val Title                  = "ColorChecker Detector"
  private var imp: Option[ImagePlus] = None

  override def run(arg: String): Unit = {
    imp = Option(IJ.getImage)
    imp match {
      case Some(imp) =>
        toColorProcessor(imp) match {
          case Right(cp)     => askForOptionsAndProcess(cp)
          case Left(message) => IJ.error(Title, message)
        }
      case None =>
        IJ.noImage()
    }
  }

  private def askForOptionsAndProcess(cp: ColorProcessor): Unit = {
    val About = "" +
      "Find position of a color chart in an image. <br>" +
      "Image can be a RGB color or stack of 3 gray images (interpreted as colors R, G, and B). <br>" +
      "  <tt>MCC24</tt> - Standard Macbeth Chart with 24 squares <br>" +
      "  <tt>SG140</tt> - DigitalSG with 140 squares <br>" +
      "  <tt>VINYL18</tt> - DKK color chart with 12 squares and 6 rectangle"
    val gd = new GenericDialog(Title, IJ.getInstance)
    gd.addPanel(IJPUtils.createHeaderAWT(Title, About))
    gd.addChoice("Chart Type", ChartType.values.map(_.name).toArray, config.chartType.name)
    gd.addHelp("https://github.com/ij-plugins/ijp-javacv/wiki/ColorChecker-Detector-Plugin")

    gd.showDialog()

    if (gd.wasOKed()) {
      val chartType = ChartType.withName(gd.getNextChoice)
      config = config.copy(chartType = chartType)
      run(cp)
    }
  }

  private def run(cp: ColorProcessor): Unit = {
    val matSrc = IJOpenCVConverters.toMat(cp)

    val ccheckerDetector = CCheckerDetector.create()
    val hasChart =
      ccheckerDetector.process(matSrc, config.chartType.value, config.numberOfCharts, true, config.detectorParams)

    if (hasChart) {
      val checker = ccheckerDetector.getListColorChecker.get(0)

      val box: Point2fVector = checker.getBox
      val nPoints: Int       = box.size().toInt
      val xPoints            = new Array[Float](nPoints)
      val yPoints            = new Array[Float](nPoints)
      for (i <- 0 until nPoints) {
        val p2f = box.get(i)
        xPoints(i) = p2f.x()
        yPoints(i) = p2f.y()
      }

      val roi = new PolygonRoi(xPoints, yPoints, nPoints, Roi.POLYGON)
      imp.foreach(_.setRoi(roi))

      if (IJ.debugMode) {
        CCheckerDraw
          .create(checker, scalar(255, 255, 255), 1)
          .draw(matSrc)

        OpenCVUtils.show(matSrc, "Detected chart tiles")
      }
    } else
      IJ.showMessage(Title, "No chart detected")
  }

}
