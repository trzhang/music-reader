package util;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Vector;

import org.ejml.alg.dense.mult.MatrixMatrixMult;
import org.ejml.alg.dense.mult.MatrixVectorMult;
import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.LinearSolver;
import org.ejml.factory.LinearSolverFactory;
import org.ejml.ops.CommonOps;

public class StateEstimator implements KalmanFilter {

	// kinematics description
    private DenseMatrix64F F;
    private DenseMatrix64F Q;
    private DenseMatrix64F H;

    // sytem state estimate
    private DenseMatrix64F x;
    private DenseMatrix64F P;

    // these are predeclared for efficency reasons
    private DenseMatrix64F a,b;
    private DenseMatrix64F y,S,S_inv,c,d;
    private DenseMatrix64F K;
    
    private LinearSolver<DenseMatrix64F> solver;
    
    private State[] states;
    private int lo;
    private int hi;
	
	public StateEstimator(String filename, int lo, int hi) {
		MidiSequencer ms = new MidiSequencer(filename);
		LinkedList<State> states = ms.sequence(100);
		this.states = new State[states.size()];
		this.states = states.toArray(this.states);
		this.lo = lo;
		this.hi = hi;
	}

	public void configure() {
		double[][] f = new double[states.length][states.length];
		for(int c = 0; c < states.length - 1; c++) {
			f[c + 1][c] = states[c].getTransitionProb();
			f[c][c] = 1 - f[c + 1][c];
		}
		f[states.length - 1][states.length - 1] = 1;
		F = new DenseMatrix64F(f);
		
		double[][] q = new double[states.length][states.length];
		for(int i = 0; i < states.length; i++)
			q[i][i] = 0.1;
		Q = new DenseMatrix64F(q);
		
		double[][] h = new double[hi - lo][states.length];
		for(int c = 0; c < states.length; c++) {
			for(int note : states[c].getNotesPlayed()) {
				for(int r = 0; r < hi - lo; r++) {
					h[r][c] += MusicUtil.TEST_DATA[note][lo + r];
				}
			}
			MathUtil.normalize(h, c);
		}
		H = new DenseMatrix64F(h);
		
		int dimenX = F.numCols;
        int dimenZ = H.numRows;

        a = new DenseMatrix64F(dimenX,1);
        b = new DenseMatrix64F(dimenX,dimenX);
        y = new DenseMatrix64F(dimenZ,1);
        S = new DenseMatrix64F(dimenZ,dimenZ);
        S_inv = new DenseMatrix64F(dimenZ,dimenZ);
        c = new DenseMatrix64F(dimenZ,dimenX);
        d = new DenseMatrix64F(dimenX,dimenZ);
        K = new DenseMatrix64F(dimenX,dimenZ);

        // covariance matrices are symmetric positive semi-definite
        solver = LinearSolverFactory.symmPosDef(dimenX);
        // wrap the solver so that it doesn't modify the input
//        solver = new LinearSolverSafe<DenseMatrix64F>(solver);
        // A little bit more performance can be gained by letting S be modified.  In some
        // applications S should not be modified.

        x = new DenseMatrix64F(dimenX,1);
        P = new DenseMatrix64F(dimenX,dimenX);
	}

	public void setState(DenseMatrix64F x, DenseMatrix64F P) {
		this.x.set(x);
		this.P.set(P);
	}

	public DenseMatrix64F predict() {
		// x = F x
        MatrixVectorMult.mult(F,x,a);
        x.set(a);

        // P = F P F' + Q
        MatrixMatrixMult.mult_small(F,P,b);
        MatrixMatrixMult.multTransB(b,F, P);
        CommonOps.addEquals(P,Q);
        
//        renormalize(x);
        return x;
	}

	public void update(DenseMatrix64F z, DenseMatrix64F R) {
		// y = z - H x
        MatrixVectorMult.mult(H,x,y);
        CommonOps.sub(z,y,y);

        // S = H P H' + R
        MatrixMatrixMult.mult_small(H,P,c);
        MatrixMatrixMult.multTransB(c,H,S);
        CommonOps.addEquals(S,R);

        // K = PH'S^(-1)
        if( !solver.setA(S) ) throw new RuntimeException("Invert failed");
        solver.invert(S_inv);
        MatrixMatrixMult.multTransA_small(H,S_inv,d);
        MatrixMatrixMult.mult_small(P,d,K);

        // x = x + Ky
        MatrixVectorMult.mult(K,y,a);
        CommonOps.addEquals(x,a);

        // P = (I-kH)P = P - (KH)P = P-K(HP)
        MatrixMatrixMult.mult_small(H,P,c);
        MatrixMatrixMult.mult_small(K,c,b);
        CommonOps.subEquals(P,b);
        
//        renormalize(x);
	}

	public void renormalize(DenseMatrix64F x) {
		double norm = 0;
		for(int i = 0; i < x.getNumElements(); i++) {
			if(x.get(i) > 0)
				norm += x.get(i); // probabilities add to 1
			else
				x.set(i, 0);
		}
		norm = Math.sqrt(norm);
		if(norm > 0)
			for(int i = 0; i < x.getNumElements(); i++)
				x.div(i, norm);
	}
	
	public DenseMatrix64F getState() {
		return x;
	}

	public DenseMatrix64F getCovariance() {
		return P;
	}
	
	public int getNumStates() {
		return states.length;
	}
	
	public static void main(String[] args) {
		StateEstimator se = new StateEstimator("fur_elise.mid", 0, 88);
		se.configure();
		double[][] initState = new double[se.getNumStates()][1];
		initState[1][0] = 1;
		DenseMatrix64F x = new DenseMatrix64F(initState);
		double[][] initP = new double[se.getNumStates()][se.getNumStates()];
		for(int i = 0; i < se.getNumStates(); i++)
			initP[i][i] = 1;
		DenseMatrix64F P = new DenseMatrix64F(initP);
		se.setState(x, P);
		
		double[][] e2 = new double[88][1];
		for(int i = 0; i < 88; i++)
			e2[i][0] = MusicUtil.TEST_DATA[55][i];
		DenseMatrix64F e = new DenseMatrix64F(e2);
		double[][] r = new double[88][88];
		for(int i = 0; i < 88; i++)
			r[i][i] = 0.2;
		DenseMatrix64F R = new DenseMatrix64F(r);
		se.update(e, R);
		se.predict();
		double[][] ds2 = new double[88][1];
		for(int i = 0; i < 88; i++)
			ds2[i][0] = MusicUtil.TEST_DATA[54][i];
		DenseMatrix64F ds = new DenseMatrix64F(ds2);
//		for(int i = 0; i < 10; i++) {
//			se.update(ds, R);
//			se.predict();
//		}
//		for(int i = 0; i < 10; i++) {
//			se.update(e, R);
//			se.predict();
//		}
//		for(int i = 0; i < 10; i++) {
//			se.update(ds, R);
//			se.predict();
//		}
//		for(int i = 0; i < 10; i++) {
//			se.update(e, R);
//			se.predict();
//		}
		se.update(ds, R);
		System.out.println(se.predict());
	}
}
