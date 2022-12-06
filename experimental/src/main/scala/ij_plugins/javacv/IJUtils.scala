/*
 * Image/J Plugins
 * Copyright (C) 2002-2022 Jarek Sacha
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

package ij_plugins.javacv

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
