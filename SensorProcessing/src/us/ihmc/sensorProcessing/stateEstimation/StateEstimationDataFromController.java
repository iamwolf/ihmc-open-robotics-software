package us.ihmc.sensorProcessing.stateEstimation;

import java.util.ArrayList;
import java.util.List;

import us.ihmc.controlFlow.AbstractControlFlowElement;
import us.ihmc.controlFlow.ControlFlowGraph;
import us.ihmc.controlFlow.ControlFlowInputPort;
import us.ihmc.controlFlow.ControlFlowOutputPort;
import us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration.PointPositionDataObject;
import us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration.PointVelocityDataObject;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

public class StateEstimationDataFromController extends AbstractControlFlowElement
{
   private final ReferenceFrame angularAccelerationEstimationFrame;
   private final ReferenceFrame centerOfMassAccelerationFrame = ReferenceFrame.getWorldFrame();

   private final FrameVector desiredCenterOfMassAcceleration;
   private final FrameVector desiredAngularAcceleration;
   private final List<PointPositionDataObject> pointPositionDataObjects;
   private final List<PointVelocityDataObject> pointVelocityDataObjects;

   private final ControlFlowOutputPort<FrameVector> desiredCenterOfMassAccelerationOutputPort;
   private final ControlFlowOutputPort<FrameVector> desiredAngularAccelerationOutputPort;
   private final ControlFlowOutputPort<List<PointPositionDataObject>> pointPositionOutputPort;
   private final ControlFlowOutputPort<List<PointVelocityDataObject>> pointVelocityOutputPort;

   private int currentIndexInPointPositionList = 0;
   private int currentIndexInPointVelocityList = 0;

   public StateEstimationDataFromController(ReferenceFrame estimationFrame)
   {
      this.angularAccelerationEstimationFrame = estimationFrame;

      this.desiredCenterOfMassAccelerationOutputPort = new ControlFlowOutputPort<FrameVector>("desiredCoMAcceleration", this);
      registerOutputPort(desiredCenterOfMassAccelerationOutputPort);

      this.desiredAngularAccelerationOutputPort = new ControlFlowOutputPort<FrameVector>("desiredAngularAcceleration", this);
      registerOutputPort(desiredAngularAccelerationOutputPort);

      this.pointPositionOutputPort = new ControlFlowOutputPort<List<PointPositionDataObject>>("pointPosition", this);
      registerOutputPort(pointPositionOutputPort);
      
      this.pointVelocityOutputPort = new ControlFlowOutputPort<List<PointVelocityDataObject>>("pointVelocity", this);
      registerOutputPort(pointVelocityOutputPort);

      desiredCenterOfMassAcceleration = new FrameVector(centerOfMassAccelerationFrame);
      desiredAngularAcceleration = new FrameVector(angularAccelerationEstimationFrame);
      pointPositionDataObjects = new ArrayList<PointPositionDataObject>();
      pointVelocityDataObjects = new ArrayList<PointVelocityDataObject>();

      desiredCenterOfMassAccelerationOutputPort.setData(desiredCenterOfMassAcceleration);
      desiredAngularAccelerationOutputPort.setData(desiredAngularAcceleration);
      pointPositionOutputPort.setData(pointPositionDataObjects);
      pointVelocityOutputPort.setData(pointVelocityDataObjects);
   }
   
   public ReferenceFrame getAngularAccelerationEstimationFrame()
   {
      return angularAccelerationEstimationFrame;
   }

   public void setDesiredCenterOfMassAcceleration(FrameVector desiredCenterOfMassAcceleration)
   {
      checkReferenceFrameMatchByName(centerOfMassAccelerationFrame, desiredCenterOfMassAcceleration.getReferenceFrame());
      this.desiredCenterOfMassAcceleration.set(centerOfMassAccelerationFrame, desiredCenterOfMassAcceleration.getVector());
      desiredCenterOfMassAccelerationOutputPort.setData(this.desiredCenterOfMassAcceleration);
   }

   public void setDesiredAngularAcceleration(FrameVector desiredAngularAcceleration)
   {
      checkReferenceFrameMatchByName(angularAccelerationEstimationFrame, desiredAngularAcceleration.getReferenceFrame());
      this.desiredAngularAcceleration.set(angularAccelerationEstimationFrame, desiredAngularAcceleration.getVector());
      desiredAngularAccelerationOutputPort.setData(this.desiredAngularAcceleration);
   }
   
   private void setPointPositionMeasurements(List<PointPositionDataObject> pointPositionDataObjects)
   {
      extendPointPositionDataObjectList(pointPositionDataObjects.size());
      
      for (int i = 0; i < pointPositionDataObjects.size(); i++)
      {
         this.pointPositionDataObjects.get(i).set(pointPositionDataObjects.get(i));
      }
      
      pointPositionOutputPort.setData(this.pointPositionDataObjects);
   }
   
   private void setPointVelocityMeasurements(List<PointVelocityDataObject> pointVelocityDataObjects)
   {
      extendPointVelocityDataObjectList(pointVelocityDataObjects.size());
      
      for (int i = 0; i < pointVelocityDataObjects.size(); i++)
      {
         this.pointVelocityDataObjects.get(i).set(pointVelocityDataObjects.get(i));
      }
      
      pointVelocityOutputPort.setData(this.pointVelocityDataObjects);
   }

   public void clearDesiredAccelerations()
   {
      desiredCenterOfMassAcceleration.set(0.0, 0.0, 0.0);
      desiredAngularAcceleration.set(0.0, 0.0, 0.0);
   }
   
   public void clearPointPositionDataObjects()
   {
      currentIndexInPointPositionList = 0;
      
      for (PointPositionDataObject pointPositionDataObject : pointPositionDataObjects)
      {
         pointPositionDataObject.invalidatePointPosition();
      }
   }
   
   public void clearPointVelocityDataObjects()
   {
      currentIndexInPointVelocityList = 0;
      
      for (PointVelocityDataObject pointVelocityDataObject : pointVelocityDataObjects)
      {
         pointVelocityDataObject.invalidatePointVelocity();
      }
   }

   public FrameVector getDesiredAngularAcceleration()
   {
      return desiredAngularAcceleration;
   }

   public FrameVector getDesiredCenterOfMassAcceleration()
   {
      return desiredCenterOfMassAcceleration;
   }

   public void updatePointPositionSensorData(PointPositionDataObject value)
   {
      setOrAddObjectToList(currentIndexInPointPositionList++, pointPositionDataObjects, value);
   }
   
   public void updatePointVelocitySensorData(PointVelocityDataObject value)
   {
      setOrAddObjectToList(currentIndexInPointVelocityList++, pointVelocityDataObjects, value);
   }

   public List<PointPositionDataObject> getPointPositionDataObjects()
   {
      return pointPositionDataObjects;
   }
   
   public List<PointVelocityDataObject> getPointVelocityDataObjects()
   {
      return pointVelocityDataObjects;
   }

   public void startComputation()
   {
   }

   public void waitUntilComputationIsDone()
   {
      // do nothing.
   }
   
   public void connectDesiredAccelerationPorts(ControlFlowGraph controlFlowGraph, StateEstimatorWithPorts orientationEstimatorWithPorts)
   {      
      ControlFlowInputPort<FrameVector> desiredAngularAccelerationInputPort = orientationEstimatorWithPorts.getDesiredAngularAccelerationInputPort();
      ControlFlowInputPort<FrameVector> desiredCenterOfMassAccelerationInputPort = orientationEstimatorWithPorts.getDesiredCenterOfMassAccelerationInputPort();
      ControlFlowInputPort<List<PointPositionDataObject>> pointPositionInputPort = orientationEstimatorWithPorts.getPointPositionInputPort();
      ControlFlowInputPort<List<PointVelocityDataObject>> pointVelocityInputPort = orientationEstimatorWithPorts.getPointVelocityInputPort();
      
      controlFlowGraph.connectElements(desiredAngularAccelerationOutputPort, desiredAngularAccelerationInputPort);
      controlFlowGraph.connectElements(desiredCenterOfMassAccelerationOutputPort, desiredCenterOfMassAccelerationInputPort);

      controlFlowGraph.connectElements(pointPositionOutputPort, pointPositionInputPort);
      controlFlowGraph.connectElements(pointVelocityOutputPort, pointVelocityInputPort);
   }

   public void set(StateEstimationDataFromController stateEstimationDataFromControllerSink)
   {
      setDesiredCenterOfMassAcceleration(stateEstimationDataFromControllerSink.getDesiredCenterOfMassAcceleration());
      setDesiredAngularAcceleration(stateEstimationDataFromControllerSink.getDesiredAngularAcceleration());
      setPointPositionMeasurements(stateEstimationDataFromControllerSink.getPointPositionDataObjects());
      setPointVelocityMeasurements(stateEstimationDataFromControllerSink.getPointVelocityDataObjects());
   }

   private void extendPointPositionDataObjectList(int newSize)
   {
      for (int i = this.pointPositionDataObjects.size(); i < newSize; i++)
      {
         PointPositionDataObject pointPositionDataObject = new PointPositionDataObject();
         pointPositionDataObjects.add(pointPositionDataObject);
      }
   }

   private void extendPointVelocityDataObjectList(int newSize)
   {
      for (int i = this.pointVelocityDataObjects.size(); i < newSize; i++)
      {
         PointVelocityDataObject pointVelocityDataObject = new PointVelocityDataObject();
         pointVelocityDataObjects.add(pointVelocityDataObject);
      }
   }

   private static void checkReferenceFrameMatchByName(ReferenceFrame expected, ReferenceFrame actual)
   {
      if(!actual.getName().equals(expected.getName()))
      {
         throw new RuntimeException("Frame name does not match, expected: " + expected.getName() + ", actual: "
               + actual.getName());  
      }
   }

   private static <ObjectType> void setOrAddObjectToList(int i, List<ObjectType> list, ObjectType object)
   {
      if (i < list.size())
         list.set(i, object);
      else
         list.add(object);
   }
}

