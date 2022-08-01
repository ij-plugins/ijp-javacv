/*
 * Copyright (c) 2011-2022 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package net.sf.ij_plugins.javacv

import ij.ImagePlus
import ij.gui.{Roi, ShapeRoi}
import ij.plugin.filter.ThresholdToSelection
import ij.process.{ByteProcessor, ImageProcessor}

object IJUtils {

  def getMask(roi: Roi, width: Int, height: Int, value: Int): ByteProcessor = {
    val mask = new ByteProcessor(width, height)
    mask.setValue(value)

    Option(roi) match {
      case Some(roi) => mask.fill(roi)
      case None      => mask.fill()
    }
    mask
  }

  def getMask(roi: Roi, ip: ImageProcessor, value: Int): ByteProcessor = {
    getMask(roi, ip.getWidth, ip.getHeight, value)
  }

  def toRoi(src: ByteProcessor, thresholdMin: Int = 0, thresholdMax: Int = 1): Roi = {
    val ip = src.convertToByteProcessor(false)
    ip.setThreshold(thresholdMin, thresholdMax)
    new ThresholdToSelection().convert(ip)
  }


  def add(r1: Roi, r2: Roi): Option[Roi] =
    add(Option(r1), r2)

  def add(r1: Option[Roi], r2: Roi): Option[Roi] = {
    (r1, Option(r2)) match {
      case (Some(rr1), Some(rr2)) =>
        val dst = new ShapeRoi(rr1)
        dst.or(new ShapeRoi(rr2))
        Option(dst)
      case (Some(rr1), None) =>
        Option(rr1)
      case (None, Some(rr2)) =>
        Option(rr2)
      case (None, None) =>
        None
    }
  }
}
