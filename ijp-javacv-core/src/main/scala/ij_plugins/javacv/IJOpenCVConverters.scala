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

import ij.process.*
import ij.{ImagePlus, ImageStack}
import org.bytedeco.javacv.{Frame, OpenCVFrameConverter}
import org.bytedeco.opencv.opencv_core.*

import java.awt.image.*
import scala.util.Using

/**
 * Converts between OpenCV and ImageJ data representations
 */
object IJOpenCVConverters {

  /**
   * Convert OpenCV `Mat` to ImageJ's ImageProcessor.
   *
   * Depending on the type input image different instance
   * of `ImageProcessor` will be created, for color images it will be `ColorProcessor`, for 8-bit gray level `ByteProcessor`.
   * Other pixel types are currently not supported.
   *
   * @param image input image.
   */
  def toImageProcessor(image: Mat): ImageProcessor = {
    Using.Manager { use =>
      val converter = use(new OpenCVFrameConverter.ToMat())
      val frame     = use(converter.convert(image))
      new ImageProcessorFrameConverter().convert(frame)
    }.get
  }

  /**
   * Convert OpenCV `Mat` to ImageJ's `ColorProcessor`.
   *
   * @param image input image.
   * @throws IllegalArgumentException if `Mat` is not a color image.
   */
  def toColorProcessor(image: Mat): ColorProcessor = {
    val ip = toImageProcessor(image)
    ip match {
      case colorProcessor: ColorProcessor => colorProcessor
      case _                              => throw new IllegalArgumentException("Input image is not a color image.")
    }
  }

  /**
   * Convert OpenCV `Mat` to ImageJ's ImagePlus.
   *
   * @param image input image.
   */
  def toImagePlus(image: Mat): ImagePlus = {
    Using.Manager { use =>
      val converter = use(new OpenCVFrameConverter.ToMat())
      val frame     = use(converter.convert(image))
      new ImagePlusFrameConverter().convert(frame)
    }.get
  }

  /** Convert ImageJ's `ImageProcessor` to `BufferedImage`. */
  def toBufferedImage(ip: ImageProcessor): BufferedImage = {

    val BP = classOf[ByteProcessor]
    val CP = classOf[ColorProcessor]
    ip.getClass match {
      case BP => ip.getBufferedImage
      case CP =>
        // Create BufferedImage of RGB type that JavaCV likes
        val dest = new BufferedImage(ip.getWidth, ip.getHeight, BufferedImage.TYPE_3BYTE_BGR)
        // Easiest way to transfer the data is to draw the input image on the output image,
        // This handles all needed color representation conversions, since both are variants of
        val g = dest.getGraphics
        g.drawImage(ip.getBufferedImage, 0, 0, null)
        dest
      case _ => throw new IllegalArgumentException("Unsupported ImageProcessor type: " + ip.getClass)
    }

  }

  /**
   * Convert BufferedImage to ImageJ's ImageProcessor.
   *
   * Based on net.sf.ij.jaiio.ImagePlusCreator#createProcessor
   *
   * @param image input image
   * @return ImageProcessor created from input BufferedImage.
   * @throws IllegalArgumentException when enable to create ImagePlus.
   */
  def toImageProcessor(image: BufferedImage): ImageProcessor = {
    val raster     = image.getRaster
    val colorModel = image.getColorModel
    val dataBuffer = raster.getDataBuffer
    val numBanks   = dataBuffer.getNumBanks
    if (numBanks > 1 && colorModel == null) {
      throw new IllegalArgumentException("Don't know what to do with image with no color model and multiple banks.")
    }

    val sampleModel = raster.getSampleModel
    if (numBanks > 1 || sampleModel.getNumBands > 1) {
      val bi = new BufferedImage(colorModel, raster, false, null)
      new ColorProcessor(bi)
    } else if (sampleModel.getSampleSize(0) < 8) {
      val bi = new BufferedImage(colorModel, raster, false, null)
      val w  = image.getWidth
      val h  = image.getHeight
      bi.getType match {
        case BufferedImage.TYPE_BYTE_GRAY => new ByteProcessor(bi)
        case BufferedImage.TYPE_BYTE_BINARY =>
          val bp   = new ByteProcessor(w, h)
          val data = bi.getData
          val p    = Array(0)
          for (y <- 0 until h; x <- 0 until w) {
            data.getPixel(x, y, p)
            bp.set(x, y, p(0))
          }
          bp.setColorModel(colorModel)
          bp
        case _ => throw new IllegalArgumentException("Unable to process buffered image of type: " + bi.getType)
      }
    } else {
      if (dataBuffer.getOffset != 0) {
        throw new IllegalArgumentException("Expecting BufferData with no offset.")
      }
      val w = image.getWidth
      val h = image.getHeight
      import java.awt.image.DataBuffer as DB
      dataBuffer.getDataType match {
        case DB.TYPE_BYTE =>
          new ByteProcessor(w, h, dataBuffer.asInstanceOf[DataBufferByte].getData, colorModel)
        case DB.TYPE_USHORT =>
          new ShortProcessor(w, h, dataBuffer.asInstanceOf[DataBufferUShort].getData, colorModel)
        case DB.TYPE_SHORT =>
          val pixels = dataBuffer.asInstanceOf[DataBufferShort].getData
          pixels.foreach(_ + 32768)
          new ShortProcessor(w, h, pixels, colorModel)
        case DB.TYPE_INT =>
          new FloatProcessor(w, h, dataBuffer.asInstanceOf[DataBufferInt].getData)
        case DB.TYPE_FLOAT =>
          new FloatProcessor(w, h, dataBuffer.asInstanceOf[DataBufferFloat].getData, colorModel)
        case DB.TYPE_DOUBLE =>
          new FloatProcessor(w, h, dataBuffer.asInstanceOf[DataBufferDouble].getData)
        case DB.TYPE_UNDEFINED =>
          throw new IllegalArgumentException("Pixel type is undefined.")
        case _ =>
          throw new IllegalArgumentException("Unrecognized DataBuffer data type")
      }
    }
  }

  /** Convert `ImageProcessor` to `Mat`. */
  def toMat(src: ImageProcessor): Mat = {
    require(src != null)
    Using(new ImageProcessorFrameConverter().convert(src)) { frame =>
      toMat(frame)
    }.get
  }

  /** Convert array of `ImageProcessor`s to `Mat`. A channel will be created for each image. */
  def toMat[T <: ImageProcessor](fps: Array[T]): Mat = {
    if (fps.isEmpty) {
      new Mat()
    } else {
      toMat(new ImagePlus("", toStack(fps)))
    }
  }

  /** Convert `ImagePlus` to `Mat`. If there re multiple slices they will be converted to channels. */
  def toMat(src: ImagePlus): Mat = {
    require(src != null)
    Using(new ImagePlusFrameConverter().convert(src)) { frame =>
      toMat(frame)
    }.get
  }

  private def toMat(frame: Frame): Mat = {
    Using(new OpenCVFrameConverter.ToMat()) { converter =>
      // Converted returns reference to internal `mat` wrapper, it may get deallocated when it gets out of scope
      //   see https://github.com/bytedeco/javacpp-presets/issues/979
      val mat = converter.convert(frame)
      mat.clone()
    }.get
  }

  /**
   * Create ImageStack from an array of `ImageProcessor`s
   *
   * @param ips input array
   * @tparam T Type of ImageProcessor
   * @return image stack
   */
  def toStack[T <: ImageProcessor](ips: Array[T]): ImageStack = {
    if (ips.isEmpty) {
      new ImageStack()
    } else {
      val stack = new ImageStack(ips(0).getWidth, ips(0).getHeight)
      ips.foreach(ip => stack.addSlice(ip))
      stack
    }
  }
}
