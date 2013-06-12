public class MusicUtil {
	public static double[] frequencies = {27.5, 29.1352, 30.8677, 32.7032, 34.6478, 36.7081, 38.8909, 41.2034, 43.6535, 46.2493, 48.9994, 51.9131, 55, 58.2705, 61.7354, 65.4064, 69.2957, 73.4162, 77.7817, 82.4069, 87.3071, 92.4986, 97.9989, 103.826, 110, 116.541, 123.471, 130.813, 138.591, 146.832, 155.563, 164.814, 174.614, 184.997, 195.998, 207.652, 220, 233.082, 246.942, 261.626, 277.183, 293.665, 311.127, 329.628, 349.228, 369.994, 391.995, 415.305, 440, 466.164, 493.883, 523.251, 554.365, 587.33, 622.254, 659.255, 698.456, 739.989, 783.991, 830.609, 880, 932.328, 987.767, 1046.5, 1108.73, 1174.66, 1244.51, 1318.51, 1396.91, 1479.98, 1567.98, 1661.22, 1760, 1864.66, 1975.53, 2093, 2217.46, 2349.32, 2489.02, 2637.02, 2793.83, 2959.96, 3135.96, 3322.44, 3520, 3729.31, 3951.07, 4186.01};
	public static String[] noteNames = {"A0", "A#/Bb0", "B0", "C1", "C#/Db1", "D1", "D#/Eb1", "E1", "F1", "F#/Gb1", "G1", "G#/Ab1", "A1", "A#/Bb1", "B1", "C2", "C#/Db2", "D2", "D#/Eb2", "E2", "F2", "F#/Gb2", "G2", "G#/Ab2", "A2", "A#/Bb2", "B2", "C3", "C#/Db3", "D3", "D#/Eb3", "E3", "F3", "F#/Gb3", "G3", "G#/Ab3", "A3", "A#/Bb3", "B3", "C4", "C#/Db4", "D4", "D#/Eb4", "E4", "F4", "F#/Gb4", "G4", "G#/Ab4", "A4", "A#/Bb4", "B4", "C5", "C#/Db5", "D5", "D#/Eb5", "E5", "F5", "F#/Gb5", "G5", "G#/Ab5", "A5", "A#/Bb5", "B5", "C6", "C#/Db6", "D6", "D#/Eb6", "E6", "F6", "F#/Gb6", "G6", "G#/Ab6", "A6", "A#/Bb6", "B6", "C7", "C#/Db7", "D7", "D#/Eb7", "E7", "F7", "F#/Gb7", "G7", "G#/Ab7", "A7", "A#/Bb7", "B7", "C8"};
	public static double[][] testData = {{0.7389660582721981,0.4009656984660385,0.11463517405851144,0.1327790532981452,0.09310359937121139,0.0444350347549948,0.06762266606044143,0.03652341019171467,0.024822311090999243,0.03866249797325996,0.05139529042777267,0.06833818917775973,0.2660561779117191},
	{0.4643760135000154,0.7229008326195154,0.40563997032932986,0.14344375116815153,0.1206468679414877,0.09251784929012825,0.05026584778759549,0.05476267122168668,0.05742334658294098,0.03475635764279355,0.017384845273357014,0.021897650723061338,0.021836869079315983},
	{0.09209096228562004,0.5139863094882409,0.7340465595653692,0.36385980611958363,0.175326952042789,0.09310822536381302,0.09280559461888847,0.053870684506157084,0.016817388358321854,0.03654013542175093,0.03640123817269128,0.03814696896945869,0.028234021856505653},
	{0.12733417733188684,0.07995414191026588,0.46208907430622415,0.7453686248946622,0.3920793489488433,0.20467663691300414,0.04753201116760146,0.07253533577235949,0.061097194007612494,0.031140219622975654,0.016026631826180002,0.010406379016643074,0.009247068019027754},
	{0.05299413121055752,0.10908179836203283,0.08641412839343904,0.4580358432808819,0.7682214031924177,0.3614250823928766,0.18069601434323077,0.06664925766996678,0.04114598576451412,0.06022612471568518,0.030694730921687542,0.04197940266086055,0.04602765861180349},
	{0.04794711695690331,0.07563361889764353,0.10382057196060471,0.15001466463152358,0.43484537066261786,0.7317447125524782,0.34432999068664816,0.20249317123979158,0.05355689372890513,0.07550873219931142,0.10867033819457995,0.15612695010663383,0.17272896347699784},
	{0.05192190073898519,0.006461647851469268,0.08348495231137505,0.03438426668258973,0.17081676923409014,0.36470303722094605,0.8340825008157791,0.3092985927826592,0.1744192219947441,0.06865018655845449,0.01763158652084764,0.00604238235805218,0.00906312612759029},
	{0.013790856076378472,0.0625521438956193,0.020487658821954455,0.09230273630048491,0.07985945531286398,0.1481788996569949,0.5081818106856963,0.8108020135935139,0.11875463472397678,0.12828531161090317,0.08379046273788122,0.05960562443156139,0.04290534781853806},
	{0.0694688021267207,0.04922213253783732,0.04336176008510312,0.043123991187663566,0.09098626617905349,0.05651089414941012,0.20808939653389646,0.4796839987979889,0.8117812117052274,0.12938244642629457,0.12540718488745387,0.07544855738855648,0.0836267238875357},
	{0.03212283526120768,0.031201925126278552,0.05305076540178386,0.02719863932279108,0.07816799334951521,0.09324418076192703,0.07165238629490545,0.2093345976146484,0.4116189100136113,0.8609126894071482,0.10442825280095601,0.08476211594364749,0.04460942542365097},
	{0.03422427983588835,0.01680737911447887,0.04521741332931081,0.04317637212269732,0.08580861030972131,0.11201933017321945,0.04375488309103486,0.09566755419007235,0.20153395127705911,0.35302123399385116,0.8851599003562065,0.1065532602544858,0.05964567718642851},
	{0.022996733620574247,0.021863150332942054,0.026059460195208342,0.033004263691542295,0.018490178474425616,0.051995383039432366,0.035702849747974476,0.018554228011789572,0.09261319535314146,0.18876367502161917,0.33106845726652206,0.8847530364578968,0.13253253832920817},
	{0.013618584509210408,0.019415963337048474,0.01999160302377749,0.02549080651361462,0.019122405736375466,0.04636608423297045,0.038381014767195167,0.022938112433333994,0.05596983760902463,0.08805034013298732,0.1421521957159087,0.2538058495660848,0.864236281268074}};
	
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

}
