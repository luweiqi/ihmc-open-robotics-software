package us.ihmc.robotics.linearAlgebra.careSolvers;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.EjmlUnitTests;
import org.junit.jupiter.api.Test;
import us.ihmc.matrixlib.NativeCommonOps;

import java.util.ArrayList;
import java.util.List;

import static us.ihmc.robotics.Assert.assertEquals;

public class NewCARESolversTest
{
   private static final double epsilon = 1e-4;

   private List<NewCARESolver> getSolvers()
   {
      List<NewCARESolver> solvers = new ArrayList<>();
      solvers.add(new NewEigenvectorCARESolver());
      solvers.add(new NewNewtonCARESolver(new NewEigenvectorCARESolver()));
      solvers.add(new NewNewton2CARESolver(new NewEigenvectorCARESolver()));
      solvers.add(new NewDefectCorrectionCARESolver(new NewEigenvectorCARESolver()));

      solvers.add(new NewSignFunctionCARESolver());
      solvers.add(new NewNewtonCARESolver(new NewSignFunctionCARESolver()));
      solvers.add(new NewNewton2CARESolver(new NewSignFunctionCARESolver()));
      solvers.add(new NewDefectCorrectionCARESolver(new NewSignFunctionCARESolver()));

      return solvers;
   }

   @Test
   public void testSimple()
   {
      for (NewCARESolver solver : getSolvers())
      {
         DenseMatrix64F A = CommonOps.identity(2);
         DenseMatrix64F B = CommonOps.identity(2);
         DenseMatrix64F Q = CommonOps.identity(2);
         DenseMatrix64F R = CommonOps.identity(2);

         DenseMatrix64F AInput = new DenseMatrix64F(A);
         DenseMatrix64F BInput = new DenseMatrix64F(B);
         DenseMatrix64F QInput = new DenseMatrix64F(Q);
         DenseMatrix64F RInput = new DenseMatrix64F(R);

         solver.setMatrices(A, B, CommonOps.identity(2), CommonOps.identity(2), Q, R, null);
         solver.computeP();

         DenseMatrix64F assembledQ = new DenseMatrix64F(2, 2);
         CommonOps.multTransA(A, solver.getP(), assembledQ);
         CommonOps.multAdd(solver.getP(), A, assembledQ);

         DenseMatrix64F RInv = new DenseMatrix64F(2, 2);
         NativeCommonOps.invert(R, RInv);
         DenseMatrix64F BTransposeP = new DenseMatrix64F(2, 2);
         CommonOps.multTransA(B, solver.getP(), BTransposeP);
         DenseMatrix64F BRInv = new DenseMatrix64F(2, 2);
         CommonOps.mult(B, RInv, BRInv);
         DenseMatrix64F PBRInv = new DenseMatrix64F(2, 2);
         CommonOps.mult(solver.getP(), BRInv, PBRInv);

         DenseMatrix64F PBRInvBTransposeP = new DenseMatrix64F(2, 2);
         CommonOps.mult(PBRInv, BTransposeP, PBRInvBTransposeP);

         CommonOps.addEquals(assembledQ, -1.0, PBRInvBTransposeP);

         CommonOps.scale(-1.0, assembledQ);

         EjmlUnitTests.assertEquals(AInput, A, epsilon);
         EjmlUnitTests.assertEquals(BInput, B, epsilon);
         EjmlUnitTests.assertEquals(QInput, Q, epsilon);
         EjmlUnitTests.assertEquals(RInput, R, epsilon);

         assertIsSymmetric(solver.getP(), epsilon);
         assertSolutionIsValid(AInput, BInput, QInput, RInput, solver.getP(), epsilon);
      }
   }

   @Test
   public void testMatlabCare()
   {
      for (NewCARESolver solver : getSolvers())
      {
         int n = 2;
         int m = 1;
         DenseMatrix64F A = new DenseMatrix64F(n, n);
         DenseMatrix64F B = new DenseMatrix64F(n, m);
         DenseMatrix64F C = new DenseMatrix64F(1, 2);
         DenseMatrix64F Q = new DenseMatrix64F(2, 2);
         DenseMatrix64F R = new DenseMatrix64F(1, 1);
         A.set(0, 0, -3);
         A.set(0, 1, 2);
         A.set(1, 0, 1);
         A.set(1, 1, 1);
         B.set(1, 0, 1);
         C.set(0, 0, 1);
         C.set(0, 1, -1);
         R.set(0, 0, 3);

         CommonOps.multInner(C, Q);

         solver.setMatrices(A, B, CommonOps.identity(n), CommonOps.identity(n), Q, R, null);
         solver.computeP();

         DenseMatrix64F PExpected = new DenseMatrix64F(2, 2);
         PExpected.set(0, 0, 0.5895);
         PExpected.set(0, 1, 1.8216);
         PExpected.set(1, 0, 1.8216);
         PExpected.set(1, 1, 8.8188);

         DenseMatrix64F P = solver.getP();

         //      assertIsSymmetric(P);
         assertSolutionIsValid(A, B, Q, R, P, epsilon);
         EjmlUnitTests.assertEquals(PExpected, P, 1e-4);
      }
   }

   @Test
   public void testMatlabCare2()
   {
      for (NewCARESolver solver : getSolvers())
      {
         int n = 3;
         int m = 1;
         DenseMatrix64F A = new DenseMatrix64F(n, n);
         DenseMatrix64F B = new DenseMatrix64F(n, m);
         DenseMatrix64F C = new DenseMatrix64F(1, n);
         DenseMatrix64F E = CommonOps.identity(n);
         DenseMatrix64F Q = new DenseMatrix64F(n, n);
         DenseMatrix64F R = new DenseMatrix64F(1, 1);
         A.set(0, 0, 1);
         A.set(0, 1, -2);
         A.set(0, 2, 3);
         A.set(1, 0, -4);
         A.set(1, 1, 5);
         A.set(1, 2, 6);
         A.set(2, 0, 7);
         A.set(2, 1, 8);
         A.set(2, 2, 9);

         B.set(0, 0, 5);
         B.set(1, 0, 6);
         B.set(2, 0, -7);
         C.set(0, 0, 7);
         C.set(0, 1, -8);
         C.set(0, 2, 9);
         R.set(0, 0, 1);

         CommonOps.multInner(C, Q);

         solver.setMatrices(A, B, CommonOps.identity(n), E, Q, R, null);
         solver.computeP();

         DenseMatrix64F RInverse = new DenseMatrix64F(m, m);
         DenseMatrix64F BTranspose = new DenseMatrix64F(m, n);
         DenseMatrix64F M = new DenseMatrix64F(n, n);

         NativeCommonOps.invert(R, RInverse);
         CommonOps.transpose(B, BTranspose);
         NativeCommonOps.multQuad(BTranspose, RInverse, M);

         //      assertIsSymmetric(solver.getP());
         assertSolutionIsValid(A, B, Q, R, solver.getP(), epsilon);
      }
   }

   private static void assertIsSymmetric(DenseMatrix64F A, double epsilon)
   {
      for (int row = 0; row < A.getNumRows(); row++)
      {
         for (int col = 0; col < A.getNumCols(); col++)
         {
            assertEquals("Not symmetric!", A.get(row, col), A.get(col, row), epsilon);
         }
      }
   }

   static void assertSolutionIsValid(DenseMatrix64F A, DenseMatrix64F B, DenseMatrix64F Q, DenseMatrix64F R, DenseMatrix64F P, double epsilon)
   {
      int n = A.getNumRows();
      int m = B.getNumCols();
      DenseMatrix64F PDot = new DenseMatrix64F(n, n);
      DenseMatrix64F M = new DenseMatrix64F(m, m);
      DenseMatrix64F BTranspose = new DenseMatrix64F(m, n);
      CommonOps.transpose(B, BTranspose);

      CARETools.computeM(BTranspose, R, null, M);
      CARETools.computeRiccatiRate(P, A, Q, M, PDot);

      EjmlUnitTests.assertEquals(new DenseMatrix64F(n, n), PDot, epsilon);
   }
}
