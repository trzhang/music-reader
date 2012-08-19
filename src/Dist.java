import java.util.Arrays;
import java.util.HashMap;


public class Dist {
	
	private int start, end;
	private HashMap<Integer, Double> probs;
	
	public Dist(int start, int end) {
		this.start = start;
		this.end = end;
	}
	
	public Dist(int start, int end, HashMap<Integer, Double> probs) {
		this.start = start;
		this.end = end;
		this.setProbs(probs);
	}

	public double prob(int val) {
		return this.probs.get(val);
	}

	public void setProbs(HashMap<Integer, Double> probs) {
		this.probs = probs;
	}
	
}

class UniformDist extends Dist {
	public UniformDist(int start, int end){
		super(start, end);
		HashMap<Integer, Double> probs = new HashMap<Integer, Double>();
		double p = 1. / (end - start + 1);
		for(int i = start; i <= end; i++)
			probs.put(i, p);
		this.setProbs(probs);
	}
}

class DeltaDist extends Dist {
	public DeltaDist(int start, int end, int val){
		super(start, end);
		HashMap<Integer, Double> probs = new HashMap<Integer, Double>();
		for(int i = start; i <= end; i++)
			probs.put(i, 0.);
		probs.put(val, 1.);
		this.setProbs(probs);
	}
}