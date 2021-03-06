package us.ihmc.commonWalkingControlModules.capturePoint.smoothCMPBasedICPPlanner.ICPGeneration;

import java.util.List;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.commonWalkingControlModules.capturePoint.smoothCMPBasedICPPlanner.DenseMatrixVector3D;
import us.ihmc.commonWalkingControlModules.capturePoint.smoothCMPBasedICPPlanner.CoMGeneration.SmoothCoMIntegrationToolbox;
import us.ihmc.commons.lists.RecyclingArrayList;
import us.ihmc.euclid.Axis;
import us.ihmc.euclid.referenceFrame.interfaces.*;
import us.ihmc.matrixlib.MatrixTools;
import us.ihmc.robotics.math.trajectories.FrameTrajectory3D;
import us.ihmc.robotics.math.trajectories.Trajectory;
import us.ihmc.robotics.math.trajectories.Trajectory3D;

/**
 * @author Tim Seyde
 */

public class SmoothCapturePointToolbox
{
   private static final int defaultSize = 100;

   private final DenseMatrix64F polynomialCoefficientCombinedVector = new DenseMatrix64F(defaultSize, defaultSize);
   private final DenseMatrix64F polynomialCoefficientVector = new DenseMatrix64F(defaultSize, 1);

   private final DenseMatrix64F generalizedAlphaPrimeMatrix = new DenseMatrix64F(defaultSize, defaultSize);
   private final DenseMatrix64F generalizedBetaPrimeMatrix = new DenseMatrix64F(defaultSize, defaultSize);
   private final DenseMatrix64F generalizedAlphaBetaPrimeMatrix = new DenseMatrix64F(defaultSize, defaultSize);

   private final DenseMatrix64F M1 = new DenseMatrix64F(defaultSize, defaultSize);

   //TODO: implement validity checks
   /**
    * Backward iteration to determine &xi;<sub>ref,&phi;</sub>(0) and
    * &xi;<sub>ref,&phi;</sub>(T<sub>&phi;</sub>) for all segments &phi;
    */
   public void computeDesiredCornerPoints3D(RecyclingArrayList<? extends FixedFramePoint3DBasics> entryCornerPointsToPack,
                                            RecyclingArrayList<? extends FixedFramePoint3DBasics> exitCornerPointsToPack,
                                            List<FrameTrajectory3D> cmpPolynomials3D, double omega0)
   {
      FrameTrajectory3D cmpPolynomial3D = cmpPolynomials3D.get(cmpPolynomials3D.size() - 1);
      entryCornerPointsToPack.clear();
      exitCornerPointsToPack.clear();

      cmpPolynomial3D.compute(cmpPolynomial3D.getFinalTime());
      FramePoint3DReadOnly nextEntryCornerPoint = cmpPolynomial3D.getFramePosition();

      for (int i = cmpPolynomials3D.size() - 1; i >= 0; i--)
      {
         cmpPolynomial3D = cmpPolynomials3D.get(i);

         FixedFramePoint3DBasics exitCornerPoint = exitCornerPointsToPack.getAndGrowIfNeeded(i);
         FixedFramePoint3DBasics entryCornerPoint = entryCornerPointsToPack.getAndGrowIfNeeded(i);

         exitCornerPoint.set(nextEntryCornerPoint);

         computeDesiredCapturePointPosition3D(omega0, cmpPolynomial3D.getInitialTime(), exitCornerPoint, cmpPolynomial3D, entryCornerPoint);
         nextEntryCornerPoint = entryCornerPoint;
      }
   }

   public void computeDesiredCornerPoints(RecyclingArrayList<? extends FixedFramePoint3DBasics> entryCornerPointsToPack,
                                          RecyclingArrayList<? extends FixedFramePoint3DBasics> exitCornerPointsToPack,
                                          List<FrameTrajectory3D> cmpPolynomials3D, double omega0)
   {
      FrameTrajectory3D cmpPolynomial3D = cmpPolynomials3D.get(cmpPolynomials3D.size() - 1);
      entryCornerPointsToPack.clear();
      exitCornerPointsToPack.clear();

      // Get the terminal ICP location
      cmpPolynomial3D.compute(cmpPolynomial3D.getFinalTime());
      FramePoint3DReadOnly nextEntryCornerPoint = cmpPolynomial3D.getFramePosition();

      for (int i = cmpPolynomials3D.size() - 1; i >= 0; i--)
      {
         cmpPolynomial3D = cmpPolynomials3D.get(i);

         FixedFramePoint3DBasics exitCornerPoint = exitCornerPointsToPack.getAndGrowIfNeeded(i);
         FixedFramePoint3DBasics entryCornerPoint = entryCornerPointsToPack.getAndGrowIfNeeded(i);

         exitCornerPoint.set(nextEntryCornerPoint);

         computeDesiredCapturePointPosition(omega0, cmpPolynomial3D.getInitialTime(), exitCornerPoint, cmpPolynomial3D, entryCornerPoint);
         nextEntryCornerPoint = entryCornerPoint;
      }
   }

   public void computeDesiredCapturePointPosition3D(double omega0, double time, FramePoint3DReadOnly finalCapturePoint, FrameTrajectory3D cmpPolynomial3D,
                                                    FixedFramePoint3DBasics desiredCapturePointToPack)
   {
      calculateICPQuantityFromCorrespondingCMPPolynomial3D(omega0, time, 0, cmpPolynomial3D, finalCapturePoint, desiredCapturePointToPack);
   }

   public void computeDesiredCapturePointPosition(double omega0, double time, FramePoint3DReadOnly finalCapturePoint, FrameTrajectory3D cmpPolynomial3D,
                                                  FixedFramePoint3DBasics desiredCapturePointToPack)
   {
      for (Axis dir : Axis.values)
      {
         Trajectory cmpPolynomial = cmpPolynomial3D.getTrajectory(dir);
         double icpPositionDesired = calculateICPQuantityFromCorrespondingCMPPolynomial1D(omega0, time, 0, cmpPolynomial,
                                                                                          finalCapturePoint.getElement(dir.ordinal()));

         desiredCapturePointToPack.setElement(dir.ordinal(), icpPositionDesired);
      }
   }

   public void computeDesiredCapturePointVelocity(double omega0, double time, FramePoint3DReadOnly finalCapturePoint, FrameTrajectory3D cmpPolynomial3D,
                                                  FixedFrameVector3DBasics desiredCapturePointVelocityToPack)
   {
      for (Axis dir : Axis.values)
      {
         Trajectory cmpPolynomial = cmpPolynomial3D.getTrajectory(dir);
         double icpVelocityDesired = calculateICPQuantityFromCorrespondingCMPPolynomial1D(omega0, time, 1, cmpPolynomial,
                                                                                          finalCapturePoint.getElement(dir.ordinal()));

         desiredCapturePointVelocityToPack.setElement(dir.ordinal(), icpVelocityDesired);
      }
   }

   public void computeDesiredCapturePointAcceleration(double omega0, double time, FramePoint3DReadOnly finalCapturePoint, FrameTrajectory3D cmpPolynomial3D,
                                                      FixedFrameVector3DBasics desiredCapturePointAccelerationToPack)
   {
      for (Axis dir : Axis.values)
      {
         Trajectory cmpPolynomial = cmpPolynomial3D.getTrajectory(dir);
         double icpAccelerationDesired = calculateICPQuantityFromCorrespondingCMPPolynomial1D(omega0, time, 2, cmpPolynomial,
                                                                                              finalCapturePoint.getElement(dir.ordinal()));

         desiredCapturePointAccelerationToPack.setElement(dir.ordinal(), icpAccelerationDesired);
      }
   }

   /**
    * Variation of J. Englsberger's "Smooth trajectory generation and push-recovery based on DCM"
    * <br>
    * The approach for calculating DCMs is based on CMP polynomials instead of discrete waypoints
    */
   public void calculateICPQuantityFromCorrespondingCMPPolynomial3D(double omega0, double time, int icpDerivativeOrder, FrameTrajectory3D cmpPolynomial3D,
                                                                    FrameTuple3DReadOnly icpPositionDesiredFinal,
                                                                    FixedFrameTuple3DBasics icpQuantityDesiredToPack)
   {
      int numberOfCoefficients = cmpPolynomial3D.getNumberOfCoefficients();
      if (numberOfCoefficients == -1)
      {
         icpQuantityDesiredToPack.setToNaN();
         return;
      }

      initializeMatrices3D(numberOfCoefficients);
      setPolynomialCoefficientVector3D(polynomialCoefficientCombinedVector, cmpPolynomial3D);

      calculateGeneralizedAlphaPrimeOnCMPSegment3D(omega0, time, generalizedAlphaPrimeMatrix, icpDerivativeOrder, cmpPolynomial3D);
      calculateGeneralizedBetaPrimeOnCMPSegment3D(omega0, time, generalizedBetaPrimeMatrix, icpDerivativeOrder, cmpPolynomial3D);
      double generalizedGammaPrime = calculateGeneralizedGammaPrimeOnCMPSegment3D(omega0, time, icpDerivativeOrder, cmpPolynomial3D);
      CommonOps.subtract(generalizedAlphaPrimeMatrix, generalizedBetaPrimeMatrix, generalizedAlphaBetaPrimeMatrix);

      calculateICPQuantity3D(generalizedAlphaBetaPrimeMatrix, generalizedGammaPrime, polynomialCoefficientCombinedVector, icpPositionDesiredFinal,
                             icpQuantityDesiredToPack);
   }

   public double calculateICPQuantityFromCorrespondingCMPPolynomial1D(double omega0, double time, int icpDerivativeOrder, Trajectory cmpPolynomial,
                                                                      double icpPositionDesiredFinal)
   {
      int numberOfCoefficients = cmpPolynomial.getNumberOfCoefficients();

      initializeMatrices1D(numberOfCoefficients);
      setPolynomialCoefficientVector1D(polynomialCoefficientVector, cmpPolynomial);

      calculateGeneralizedAlphaPrimeOnCMPSegment1D(omega0, time, generalizedAlphaPrimeMatrix, icpDerivativeOrder, cmpPolynomial);
      calculateGeneralizedBetaPrimeOnCMPSegment1D(omega0, time, generalizedBetaPrimeMatrix, icpDerivativeOrder, cmpPolynomial);
      double gammaPrimeDouble = calculateGeneralizedGammaPrimeOnCMPSegment1D(omega0, time, icpDerivativeOrder, cmpPolynomial);
      CommonOps.subtract(generalizedAlphaPrimeMatrix, generalizedBetaPrimeMatrix, generalizedAlphaBetaPrimeMatrix);

      return calculateICPQuantity1D(generalizedAlphaBetaPrimeMatrix, gammaPrimeDouble, polynomialCoefficientVector, icpPositionDesiredFinal);
   }

   /**
    * Compute the i-th derivative of &xi;<sub>ref,&phi;</sub> at time t<sub>&phi;</sub>:
    * <P>
    * &xi;<sup>(i)</sup><sub>ref,&phi;</sub>(t<sub>&phi;</sub>) =
    * (&alpha;<sup>(i)</sup><sub>ICP,&phi;</sub>(t<sub>&phi;</sub>) -
    * &beta;<sup>(i)</sup><sub>ICP,&phi;</sub>(t<sub>&phi;</sub>)) * p<sub>&phi;</sub> +
    * &gamma;<sup>(i)</sup><sub>ICP,&phi;</sub>(t<sub>&phi;</sub>) *
    * &xi;<sub>ref,&phi;</sub>(T<sub>&phi;</sub>)
    */
   public void calculateICPQuantity3D(DenseMatrix64F generalizedAlphaBetaPrimeMatrix, double generalizedGammaPrime,
                                      DenseMatrix64F polynomialCoefficientCombinedVector, FrameTuple3DReadOnly icpPositionDesiredFinal,
                                      FixedFrameTuple3DBasics icpQuantityDesiredToPack)
   {
      int numRows = generalizedAlphaBetaPrimeMatrix.getNumRows();
      M1.reshape(numRows, 1);

      icpPositionDesiredFinal.get(M1);
      CommonOps.scale(generalizedGammaPrime, M1);
      CommonOps.multAdd(generalizedAlphaBetaPrimeMatrix, polynomialCoefficientCombinedVector, M1);

      icpQuantityDesiredToPack.set(M1);
   }

   public static double calculateICPQuantity1D(DenseMatrix64F generalizedAlphaBetaPrimeMatrix, double generalizedGammaPrime,
                                               DenseMatrix64F polynomialCoefficientVector, double icpPositionDesiredFinal)
   {
      return CommonOps.dot(generalizedAlphaBetaPrimeMatrix, polynomialCoefficientVector) + generalizedGammaPrime * icpPositionDesiredFinal;
   }

   /**
    * Compute the i-th derivative of &alpha;<sub>ICP,&phi;</sub> at time t<sub>&phi;</sub>:
    * <P>
    * &alpha;<sup>(i)</sup><sub>ICP,&phi;</sub>(t<sub>&phi;</sub>) =
    * &Sigma;<sub>j=0</sub><sup>n</sup> &omega;<sub>0</sub><sup>-j</sup> *
    * t<sup>(j+i)<sup>T</sup></sup> (t<sub>&phi;</sub>)
    */
   public static void calculateGeneralizedAlphaPrimeOnCMPSegment3D(double omega0, double time, DenseMatrix64F generalizedAlphaPrimeToPack,
                                                                   int alphaDerivativeOrder, FrameTrajectory3D cmpPolynomial3D)
   {
      int numberOfCoefficients = cmpPolynomial3D.getNumberOfCoefficients();

      generalizedAlphaPrimeToPack.zero();
      double omega0Inverse = 1.0 / omega0;
      double omega0Power = 1.0;

      for (int i = 0; i < numberOfCoefficients; i++)
      {
         for (Axis dir : Axis.values)
         {
            Trajectory cmpPolynomial = cmpPolynomial3D.getTrajectory(dir);
            DenseMatrix64F geometricSequenceDerivative = cmpPolynomial.evaluateGeometricSequenceDerivative(i + alphaDerivativeOrder, time);

            MatrixTools.addMatrixBlock(generalizedAlphaPrimeToPack, dir.ordinal(), dir.ordinal() * geometricSequenceDerivative.numCols,
                                       geometricSequenceDerivative, 0, 0, geometricSequenceDerivative.numRows, geometricSequenceDerivative.numCols,
                                       omega0Power);
         }
         omega0Power *= omega0Inverse;
      }
   }

   /**
    * Compute the i-th derivative of &alpha;<sub>ICP,&phi;</sub> at time t<sub>&phi;</sub>:
    * <pre>
    * &alpha;<sup>(i)</sup><sub>ICP,&phi;</sub>(t<sub>&phi;</sub>) = &Sigma;<sub>j=0</sub><sup>n</sup> &omega;<sub>0</sub><sup>-j</sup> * t<sup>(j+i)<sup>T</sup></sup>(t<sub>&phi;</sub>)
    * </pre>
    */
   public static void calculateGeneralizedAlphaPrimeOnCMPSegment3D(double omega0, double time, DenseMatrixVector3D generalizedAlphaPrimeToPack,
                                                                   int alphaDerivativeOrder, Trajectory3D cmpPolynomial3D)
   {
      int numberOfCoefficients = cmpPolynomial3D.getNumberOfCoefficients();

      for (Axis dir : Axis.values)
         generalizedAlphaPrimeToPack.getMatrix(dir).reshape(1, cmpPolynomial3D.getNumberOfCoefficients(dir));

      generalizedAlphaPrimeToPack.zero();
      double omega0Inverse = 1.0 / omega0;
      double omega0Power = 1.0;

      for (int i = 0; i < numberOfCoefficients; i++)
      {
         for (Axis dir : Axis.values)
         {
            Trajectory cmpPolynomial = cmpPolynomial3D.getTrajectory(dir);
            DenseMatrix64F matrix = generalizedAlphaPrimeToPack.getMatrix(dir);
            DenseMatrix64F geometricSequenceDerivative = cmpPolynomial.evaluateGeometricSequenceDerivative(i + alphaDerivativeOrder, time);
            CommonOps.addEquals(matrix, omega0Power, geometricSequenceDerivative);
         }
         omega0Power *= omega0Inverse;
      }
   }

   public static void calculateGeneralizedAlphaPrimeOnCMPSegment1D(double omega0, double time, DenseMatrix64F generalizedAlphaPrimeRow,
                                                                   int alphaDerivativeOrder, Trajectory cmpPolynomial)
   {
      int numberOfCoefficients = cmpPolynomial.getNumberOfCoefficients();

      generalizedAlphaPrimeRow.reshape(1, numberOfCoefficients);
      generalizedAlphaPrimeRow.zero();
      double omega0Inverse = 1.0 / omega0;
      double omega0Power = 1.0;

      for (int i = 0; i < numberOfCoefficients; i++)
      {
         CommonOps.addEquals(generalizedAlphaPrimeRow, omega0Power, cmpPolynomial.evaluateGeometricSequenceDerivative(i + alphaDerivativeOrder, time));
         omega0Power *= omega0Inverse;
      }
   }

   /**
    * Compute the i-th derivative of &beta;<sub>ICP,&phi;</sub> at time t<sub>&phi;</sub>:
    * <P>
    * &beta;<sup>(i)</sup><sub>ICP,&phi;</sub>(t<sub>&phi;</sub>) =
    * &Sigma;<sub>j=0</sub><sup>n</sup> &omega;<sub>0</sub><sup>-(j-i)</sup> *
    * t<sup>(j)<sup>T</sup></sup> (T<sub>&phi;</sub>) *
    * e<sup>&omega;<sub>0</sub>(t<sub>&phi;</sub>-T<sub>&phi;</sub>)</sup>
    */
   public static void calculateGeneralizedBetaPrimeOnCMPSegment3D(double omega0, double time, DenseMatrix64F generalizedBetaPrimeToPack,
                                                                  int betaDerivativeOrder, FrameTrajectory3D cmpPolynomial3D)
   {
      int numberOfCoefficients = cmpPolynomial3D.getNumberOfCoefficients();
      double timeSegmentTotal = cmpPolynomial3D.getFinalTime();

      generalizedBetaPrimeToPack.zero();

      double omega0Inverse = 1.0 / omega0;
      double omega0Power = SmoothCoMIntegrationToolbox.power(omega0, betaDerivativeOrder);
      double expOmega0Time = Math.exp(omega0 * (time - timeSegmentTotal));

      for (int i = 0; i < numberOfCoefficients; i++)
      {
         double scalar = omega0Power * expOmega0Time;

         for (Axis dir : Axis.values)
         {
            Trajectory cmpPolynomial = cmpPolynomial3D.getTrajectory(dir);
            DenseMatrix64F geometricSequenceDerivative = cmpPolynomial.evaluateGeometricSequenceDerivative(i, timeSegmentTotal);

            MatrixTools.addMatrixBlock(generalizedBetaPrimeToPack, dir.ordinal(), dir.ordinal() * geometricSequenceDerivative.numCols,
                                       geometricSequenceDerivative, 0, 0, geometricSequenceDerivative.numRows, geometricSequenceDerivative.numCols, scalar);
         }

         omega0Power *= omega0Inverse; // == Math.pow(omega0, betaDerivativeOrder - i)
      }
   }

   public static void calculateGeneralizedBetaPrimeOnCMPSegment1D(double omega0, double time, DenseMatrix64F generalizedBetaPrimeRowToPack,
                                                                  int betaDerivativeOrder, Trajectory cmpPolynomial)
   {
      int numberOfCoefficients = cmpPolynomial.getNumberOfCoefficients();
      double timeSegmentTotal = cmpPolynomial.getFinalTime();

      generalizedBetaPrimeRowToPack.reshape(1, numberOfCoefficients);
      generalizedBetaPrimeRowToPack.zero();

      double omega0Inverse = 1.0 / omega0;
      double omega0Power = SmoothCoMIntegrationToolbox.power(omega0, betaDerivativeOrder);
      double expOmega0Time = Math.exp(omega0 * (time - timeSegmentTotal));

      for (int i = 0; i < numberOfCoefficients; i++)
      {
         double scalar = omega0Power * expOmega0Time;
         CommonOps.addEquals(generalizedBetaPrimeRowToPack, scalar, cmpPolynomial.evaluateGeometricSequenceDerivative(i, timeSegmentTotal));
         omega0Power *= omega0Inverse; // == Math.pow(omega0, betaDerivativeOrder - i)
      }
   }

   /**
    * Compute the i-th derivative of &gamma;<sub>ICP,&phi;</sub> at time t<sub>&phi;</sub>:
    * <P>
    * &gamma;<sup>(i)</sup><sub>ICP,&phi;</sub>(t<sub>&phi;</sub>) = &omega;<sub>0</sub><sup>i</sup>
    * * e<sup>&omega;<sub>0</sub>(t<sub>&phi;</sub>-T<sub>&phi;</sub>)</sup>
    */
   public static double calculateGeneralizedGammaPrimeOnCMPSegment3D(double omega0, double time, int gammaDerivativeOrder, FrameTrajectory3D cmpPolynomial3D)
   {
      double timeSegmentTotal = cmpPolynomial3D.getFinalTime();
      return SmoothCoMIntegrationToolbox.power(omega0, gammaDerivativeOrder) * Math.exp(omega0 * (time - timeSegmentTotal));
   }

   public static double calculateGeneralizedGammaPrimeOnCMPSegment1D(double omega0, double time, int gammaDerivativeOrder, Trajectory cmpPolynomial)
   {
      double timeSegmentTotal = cmpPolynomial.getFinalTime();
      return SmoothCoMIntegrationToolbox.power(omega0, gammaDerivativeOrder) * Math.exp(omega0 * (time - timeSegmentTotal));
   }

   public static double calculateGeneralizedMatricesPrimeOnCMPSegment1D(double omega0, double time, int derivativeOrder, Trajectory cmpPolynomial,
                                                                        DenseMatrix64F generalizedAlphaPrime, DenseMatrix64F generalizedBetaPrime,
                                                                        DenseMatrix64F generalizedAlphaBetaPrime)
   {
      calculateGeneralizedAlphaPrimeOnCMPSegment1D(omega0, time, generalizedAlphaPrime, derivativeOrder, cmpPolynomial);
      calculateGeneralizedBetaPrimeOnCMPSegment1D(omega0, time, generalizedBetaPrime, derivativeOrder, cmpPolynomial);
      double generalizedGammaPrime = calculateGeneralizedGammaPrimeOnCMPSegment1D(omega0, time, derivativeOrder, cmpPolynomial);
      CommonOps.subtract(generalizedAlphaPrime, generalizedBetaPrime, generalizedAlphaBetaPrime);

      return generalizedGammaPrime;
   }

   private void initializeMatrices3D(int numberOfCoefficients)
   {
      initializeMatrices(3, numberOfCoefficients);
   }

   private void initializeMatrices1D(int numberOfCoefficients)
   {
      initializeMatrices(1, numberOfCoefficients);
   }

   private void initializeMatrices(int dimension, int numberOfCoefficients)
   {
      polynomialCoefficientCombinedVector.reshape(dimension * numberOfCoefficients, 1);
      polynomialCoefficientCombinedVector.zero();

      generalizedAlphaPrimeMatrix.reshape(dimension, dimension * numberOfCoefficients);
      generalizedAlphaPrimeMatrix.zero();

      generalizedBetaPrimeMatrix.reshape(dimension, dimension * numberOfCoefficients);
      generalizedBetaPrimeMatrix.zero();

      generalizedAlphaBetaPrimeMatrix.reshape(dimension, dimension * numberOfCoefficients);
      generalizedAlphaBetaPrimeMatrix.zero();
   }

   private void setPolynomialCoefficientVector3D(DenseMatrix64F polynomialCoefficientCombinedVectorToPack, FrameTrajectory3D cmpPolynomial3D)
   {
      int numRows = cmpPolynomial3D.getNumberOfCoefficients();
      int numCols = 1;
      for (Axis dir : Axis.values)
      {
         setPolynomialCoefficientVector1D(polynomialCoefficientVector, cmpPolynomial3D.getTrajectory(dir));
         MatrixTools.setMatrixBlock(polynomialCoefficientCombinedVectorToPack, dir.ordinal() * numRows, 0, polynomialCoefficientVector, 0, 0, numRows, numCols,
                                    1.0);
      }
   }

   private static void setPolynomialCoefficientVector1D(DenseMatrix64F polynomialCoefficientVectorToPack, Trajectory cmpPolynomial)
   {
      double[] polynomialCoefficients = cmpPolynomial.getCoefficients();

      polynomialCoefficientVectorToPack.setData(polynomialCoefficients);
      polynomialCoefficientVectorToPack.reshape(cmpPolynomial.getNumberOfCoefficients(), 1);
   }
}
