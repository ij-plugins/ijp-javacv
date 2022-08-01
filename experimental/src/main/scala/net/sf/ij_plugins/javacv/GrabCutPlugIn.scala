/*
 * Copyright (c) 2011-2022 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package net.sf.ij_plugins.javacv

import ij.gui.{NonBlockingGenericDialog, Overlay, Roi}
import ij.plugin.PlugIn
import ij.process.ImageProcessor
import ij.{IJ, ImagePlus}

import java.awt.Color

class GrabCutPlugIn extends PlugIn {

  private val Title = "Grab Cut Segmentation"

  private var imp: Option[ImagePlus] = None

  private var gci: Option[GrubCutInteraction] = None

  private var frgROIAddition: Option[Roi] = None
  private var bkgROIAddition: Option[Roi] = None

  private val bkgColor: Color      = Color.BLUE
  private val bkgTransparency: Int = 128

  // TODO: Option to set number of iterations
  override def run(arg: String): Unit = {
    imp = Option(IJ.getImage)
    imp match {
      case Some(imp) =>
        Option(imp.getRoi) match {
          case Some(roi) =>
            interact(imp.getProcessor, roi)
          case None =>
            IJ.error(Title, "ROI enclosing the object is required.")
        }
      case None =>
        IJ.noImage()
    }
  }

  def interact(ip: ImageProcessor, roi: Roi): Unit = {
    IJ.showStatus(s"$Title: Starting")

    gci = Option(new GrubCutInteraction(ip))

    IJ.showStatus(s"$Title: Run initial segmentation")
    gci.foreach(_.initialRun(roi.getBounds))
    updateDisplay()

    val dialog = new NonBlockingGenericDialog(Title)
    dialog.addButton(
      "Add to Foreground",
      (_) => {
        imp.flatMap(im => Option(im.getRoi)) match {
          case Some(r) =>
            frgROIAddition = IJUtils.add(frgROIAddition, r)
            updateDisplay()
          case None =>
            IJ.error(Title, "Selection required")
        }
      }
    )

    dialog.addButton(
      "Add to Background",
      (_) => {
        imp.flatMap(im => Option(im.getRoi)) match {
          case Some(r) =>
            bkgROIAddition = IJUtils.add(bkgROIAddition, r)
            updateDisplay()
          case None =>
            IJ.error(Title, "Selection required")
        }
      }
    )

    dialog.addButton(
      "Update GrubCut",
      (_) => {
        IJ.showStatus(s"$Title: Update GrubCut")

        // Add BKG and FRG modifications

        frgROIAddition.foreach(roi => gci.foreach(_.addToForeground(roi)))
        frgROIAddition = None

        bkgROIAddition.foreach(roi => gci.foreach(_.addToBackground(roi)))
        bkgROIAddition = None

        IJ.showStatus(s"$Title: Run GrubCut")
        gci.foreach(_.update())

        updateDisplay()
      }
    )

    dialog.showDialog()

    if (dialog.wasOKed()) {
      showFinalResult()
    } else {
      // Restore original ROI
      imp.foreach(_.setOverlay(null))
      imp.foreach(_.setRoi(roi))
    }
  }

  private def updateDisplay(): Unit = {

    val bkgOverlay = new Overlay()

    IJ.showStatus(s"$Title: Display results")
    gci.foreach { gc =>
      IJUtils
        .add(gc.backgroundRoi, gc.probableBackgroundRoi)
        .foreach { roi =>
          roi.setStrokeColor(bkgColor)
          roi.setStrokeWidth(2)
          bkgOverlay.add(roi, "All background outline")

          addFill(bkgOverlay, roi, bkgColor, bkgTransparency, "All background fill")
        }
    }

    frgROIAddition.foreach { r =>
      r.setStrokeColor(Color.GREEN)
      bkgOverlay.add(r, "Foreground addition")
      addFill(bkgOverlay, r, r.getStrokeColor, bkgTransparency, "Foreground addition fill")
    }
    bkgROIAddition.foreach { r =>
      r.setStrokeColor(Color.RED)
      bkgOverlay.add(r, "Background addition")
      addFill(bkgOverlay, r, r.getStrokeColor, bkgTransparency, "Background addition fill")
    }

    imp.foreach(_.setRoi(null.asInstanceOf[Roi]))
    imp.foreach(_.setOverlay(bkgOverlay))
  }

  private def addFill(overlay: Overlay, roi: Roi, color: Color, transparency: Int, name: String): Unit = {
    val roiFill = roi.clone().asInstanceOf[Roi]
    roiFill.setFillColor(new Color(color.getRed, color.getGreen, color.getBlue, transparency))

    overlay.add(roiFill, name)
  }

  private def showFinalResult(): Unit = {
    IJ.showStatus(s"$Title: Display final results")

    gci.foreach { gc =>
      IJUtils.add(gc.foregroundRoi, gc.probableForegroundRoi) match {
        case Some(roi) => imp.foreach(_.setRoi(roi))
        case None      => imp.foreach(_.setRoi(null.asInstanceOf[Roi]))
      }
    }

    imp.foreach(_.setOverlay(null))

    IJ.showStatus(s"$Title: Result")
  }
}
