package us.ihmc.footstepPlanning.polygonSnapping;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

import org.junit.Test;

import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.robotics.geometry.ConvexPolygon2d;
import us.ihmc.robotics.geometry.PlanarRegion;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.geometry.PlanarRegionsListGenerator;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.random.RandomTools;
import us.ihmc.tools.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.tools.testing.MutationTestingTools;
import us.ihmc.tools.thread.ThreadTools;

public class PlanarRegionsListPolygonSnapperTest
{
   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testSimpleVerticalSnap()
   {
      boolean visualize = false;
      ConvexPolygon2d polygonToSnap = PlanarRegionsListExamples.createRectanglePolygon(0.5, 0.25);
      RigidBodyTransform nonSnappedTransform = new RigidBodyTransform();

      PolygonSnapperVisualizer polygonSnapperVisualizer = null;
      if (visualize)
      {
         polygonSnapperVisualizer = new PolygonSnapperVisualizer(polygonToSnap);
      }

      PlanarRegionsListGenerator generator = new PlanarRegionsListGenerator();

      generator.addCubeReferencedAtBottomMiddle(1.0, 0.5, 0.7);
      PlanarRegionsList planarRegionsList = generator.getPlanarRegionsList();

      RigidBodyTransform snapTransform = PlanarRegionsListPolygonSnapper.snapPolygonToPlanarRegionsList(polygonToSnap, planarRegionsList);

      if (polygonSnapperVisualizer != null)
      {
         polygonSnapperVisualizer.addPlanarRegionsList(planarRegionsList, YoAppearance.Gray());
         polygonSnapperVisualizer.setSnappedPolygon(nonSnappedTransform, snapTransform);
      }

      RigidBodyTransform expectedTransform = new RigidBodyTransform();
      expectedTransform.setTranslation(0.0, 0.0, 0.7);
      assertTrue(expectedTransform.epsilonEquals(snapTransform, 1e-7));

      if (visualize)
      {
         ThreadTools.sleepForever();
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testSimpleVerticalAndRotatedSnap()
   {
      boolean visualize = false;
      ConvexPolygon2d polygonToSnap = PlanarRegionsListExamples.createRectanglePolygon(0.5, 0.25);
      RigidBodyTransform nonSnappedTransform = new RigidBodyTransform();

      PolygonSnapperVisualizer polygonSnapperVisualizer = null;
      if (visualize)
      {
         polygonSnapperVisualizer = new PolygonSnapperVisualizer(polygonToSnap);
      }

      RigidBodyTransform planarRegionTransform = new RigidBodyTransform();
      planarRegionTransform.setRotationEulerAndZeroTranslation(0.1, 0.2, 0.3);

      PlanarRegionsListGenerator generator = new PlanarRegionsListGenerator();
      generator.setTransform(planarRegionTransform);

      generator.addCubeReferencedAtBottomMiddle(1.0, 0.5, 0.7);
      PlanarRegionsList planarRegionsList = generator.getPlanarRegionsList();

      RigidBodyTransform snapTransform = PlanarRegionsListPolygonSnapper.snapPolygonToPlanarRegionsList(polygonToSnap, planarRegionsList);

      if (polygonSnapperVisualizer != null)
      {
         polygonSnapperVisualizer.addPlanarRegionsList(planarRegionsList, YoAppearance.Gray());
         polygonSnapperVisualizer.setSnappedPolygon(nonSnappedTransform, snapTransform);
      }

      PlanarRegionPolygonSnapperTest.assertSurfaceNormalsMatchAndSnapPreservesXFromAbove(snapTransform, planarRegionTransform);

      if (visualize)
      {
         ThreadTools.sleepForever();
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testMovingAcrossStaircase()
   {
      boolean visualize = false;
      PlanarRegionsList planarRegionsList = PlanarRegionsListExamples.generateStairCase();
      ArrayList<double[]> xyYawToTest = new ArrayList<>();
      for (double xTranslation = 0.0; xTranslation < 3.0; xTranslation = xTranslation + 0.1)
      {
         xyYawToTest.add(new double[] { xTranslation, 0.0, 0.0 });
      }

      doATest(planarRegionsList, xyYawToTest, visualize);
   }

   @ContinuousIntegrationTest(estimatedDuration = 2.0)
   @Test(timeout = 30000)
   public void testRandomPlanarRegions()
   {
      Random random = new Random(1776L);

      boolean visualize = false;
      int numberOfRandomObjects = 100;
      double maxX = 2.0;
      double maxY = 1.0;
      double maxZ = 0.5;

      PlanarRegionsList planarRegionsList = PlanarRegionsListExamples.generateRandomObjects(random, numberOfRandomObjects, maxX, maxY, maxZ);
      ArrayList<double[]> xyYawToTest = new ArrayList<>();

      for (double x = -maxX; x<maxX; x = x + 0.1)
      {
         for (double y = -maxY; y<maxY; y = y + 0.1)
         {
            double yaw = RandomTools.generateRandomDouble(random, Math.PI);

            xyYawToTest.add(new double[] { x, y, yaw });
         }
      }

      doATest(planarRegionsList, xyYawToTest, visualize);
   }

   @ContinuousIntegrationTest(estimatedDuration = 2.0)
   @Test(timeout = 30000)
   public void testBumpyGround()
   {
      Random random = new Random(1776L);

      boolean visualize = false;
      double maxX = 2.0;
      double maxY = 1.0;
      double maxZ = 0.2;

      PlanarRegionsList planarRegionsList = PlanarRegionsListExamples.generateBumpyGround(random, maxX, maxY, maxZ);
      ArrayList<double[]> xyYawToTest = new ArrayList<>();

      for (double x = -maxX; x<maxX; x = x + 0.1)
      {
         for (double y = -maxY; y<maxY; y = y + 0.1)
         {
            double yaw = RandomTools.generateRandomDouble(random, Math.PI);

            xyYawToTest.add(new double[] { x, y, yaw });
         }
      }

      doATest(planarRegionsList, xyYawToTest, visualize);
   }

   private static void doATest(PlanarRegionsList planarRegionsList, ArrayList<double[]> xyYawToTest, boolean visualize)
   {
      ConvexPolygon2d originalPolygon = PlanarRegionsListExamples.createRectanglePolygon(0.3, 0.15);
      RigidBodyTransform nonSnappedTransform = new RigidBodyTransform();

      PolygonSnapperVisualizer polygonSnapperVisualizer = null;
      if (visualize)
      {
         polygonSnapperVisualizer = new PolygonSnapperVisualizer(originalPolygon);
      }

      if (polygonSnapperVisualizer != null)
      {
         polygonSnapperVisualizer.addPlanarRegionsList(planarRegionsList, YoAppearance.Gold(), YoAppearance.Purple(), YoAppearance.Brown(), YoAppearance.Blue(), YoAppearance.Chartreuse());
      }

      for (double[] xyYaw : xyYawToTest)
      {
         ConvexPolygon2d polygonToSnap = new ConvexPolygon2d(originalPolygon);
         nonSnappedTransform = new RigidBodyTransform();
         nonSnappedTransform.setRotationEulerAndZeroTranslation(0.0, 0.0, xyYaw[2]);
         nonSnappedTransform.setTranslation(xyYaw[0], xyYaw[1], 0.0);
         polygonToSnap.applyTransformAndProjectToXYPlane(nonSnappedTransform);

         RigidBodyTransform snapTransform = PlanarRegionsListPolygonSnapper.snapPolygonToPlanarRegionsList(polygonToSnap, planarRegionsList);
         //         PlanarRegionPolygonSnapperTest.assertSurfaceNormalsMatchAndSnapPreservesXFromAbove(snapTransform, planarRegionTransform);

         //         System.out.println(snapTransform);

         if (snapTransform != null)
         {
            int numberOfVertices = polygonToSnap.getNumberOfVertices();
            for (int vertexIndex = 0; vertexIndex < numberOfVertices; vertexIndex++)
            {
               Point2d vertex = polygonToSnap.getVertex(vertexIndex);
               Point3d snappedVertex = new Point3d(vertex.getX(), vertex.getY(), 0.0);

               snapTransform.transform(snappedVertex);

               List<PlanarRegion> planarRegions = planarRegionsList.findPlanarRegionsContainingPointByProjectionOntoXYPlane(snappedVertex.getX(), snappedVertex.getY());

               if (planarRegions != null)
               {
                  for (PlanarRegion planarRegion : planarRegions)
                  {
                     double planeZGivenXY = planarRegion.getPlaneZGivenXY(snappedVertex.getX(), snappedVertex.getY());
                     //                     assertTrue("planeZGivenXY = " + planeZGivenXY + ", snappedVertex.getZ() = " + snappedVertex.getZ(), planeZGivenXY <= snappedVertex.getZ() + 1e-4);
                  }
               }
            }
         }

         if (polygonSnapperVisualizer != null)
         {
            polygonSnapperVisualizer.setSnappedPolygon(nonSnappedTransform, snapTransform);
         }
      }

      if (visualize)
      {
         polygonSnapperVisualizer.cropBuffer();
         ThreadTools.sleepForever();
      }
   }

   public static void main(String[] args)
   {
      String targetTests = PlanarRegionsListPolygonSnapperTest.class.getName();
      String targetClassesInSamePackage = PlanarRegionsListPolygonSnapper.class.getName();
      MutationTestingTools.doPITMutationTestAndOpenResult(targetTests, targetClassesInSamePackage);
   }
}
