/*
 * Image/J Plugins
 * Copyright (C) 2002-2021 Jarek Sacha
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

package net.sf.ij_plugins.javacv

import ij.process.{ByteProcessor, ColorProcessor}
import ij.{IJ, ImagePlus}
import net.sf.ij_plugins.javacv.util.OpenCVUtils.loadMulti
import org.bytedeco.javacv.{Frame, OpenCVFrameConverter}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

import java.io.File
import scala.util.Using

class ImagePlusFrameConverterTest extends AnyFlatSpec {

  it should "convert GRAY8 to Frame" in {

    val bp = new ByteProcessor(13, 17)
    val imp = new ImagePlus("", bp)

    val converter = new ImagePlusFrameConverter()

    val frame = converter.convert(imp)

    frame.imageDepth should be(Frame.DEPTH_UBYTE)
    frame.imageWidth should be(13)
    frame.imageHeight should be(17)
    frame.imageChannels should be(1)
  }


  it should "convert RGB to Frame" in {

    val cp = new ColorProcessor(13, 17)
    val imp = new ImagePlus("", cp)

    val converter = new ImagePlusFrameConverter()

    val frame = converter.convert(imp)

    frame.imageDepth should be(Frame.DEPTH_UBYTE)
    frame.imageWidth should be(13)
    frame.imageHeight should be(17)
    frame.imageChannels should be(3)
  }


  it should "convert Frame to GRAY8" in {

    val frame = new Frame(13, 17, Frame.DEPTH_UBYTE, 1)

    val converter = new ImagePlusFrameConverter()

    val imp = converter.convert(frame)

    imp.getType should be(ImagePlus.GRAY8)
    imp.getWidth should be(13)
    imp.getHeight should be(17)
    imp.getStackSize should be(1)
  }


  it should "convert Frame to GRAY8 Stack (from disk)" in {
    val srcFile = new File("../test/data/mri-stack_c.tif").getCanonicalFile
    assert(srcFile.exists())

    val srcMat = loadMulti(srcFile)
    srcMat should not be (null)

    Using.resource(new OpenCVFrameConverter.ToMat()) { openCVConverter =>
      Using.resource(openCVConverter.convert(srcMat)) { srcFrame =>

        srcFrame should not be (null)

        val converter = new ImagePlusFrameConverter()
        val dstImp = converter.convert(srcFrame)
        dstImp should not be (null)

        dstImp.getType should be(ImagePlus.GRAY8)
        dstImp.getWidth should be(186)
        dstImp.getHeight should be(226)
        dstImp.getStackSize should be(27)

        // Read using ImageJ so we can compare conversion
        val srcImp = IJ.openImage(srcFile.getCanonicalPath)
        // Sanity checks
        srcImp should not be (null)
        srcImp.getWidth should be(186)
        srcImp.getHeight should be(226)
        srcImp.getStackSize should be(27)

        val srcImageArray = srcImp.getStack.getImageArray
        val dstImageArray = dstImp.getStack.getImageArray
        for (i <- 0 until srcImp.getStackSize) {
          srcImageArray(i) should be(dstImageArray(i))
        }
      }
    }

  }
}
