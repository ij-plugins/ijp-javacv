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

package ij_plugins.javacv.imgcodecs

import ij_plugins.javacv.util.OpenCVUtils
import OpenCVUtils.loadMulti
import org.bytedeco.javacpp.IntPointer
import org.bytedeco.opencv.global.opencv_core.split
import org.bytedeco.opencv.global.opencv_imgcodecs
import org.bytedeco.opencv.opencv_core.MatVector

import java.io.File

/**
 * Example of writing multiple images to TIFF file.
 */
object ReadWriteImageApp extends App {

  val srcFile = new File("../test/data/mri-stack.tif").getCanonicalFile
  assert(srcFile.exists())

  val mat = loadMulti(srcFile)

  OpenCVUtils.printInfo(mat, srcFile.getCanonicalPath)

  val dstFile = new File("../test/data/mri-stack_imwite_eval.tif").getCanonicalFile

  val dstMatV = new MatVector()
  split(mat, dstMatV)
  val params = toIntPointerArray(Array[Int](opencv_imgcodecs.IMWRITE_TIFF_COMPRESSION))
  val ok     = opencv_imgcodecs.imwritemulti(dstFile.getAbsolutePath, dstMatV, params)

  println(s"imwritemulti: $ok")

  def toIntPointerArray(src: Array[Int]): IntPointer = {
    val dst = new IntPointer(src.length.toLong)
    for (i <- src.indices) dst.put(i.toLong, src(i))
    dst
  }

}
