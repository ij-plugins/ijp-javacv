/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package net.sf.ijplugins.javacv.util

import java.awt._
import java.awt.geom.{AffineTransform, Ellipse2D, Point2D}
import java.awt.image.BufferedImage
import java.io.{File, IOException}

import ij.ImagePlus
import ij.gui.{Overlay, ShapeRoi}
import ij.process.ColorProcessor
import javax.swing.WindowConstants
import org.bytedeco.javacpp.DoublePointer
import org.bytedeco.javacpp.indexer.{DoubleIndexer, FloatIndexer}
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat
import org.bytedeco.javacv.{CanvasFrame, Java2DFrameConverter}
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core.{Point, _}

import scala.math.round


/** Helper methods that simplify use of OpenCV API. */
object OpenCVUtils {
  /**
   * Initialize loading of OpenCV libraries. Generally those libraries are loaded automatically,
   * but in some situations it may not happen.
   *
   * Some part of the JavaCPP may not initialize automatically resulting in errors like:
   * {{{
   *   java.lang.RuntimeException: No native JavaCPP library in memory. (Has Loader.load() been called?)
   *   ...
   *   Caused by: java.lang.UnsatisfiedLinkError: org.bytedeco.javacpp.BytePointer.allocateArray(J)V
   * }}}
   *
   * See JavaCV bug [[https://github.com/bytedeco/javacv/issues/1305]]
   *
   * @return current version of the OpenCV library
   */
  def initJavaCV(): String = CV_VERSION


  /** Load an image.
   *
   * @param flags Flags specifying the color type of a loaded image:
   *              <ul>
   *              <li> `>0` Return a 3-channel color image</li>
   *              <li> `=0` Return a gray scale image</li>
   *              <li> `<0` Return the loaded image as is. Note that in the current implementation
   *              the alpha channel, if any, is stripped from the output image. For example, a 4-channel
   *              RGBA image is loaded as RGB if the `flags` is greater than 0.</li>
   *              </ul>
   *              Default is gray scale.
   * @return loaded image
   * @throws IOException if image cannot be loaded
   */
  def load(file: File, flags: Int = IMREAD_COLOR): Mat = {
    // Read input image
    val image = imread(file.getAbsolutePath, flags)
    if (image.empty()) {
      throw new IOException("Couldn't load image: " + file.getAbsolutePath)
    }
    image
  }

  /** Load an image and show in a CanvasFrame.
   *
   * @param flags Flags specifying the color type of a loaded image:
   *              <ul>
   *              <li> `>0` Return a 3-channel color image</li>
   *              <li> `=0` Return a gray scale image</li>
   *              <li> `<0` Return the loaded image as is. Note that in the current implementation
   *              the alpha channel, if any, is stripped from the output image. For example, a 4-channel
   *              RGBA image is loaded as RGB if the `flags` is greater than 0.</li>
   *              </ul>
   *              Default is gray scale.
   * @return loaded image
   * @throws IOException if image cannot be loaded
   */
  def loadAndShow(file: File, flags: Int = IMREAD_COLOR): Mat = {
    // Read input image
    val image = load(file, flags)
    show(image, file.getName)
    image
  }

  /** Load an image and show in a CanvasFrame. If image cannot be loaded the application will exit with code 1.
   *
   * @param flags Flags specifying the color type of a loaded image:
   *              <ul>
   *              <li> `>0` Return a 3-channel color image</li>
   *              <li> `=0` Return a gray scale image</li>
   *              <li> `<0` Return the loaded image as is. Note that in the current implementation
   *              the alpha channel, if any, is stripped from the output image. For example, a 4-channel
   *              RGBA image is loaded as RGB if the `flags` is greater than 0.</li>
   *              </ul>
   *              Default is gray scale.
   * @return loaded image
   */
  def loadAndShowOrExit(file: File, flags: Int = IMREAD_COLOR): Mat = {
    // Read input image
    val image = loadOrExit(file, flags)
    show(image, file.getName)
    image
  }

  /** Load an image. If image cannot be loaded the application will exit with code 1.
   *
   * @param flags Flags specifying the color type of a loaded image:
   *              <ul>
   *              <li> `>0` Return a 3-channel color image</li>
   *              <li> `=0` Return a gray scale image</li>
   *              <li> `<0` Return the loaded image as is. Note that in the current implementation
   *              the alpha channel, if any, is stripped from the output image. For example, a 4-channel
   *              RGBA image is loaded as RGB if the `flags` is greater than 0.</li>
   *              </ul>
   *              Default is gray scale.
   * @return loaded image
   */
  def loadOrExit(file: File, flags: Int = IMREAD_COLOR): Mat = {
    // Read input image
    val image = try {
      load(file, flags)
    } catch {
      case t: IOException =>
        println(t.getMessage)
        sys.exit(1)
    }
    image
  }

  /** Show image in a window. Closing the window will exit the application. */
  def show(mat: Mat, title: String): Unit = {
    val converter = new ToMat()
    val canvas = new CanvasFrame(title, 1)
    canvas.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
    canvas.showImage(converter.convert(mat))
  }

  /** Show image in a window. Closing the window will exit the application. */
  def show(image: Image, title: String): Unit = {
    val canvas = new CanvasFrame(title, 1)
    canvas.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
    canvas.showImage(image)
  }


  /** Draw red circles at point locations on an image. */
  def drawOnImage(image: Mat, points: Point2fVector): Mat = {
    val dest = image.clone()
    val radius = 5
    val red = new Scalar(0, 0, 255, 0)
    for (i <- 0 until points.size.toInt) {
      val p = points.get(i)
      circle(dest, new Point(round(p.x), round(p.y)), radius, red)
    }

    dest
  }

  /** Draw a shape on an image.
   *
   * @param image   input image
   * @param overlay shape to draw
   * @param color   color to use
   * @return new image with drawn overlay
   */
  def drawOnImage(image: Mat, overlay: Rect, color: Scalar): Mat = {
    val dest = image.clone()
    rectangle(dest, overlay, color)
    dest
  }

  /** Save the image to the specified file.
   *
   * The image format is chosen based on the filename extension (see `imread()` in OpenCV documentation for the list of extensions).
   * Only 8-bit (or 16-bit in case of PNG, JPEG 2000, and TIFF) single-channel or
   * 3-channel (with ‘BGR’ channel order) images can be saved using this function.
   * If the format, depth or channel order is different, use Mat::convertTo() , and cvtColor() to convert it before saving.
   *
   * @param file  file to save to. File name extension decides output image format.
   * @param image image to save.
   */
  def save(file: File, image: Mat): Unit = {
    imwrite(file.getAbsolutePath, image)
  }

  /** Convert native vector to JVM array.
   *
   * @param keyPoints pointer to a native vector containing KeyPoints.
   */
  def toArray(keyPoints: KeyPoint): Array[KeyPoint] = {
    val oldPosition = keyPoints.position()
    // Convert keyPoints to Scala sequence
    val points = for (i <- Array.range(0, keyPoints.capacity.toInt)) yield new KeyPoint(keyPoints.position(i))
    // Reset position explicitly to avoid issues from other uses of this position-based container.
    keyPoints.position(oldPosition)

    points
  }

  /** Convert native vector to JVM array.
   *
   * @param keyPoints pointer to a native vector containing KeyPoints.
   */
  def toArray(keyPoints: KeyPointVector): Array[KeyPoint] = {
    // for the simplicity of the implementation we will assume that number of key points is within Int range.
    require(keyPoints.size() <= Int.MaxValue)
    val n = keyPoints.size().toInt

    // Convert keyPoints to Scala sequence
    for (i <- Array.range(0, n)) yield new KeyPoint(keyPoints.get(i))
  }

  /** Convert native vector to JVM array.
   *
   * @param matches pointer to a native vector containing DMatches.
   * @return
   */
  def toArray(matches: DMatchVector): Array[DMatch] = {
    // for the simplicity of the implementation we will assume that number of key points is within Int range.
    require(matches.size() <= Int.MaxValue)
    val n = matches.size().toInt

    // Convert keyPoints to Scala sequence
    for (i <- Array.range(0, n)) yield new DMatch(matches.get(i))
  }

  def toBufferedImage(mat: Mat): BufferedImage = {
    val openCVConverter = new ToMat()
    val java2DConverter = new Java2DFrameConverter()
    java2DConverter.convert(openCVConverter.convert(mat))
  }

  def toDMatchVector(src: Seq[DMatch]): DMatchVector = {
    val dest = new DMatchVector(src.size)
    for ((m, i) <- src.toArray.zipWithIndex) {
      dest.put(i, m)
    }
    dest
  }

  def toPoint(p: Point2f): Point = new Point(round(p.x), round(p.y))


  /**
   * Convert `Mat` to one where pixels are represented as 8 bit unsigned integers (`CV_8U`).
   * It creates a copy of the input image.
   *
   * @param src input image.
   * @return copy of the input with pixels values represented as 8 bit unsigned integers.
   */
  def toMat8U(src: Mat, doScaling: Boolean = true): Mat = {
    val minVal = new DoublePointer(Double.MaxValue)
    val maxVal = new DoublePointer(Double.MinValue)
    minMaxLoc(src, minVal, maxVal, null, null, new Mat())

    val min = minVal.get()
    val max = maxVal.get()
    val (scale, offset) = if (doScaling) {
      val s = 255d / (max - min)
      (s, -min * s)
    } else (1d, 0d)

    val dest = new Mat()
    src.convertTo(dest, CV_8U, scale, offset)
    dest
  }

  def toMatPoint2f(points: Seq[Point2f]): Mat = {
    // Create Mat representing a vector of Points3f
    val dest = new Mat(1, points.size, CV_32FC2)
    val indx = dest.createIndexer().asInstanceOf[FloatIndexer]
    for (i <- points.indices) {
      val p = points(i)
      indx.put(0, i, 0, p.x)
      indx.put(0, i, 1, p.y)
    }
    require(dest.checkVector(2) >= 0)
    dest
  }

  /**
   * Convert a sequence of Point3D to a Mat representing a vector of Points3f.
   * Calling  `checkVector(3)` on the return value will return non-negative value indicating that it is a vector with 3 channels.
   */
  def toMatPoint3f(points: Seq[Point3f]): Mat = {
    // Create Mat representing a vector of Points3f
    val dest = new Mat(1, points.size, CV_32FC3)
    val indx = dest.createIndexer().asInstanceOf[FloatIndexer]
    for (i <- points.indices) {
      val p = points(i)
      indx.put(0, i, 0, p.x)
      indx.put(0, i, 1, p.y)
      indx.put(0, i, 2, p.z)
    }
    dest
  }

  def toPoint2fArray(mat: Mat): Array[Point2f] = {
    require(mat.checkVector(2) >= 0, "Expecting a vector Mat")

    val indexer = mat.createIndexer().asInstanceOf[FloatIndexer]
    val size = mat.total.toInt
    val dest = new Array[Point2f](size)

    for (i <- 0 until size) dest(i) = new Point2f(indexer.get(0, i, 0), indexer.get(0, i, 1))
    dest
  }

  /** Convert from KeyPoint to Point2D32f representation */
  def toPoint2fVectorPair(matches: DMatchVector, keyPoints1: KeyPointVector, keyPoints2: KeyPointVector): (Point2fVector, Point2fVector) = {

    // Extract keypoints from each match, separate Left and Right
    val size = matches.size.toInt
    val pointIndexes1 = new Array[Int](size)
    val pointIndexes2 = new Array[Int](size)
    for (i <- 0 until size) {
      pointIndexes1(i) = matches.get(i).queryIdx()
      pointIndexes2(i) = matches.get(i).trainIdx()
    }

    // Convert keypoints into Point2f
    val points1 = new Point2fVector()
    val points2 = new Point2fVector()
    KeyPoint.convert(keyPoints1, points1, pointIndexes1)
    KeyPoint.convert(keyPoints2, points2, pointIndexes2)

    (points1, points2)
  }

  /**
   * Convert a vector of Point2f to a Mat representing a vector of Points2f.
   */
  def toMat(points: Point2fVector): Mat = {
    // Create Mat representing a vector of Points3f
    val size: Int = points.size.toInt
    // Argument to Mat constructor must be `Int` to mean sizes, otherwise it may be interpreted as content.
    val dest = new Mat(1, size, CV_32FC2)
    val indx = dest.createIndexer().asInstanceOf[FloatIndexer]
    for (i <- 0 until size) {
      val p = points.get(i)
      indx.put(0, i, 0, p.x)
      indx.put(0, i, 1, p.y)
    }
    dest
  }


  /** Convert a Scala collection to a JavaCV "vector".
   *
   * @param src Scala collection
   * @return JavaCV/native collection
   */
  def toVector(src: Array[DMatch]): DMatchVector = {
    val dest = new DMatchVector(src.length)
    for (i <- src.indices) dest.put(i, src(i))
    dest
  }

  /**
   * Print info about the `mat`
   */
  def printInfo(mat: Mat, caption: String = ""): Unit = {
    println(
      caption + "\n" +
        s"  cols:     ${mat.cols}\n" +
        s"  rows:     ${mat.rows}\n" +
        s"  depth:    ${mat.depth}\n" +
        s"  channels: ${mat.channels}\n" +
        s"  type:     ${mat.`type`}\n" +
        s"  dims:     ${mat.dims}\n" +
        s"  total:    ${mat.total}\n"
    )
  }

  def homographyToAffineTransform(homography: Mat): AffineTransform = {
    // TODO: validate that matrix is a homography
    val hIndexer = homography.createIndexer().asInstanceOf[DoubleIndexer]
    val afm = new Array[Double](6)
    afm(0) = hIndexer.get(0)
    afm(1) = hIndexer.get(3)
    afm(2) = hIndexer.get(1)
    afm(3) = hIndexer.get(4)
    afm(4) = hIndexer.get(2)
    afm(5) = hIndexer.get(5)

    new AffineTransform(afm)
  }

  def affineTransforToHomography(t: AffineTransform): Mat = {
    val homography = new Mat(3, 3, CV_64F)
    val hIndexer = homography.createIndexer().asInstanceOf[DoubleIndexer]

    val afm = new Array[Double](6)
    t.getMatrix(afm)

    hIndexer.put(0, afm(0))
    hIndexer.put(3, afm(1))
    hIndexer.put(1, afm(2))
    hIndexer.put(4, afm(3))
    hIndexer.put(2, afm(4))
    hIndexer.put(5, afm(5))

    hIndexer.put(6, 0.0)
    hIndexer.put(7, 0.0)
    hIndexer.put(8, 1.0)

    homography
  }

  def drawFeatures(keyPointsArray: Array[KeyPoint], cp: ColorProcessor, fixedSize: Boolean = false, color: Color = Color.RED): Unit = {

    val circles = keyPointsArray.map { kp =>
      val x = kp.pt().x()
      val y = kp.pt().y()
      new Point2D.Float(x, y)
      val radius = if (fixedSize) 2f else kp.size()
      new ShapeRoi(new Ellipse2D.Float(x - radius, y - radius, 2 * radius, 2 * radius))
    }

    cp.setColor(color)
    circles.foreach {
      cp.draw(_)
    }
  }

  def drawFeaturesOverlay(keyPointsArray: Array[KeyPoint], imp: ImagePlus, fixedSize: Boolean = false, color: Color = Color.RED): Unit = {

    val circles = keyPointsArray.map { kp =>
      val x = kp.pt().x()
      val y = kp.pt().y()
      new Point2D.Float(x, y)
      val radius = if (fixedSize) 2f else kp.size()
      new ShapeRoi(new Ellipse2D.Float(x - radius, y - radius, 2 * radius, 2 * radius))
    }

    val overlay = new Overlay()
    circles.foreach {
      overlay.add(_)
    }
    overlay.setStrokeColor(color)
    imp.setOverlay(overlay)
  }


}