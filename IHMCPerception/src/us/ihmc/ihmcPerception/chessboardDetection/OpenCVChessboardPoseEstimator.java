package us.ihmc.ihmcPerception.chessboardDetection;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;

import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import us.ihmc.utilities.nativelibraries.NativeLibraryLoader;

public class OpenCVChessboardPoseEstimator
{
   static
   {
      NativeLibraryLoader.loadLibrary("org.opencv", "opencv_java2411");
   }
   public boolean DEBUG = false;

   Size boardInnerCrossPattern;
   double gridWidth;
   Point3[] boardPoints;
   MatOfPoint2f corners = new MatOfPoint2f();
   TermCriteria termCriteria = new TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 30, 0.001);
   Mat cameraMatrix = null;
   MatOfDouble distCoeffs = new MatOfDouble();

   public OpenCVChessboardPoseEstimator(int rowsOfSquare, int colsOfSquare, double gridWidth)
   {
      setBoardSize(rowsOfSquare, colsOfSquare, gridWidth);
   }
   
   public void setBoardSize(int rowsOfSquare, int colsOfSquare, double gridWidth)
   {
      boardInnerCrossPattern = new Size(colsOfSquare - 1, rowsOfSquare - 1);
      this.gridWidth = gridWidth;
      generateBoardPoints();
      
   }

   private void generateBoardPoints()
   {
      boardPoints = new Point3[(int) boardInnerCrossPattern.area()];
      int count = 0;
      for (int j = 0; j < boardInnerCrossPattern.height; j++)
         for (int i = 0; i < boardInnerCrossPattern.width; i++)
         {
            double x = gridWidth * (i - boardInnerCrossPattern.width / 2 + 0.5);
            double y = -gridWidth * (j - boardInnerCrossPattern.height / 2 + 0.5);
            boardPoints[count++] = new Point3(x, y, 0.0);
         }
   }

   static public Mat convertBufferedImageToMat(BufferedImage image)
   {
      Mat imageMat;
      byte[] pixel;
      switch (image.getType())
      {
      case BufferedImage.TYPE_3BYTE_BGR:
         imageMat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
         pixel = ((DataBufferByte) (image.getRaster().getDataBuffer())).getData();
         break;
      case BufferedImage.TYPE_4BYTE_ABGR:
      case BufferedImage.TYPE_INT_ARGB:
         //need to double check endianess
         imageMat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC4);
         pixel = new byte[image.getHeight() * image.getWidth() * 4];
         ByteBuffer pixelBuf = ByteBuffer.wrap(pixel);
         int[] intBuf = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
         pixelBuf.asIntBuffer().put(intBuf);
         break;
      default:
         throw new RuntimeException("unknown type " + image.getType());
      }
      imageMat.put(0, 0, pixel);
      return imageMat;

   }

   public void setCameraMatrix(double fx, double fy, double cx, double cy)
   {
      cameraMatrix = Mat.zeros(3, 3, CvType.CV_32F);
      cameraMatrix.put(0, 0, fx); //fx
      cameraMatrix.put(1, 1, fy); //fy
      cameraMatrix.put(0, 2, cx); //cx
      cameraMatrix.put(1, 2, cy); //cy
      cameraMatrix.put(2, 2, 1);
   }

   public void setDefaultCameraMatrix(int rows, int cols, double fovY)
   {
      double f = rows / 2.0 / Math.tan(fovY / 2.0);
      cameraMatrix = Mat.zeros(3, 3, CvType.CV_32F);
      cameraMatrix.put(0, 0, f); //fx
      cameraMatrix.put(1, 1, f); //fy
      cameraMatrix.put(0, 2, cols / 2.0); //cx
      cameraMatrix.put(1, 2, rows / 2.0); //cy
      cameraMatrix.put(2, 2, 1);
   }

   public RigidBodyTransform detect(BufferedImage image)
   {
      if (cameraMatrix == null)
         setDefaultCameraMatrix(image.getHeight(), image.getWidth(), Math.PI / 4);
      Mat imageMat = convertBufferedImageToMat(image);
      int flags = Calib3d.CALIB_CB_ADAPTIVE_THRESH + Calib3d.CALIB_CB_NORMALIZE_IMAGE;//+ Calib3d.CALIB_CB_FAST_CHECK;
      if (Calib3d.findChessboardCorners(imageMat, boardInnerCrossPattern, corners, flags))
      {
         Mat imageGray = new Mat(imageMat.rows(), imageMat.cols(), CvType.CV_8UC1);
         Imgproc.cvtColor(imageMat, imageGray, Imgproc.COLOR_BGRA2GRAY);
         Mat rvec = new Mat(), tvec = new Mat();

         int subPixSize = 3;
         Imgproc.cornerSubPix(imageGray, corners, new Size(subPixSize, subPixSize), new Size(-1, -1), termCriteria);
         //         Calib3d.drawChessboardCorners(imageGray, boardInnerCrossPattern, corners, true);

         Calib3d.solvePnPRansac(new MatOfPoint3f(boardPoints), corners, cameraMatrix, distCoeffs, rvec, tvec);

         RigidBodyTransform transform = opencvTRtoRigidBodyTransform(rvec, tvec);

         if (DEBUG)
         {
            printMat(cameraMatrix);
            drawImagePoints(image, corners, Color.GREEN);
            drawReprojectedPoints(image, rvec, tvec, Color.RED);
            drawAxis(image, transform, 0.2);
         }
         return transform;
      }
      else
      {
         return null;
      }

   }

   public static RigidBodyTransform opencvTRtoRigidBodyTransform(Mat rvec, Mat tvec)
   {
      Vector3d translation = new Vector3d(tvec.get(0, 0)[0], tvec.get(1, 0)[0], tvec.get(2, 0)[0]);
      Vector3d axis = new Vector3d(rvec.get(0, 0)[0], rvec.get(1, 0)[0], rvec.get(2, 0)[0]);
      double angle = axis.length();
      axis.normalize();
      AxisAngle4d rotation = new AxisAngle4d(axis, angle);
      RigidBodyTransform transform = new RigidBodyTransform(rotation, translation);
      return transform;
   }

   public static void rigidBodyTransformToOpenCVTR(RigidBodyTransform transform, Mat tvec, Mat rvec)
   {
      Point3d translation = new Point3d();
      transform.getTranslation(translation);
      AxisAngle4d axisAngle = new AxisAngle4d();
      transform.getRotation(axisAngle);
      Vector3d rotVector = new Vector3d(axisAngle.x, axisAngle.y, axisAngle.z);
      rotVector.normalize();
      rotVector.scale(axisAngle.angle);

      tvec.put(0, 0, translation.x);
      tvec.put(1, 0, translation.y);
      tvec.put(2, 0, translation.z);

      rvec.put(0, 0, rotVector.x);
      rvec.put(1, 0, rotVector.y);
      rvec.put(2, 0, rotVector.z);
   }

   public void drawAxis(BufferedImage image, RigidBodyTransform transform, double scale)
   {
      Mat tvec = new Mat(3, 1, CvType.CV_32F);
      Mat rvec = new Mat(3, 1, CvType.CV_32F);
      rigidBodyTransformToOpenCVTR(transform, tvec, rvec);
      drawAxis(image, rvec, tvec, scale);
   }
   
   public javax.vecmath.Point2d getCheckerBoardImageOrigin(RigidBodyTransform transform)
   {
      Point3 origin = new Point3();
      Mat tvec = new Mat(3, 1, CvType.CV_32F);
      Mat rvec = new Mat(3, 1, CvType.CV_32F);
      rigidBodyTransformToOpenCVTR(transform, tvec, rvec);
      MatOfPoint2f imagePoints = new MatOfPoint2f();
      Calib3d.projectPoints(new MatOfPoint3f(origin), rvec, tvec, cameraMatrix, distCoeffs, imagePoints);
      return new javax.vecmath.Point2d(imagePoints.get(0, 0));
   }

   private void drawAxis(BufferedImage image, Mat rvec, Mat tvec, double scale)
   {
      Point3 origin = new Point3();
      Point3 xAxis = new Point3(scale, 0.0, 0.0);
      Point3 yAxis = new Point3(0.0, scale, 0.0);
      Point3 zAxis = new Point3(0.0, 0.0, scale);

      MatOfPoint2f imagePoints = new MatOfPoint2f();
      Calib3d.projectPoints(new MatOfPoint3f(origin, xAxis), rvec, tvec, cameraMatrix, distCoeffs, imagePoints);
      drawImagePoints(image, imagePoints, Color.RED);
      Calib3d.projectPoints(new MatOfPoint3f(origin, yAxis), rvec, tvec, cameraMatrix, distCoeffs, imagePoints);
      drawImagePoints(image, imagePoints, Color.GREEN);
      Calib3d.projectPoints(new MatOfPoint3f(origin, zAxis), rvec, tvec, cameraMatrix, distCoeffs, imagePoints);
      drawImagePoints(image, imagePoints, Color.BLUE);
   }

   public static void printMat(Mat m)
   {
      System.out.println("Mat " + m.height() + "x" + m.width() + "\n------------------------------");
      for (int i = 0; i < m.height(); i++)
      {
         for (int j = 0; j < m.width(); j++)
         {
            System.out.print(String.format(" %8.2f", m.get(i, j)[0]));
         }
         System.out.println();
      }

      System.out.println("------------------------------");
   }

   public void drawReprojectedPoints(BufferedImage image, RigidBodyTransform transform, Color color)
   {
      Mat tvec = new Mat(3, 1, CvType.CV_32F);
      Mat rvec = new Mat(3, 1, CvType.CV_32F);
      rigidBodyTransformToOpenCVTR(transform, tvec, rvec);
      drawReprojectedPoints(image, rvec, tvec, color);
   }

   private void drawReprojectedPoints(BufferedImage image, Mat rvec, Mat tvec, Color color)
   {
      MatOfPoint2f imagePoints = new MatOfPoint2f();
      Calib3d.projectPoints(new MatOfPoint3f(boardPoints), rvec, tvec, cameraMatrix, distCoeffs, imagePoints);
      drawImagePoints(image, imagePoints, color);
   }

   public void drawImagePoints(BufferedImage image, Mat imagePoints, Color color)
   {
      Point2d[] point2dArray = new Point2d[imagePoints.rows()];
      for (int i = 0; i < point2dArray.length; i++)
      {
         double[] v = imagePoints.get(i, 0);
         point2dArray[i] = new Point2d(v[0], v[1]);
      }
      drawImagePoints(image, point2dArray, color);

   }

   public void drawImagePoints(BufferedImage image, Point2d[] imagePoints, Color color)
   {

      Graphics2D g2 = image.createGraphics();
      g2.setStroke(new BasicStroke(4));
      g2.setColor(color);

      for (int i = 0; i < imagePoints.length; i++)
      {
         if (i > 0)
         {
            int radius = 2;
            g2.drawOval((int) imagePoints[i].x - radius, (int) imagePoints[i].y - radius, 2 * radius, 2 * radius);
            g2.drawLine((int) imagePoints[i - 1].x, (int) imagePoints[i - 1].y, (int) imagePoints[i].x, (int) imagePoints[i].y);
         }
         else
         {
            int radius = 12;
            g2.drawOval((int) imagePoints[i].x - radius, (int) imagePoints[i].y - radius, 2 * radius, 2 * radius);
         }

      }
      g2.finalize();
   }

}
