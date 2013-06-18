package util;

public class MathUtil {

	public static double[][] transpose(double[][] arr) {
		double[][] ret = new double[arr[0].length][arr.length];
		for(int i = 0; i < arr.length; i++)
			for(int j = 0; j < arr.length; j++)
				ret[j][i] = arr[i][j];
		return ret;
	}
	
	private static final double EPSILON = 1e-10;
	
	// Gaussian elimination with partial pivoting
    public static double[] lsolve(double[][] A, double[] b) {
        int N  = b.length;

        for (int p = 0; p < N; p++) {

            // find pivot row and swap
            int max = p;
            for (int i = p + 1; i < N; i++) {
                if (Math.abs(A[i][p]) > Math.abs(A[max][p])) {
                    max = i;
                }
            }
            double[] temp = A[p]; A[p] = A[max]; A[max] = temp;
            double   t    = b[p]; b[p] = b[max]; b[max] = t;

            // singular or nearly singular
            if (Math.abs(A[p][p]) <= EPSILON) {
                throw new RuntimeException("Matrix is singular or nearly singular");
            }

            // pivot within A and b
            for (int i = p + 1; i < N; i++) {
                double alpha = A[i][p] / A[p][p];
                b[i] -= alpha * b[p];
                for (int j = p; j < N; j++) {
                    A[i][j] -= alpha * A[p][j];
                }
            }
        }

        // back substitution
        double[] x = new double[N];
        for (int i = N - 1; i >= 0; i--) {
            double sum = 0.0;
            for (int j = i + 1; j < N; j++) {
                sum += A[i][j] * x[j];
            }
            x[i] = (b[i] - sum) / A[i][i];
        }
        return x;
    }
    
    // Min of an array
    public static long min(long[] arr) {
    	long min = Long.MAX_VALUE;
    	for(long n : arr)
    		if(n < min)
    			min = n;
    	return min;
    }
    
	public static double average(double[] data){
		double sum = 0;
		for(double d : data)
			sum += Math.abs(d);
		return sum / data.length;
	}
	
	public static double rms(double[] data){
		double sum = 0;
		for(double d : data)
			sum += Math.pow(d, 2);
		return Math.sqrt(sum / data.length);
	}
	
	public static double norm(double[] data) {
		double norm = 0;
		for(double d : data)
			norm += Math.pow(d, 2);
		norm = Math.sqrt(norm);
		return norm;
	}
	
	// Normalize an array
	public static double[] normalize(double[] data) {
		double norm = MathUtil.norm(data);
		if(norm > 0)
			for(int i = 0; i < data.length; i++)
				data[i] /= norm;
		return data;
	}

	// Normalize a column in a 2d array
	public static double[][] normalize(double[][] data, int c) {
		double norm = 0;
		for(int r = 0; r < data.length; r++)
			norm += Math.pow(data[r][c], 2);
		norm = Math.sqrt(norm);
		if(norm > 0)
			for(int r = 0; r < data.length; r++)
				data[r][c] /= norm;
		return data;
	}
}
