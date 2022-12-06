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

package ij_plugins.javacv.util

import ij.gui.DialogListener
import ij.plugin.filter.ExtendedPlugInFilter
import ij.process.ImageProcessor
import ij.{IJ, ImagePlus}

/**
 * Helper for implementing `ExtendedPlugInFilter`.
 */
trait ExtendedPlugInFilterTrait extends ExtendedPlugInFilter with DialogListener {

  private var _imp: ImagePlus = _
  private var _nPasses        = 0
  private var _currentPass    = 0

  /**
   * Image container that is the input to this plugin.
   */
  protected final def imp: ImagePlus = _imp

  protected final def nPasses: Int = _nPasses

  protected final def currentPass: Int = _currentPass

  /**
   * Flags (plugin capabilities, like DOES_8G) returned by `setup` method.
   */
  protected def Flags: Int

  /**
   * Plugin name, used in dialog titles and status messages.
   */
  protected def Title: String

  override def setup(arg: String, imp: ImagePlus): Int = {
    if ("final" == arg) {
      // Cleanup
      _imp = null
      _nPasses = 0
      _currentPass = 0
    } else {
      // Prepare for run
      _imp = imp
    }
    Flags
  }

  override def run(ip: ImageProcessor): Unit = {

    _currentPass += 1
    if (_nPasses > 1) {
      IJ.showProgress(_currentPass - 1, _nPasses)
      IJ.showStatus(Title + " - processing: " + currentLabel)
    }

    // Process image and send local results to the results table
    process(ip)

    if (_nPasses > 1) {
      IJ.showProgress(_currentPass, _nPasses)
      IJ.showStatus(Title)
    }
  }

  /**
   * Method that does actual processing of the image.
   *
   * @param ip input image.
   */
  protected def process(ip: ImageProcessor): Unit

  override def setNPasses(nPasses: Int): Unit = {
    _currentPass = 0
    _nPasses = nPasses
    if (_nPasses > 1) {
      IJ.showProgress(_currentPass, _nPasses)
      IJ.showStatus(Title)
    }
  }

  /**
   * Current image or slice label. If only one slice is processed it is the image title, if more than one slice
   * is processed it is the slice label.
   */
  protected final def currentLabel: String = {
    if (_nPasses < 2) {
      _imp.getTitle
    } else {
      // Extract first part of the stack label that corresponds to image name.
      // The rest, after new line character, contains additional image info
      _imp.getStack.getSliceLabel(_currentPass).split("\\n").head
    }
  }
}
