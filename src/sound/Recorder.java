package sound;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import util.MusicUtil;


public class Recorder extends Thread{
	
	private TargetDataLine line;
	private AudioFormat format;
	private DataLine.Info info;
	private ByteArrayOutputStream out;
	private boolean recording;
	private boolean paused;
	private boolean training;
	private Object lock;
	private double level;
	private int bufferSize;
	private double[] buffer;
	private double[] frequencySpectrum;
	private double[] noiseThresholds;
	private double[][] trainData;
	private double[] defaultDist;
	private int[] learningRateReciprocals;
	private double noiseThreshold;
	private int lo, hi;
	private int noteLabelCounter;
	
	public Recorder(){
		line = null;
		format = new AudioFormat(16000, 16, 1, true, true);
		info = new DataLine.Info(TargetDataLine.class, format);
		recording = false;
		paused = false;
		training = false;
		lock = new Object();
		level = 0;
		bufferSize = -1;
		frequencySpectrum = new double[88];
		noiseThresholds = new double[88];
		trainData = new double[88][88];
		defaultDist = new double[] {0.25, 0.5, 1, 0.5, 0.25};
		for(int i = 0; i < 88; i++) {
			for(int j = -2; j <= 2; j++)
				if(i + j >= 0 && i + j < 88)
					trainData[i][i + j] = defaultDist[j + 2];
			normalize(trainData[i]);
		}
		learningRateReciprocals = new int[88];
		Arrays.fill(learningRateReciprocals, 5);
		noiseThreshold = 0.05;
		lo = 0;//39;
		hi = 88;//52;
		noteLabelCounter = lo;
	}
	
	public void init(){
		if (!AudioSystem.isLineSupported(info)) {
		    System.out.println("TargetDataLine unsupported.");
		    System.exit(0);
		}
		// Obtain and open the line.
		try {
		    line = (TargetDataLine) AudioSystem.getLine(info);
		    line.open(format);
		} catch (LineUnavailableException ex) {
		    System.out.println("TargetDataLine unavailable.");
		    System.exit(0);
		}
		if(bufferSize == -1){
			bufferSize = Math.min(512, 8 * line.getBufferSize() / (format.getSampleSizeInBits() * format.getChannels()));
			buffer = new double[bufferSize];
		}
		calibrate();
		synchronized(lock) {
			while(!recording) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		startRecording();
	}
	
	public void calibrate(){
		int n = 100;
		double sum = 0;
		double[] buffer = new double[bufferSize];
		System.out.println("Calibrating...");
		byte[] data = new byte[bufferSize * format.getSampleSizeInBits() * format.getChannels() / 8]; //line.getBufferSize() / 20;
		line.start();
		for(int count = 0; count < n; count++){
			double max = Double.MIN_VALUE;
			line.read(data, 0, data.length);
			convertRawDataToBuffer(data, buffer);
			for(int i = lo; i < hi; i++) {
				double delta = dft(buffer, MusicUtil.FREQUENCIES[i]);
				noiseThresholds[i] += delta;
				if(delta > max)
					max = delta;
			}
			sum += max;
		}
		for(int i = lo; i < hi; i++)
			noiseThresholds[i] /= Double.valueOf(n);
		noiseThreshold = sum / Double.valueOf(n);
		System.out.println("Done");
//		System.out.println(Arrays.toString(noiseThresholds));
	}
	
	public void startRecording(){
		System.out.println("Recording...");
		out = new ByteArrayOutputStream();
		int numBytesRead;
		byte[] data = new byte[bufferSize * format.getSampleSizeInBits() * format.getChannels() / 8]; //line.getBufferSize() / 20;

		// Begin audio capture.
		line.start();

		int count = 0;
		
		// Here, recording is a global boolean set by another thread.
		while(recording){
			// Read the next chunk of data from the TargetDataLine.
			numBytesRead = line.read(data, 0, data.length);
			convertRawDataToBuffer(data, buffer);
			buffer = highPass(buffer, MusicUtil.FREQUENCIES[19]);
			setLevel(rms(buffer));
			for(int i = lo; i < hi; i++)
				frequencySpectrum[i] = Math.max(dft(buffer, MusicUtil.FREQUENCIES[i]) - noiseThresholds[i], 0);
			double[] normalizedFrequencySpectrum = normalize(Arrays.copyOf(frequencySpectrum, 88));
			double[] trimmedFrequencySpectrum = new double[hi - lo];
			for(int i = 0; i < hi - lo; i++)
				trimmedFrequencySpectrum[i] = frequencySpectrum[lo + i];
//			double[] transformedFrequencySpectrum = MusicUtil.lsolve(MusicUtil.transpose(MusicUtil.TEST_DATA), trimmedFrequencySpectrum);
//			System.out.println(Arrays.toString(trimmedFrequencySpectrum));
//			System.out.println(Arrays.toString(transformedFrequencySpectrum));
			if(noteLikelyPlayed())
				System.out.println(MusicUtil.NOTE_NAMES[argmax(frequencySpectrum)]);
			if(training) {
				if(noteLikelyPlayed() && Math.random() < 0.1) {
					int note = noteLabelCounter;
//					int note = argmax(frequencySpectrum);
					for(int j = lo; j < hi; j++)
						trainData[note][j] = (1 - 1. / learningRateReciprocals[note]) * trainData[note][j] + 1. / learningRateReciprocals[note] * normalizedFrequencySpectrum[j];
					normalize(trainData[note]);
					learningRateReciprocals[note]++;
				}
			}
//			System.out.println(getNoteTrainingProgress());
//			for(int i = 0; i < hi - lo; i++)
//				frequencySpectrum[lo + i] = transformedFrequencySpectrum[i];
			// Save this chunk of data.
			out.write(data, 0, numBytesRead);
			count++;
			synchronized(lock) {
				while(paused) {
					try {
						lock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
		processRecording(count * bufferSize);
	}
	
	public double[] highPass(double[] buffer, double thresholdFreq) {
		double[] ret = new double[buffer.length];
		double rc = 2 * Math.PI * thresholdFreq;
		double dt = 1. / format.getSampleRate();
		double a = rc / (rc + dt);
		ret[0] = buffer[0];
		for(int i = 1; i < buffer.length; i++)
			ret[i] = a * ret[i - 1] + a * (buffer[i] - buffer[i - 1]);
		return ret;
	}
	
	// Convert java audio data into usable buffer data
	public void convertRawDataToBuffer(byte[] data, double[] buffer){
		double max = Math.pow(2, format.getSampleSizeInBits() - 1);
		if(format.getSampleSizeInBits() == 8){
			if(format.getChannels() == 1)
				for(int i = 0; i < buffer.length; i++)
					buffer[i] = data[i] / max;
			else if(format.getChannels() == 2)
				for(int i = 0; i < buffer.length; i++)
					buffer[i] = (data[2 * i] + data[2 * i + 1]) / (2 * max);
		} else if(format.getSampleSizeInBits() == 16){
			if(format.getChannels() == 1)
				for(int i = 0; i < buffer.length; i++)
					buffer[i] = (256 * data[2 * i] + data[2 * i + 1]) / max;
			else if(format.getChannels() == 2)
				for(int i = 0; i < buffer.length; i++)
					buffer[i] = (256 * data[4 * i] + data[4 * i + 1] + 256 * data[4 * i + 2] + data[4 * i + 3]) / (2 * max);
		}
	}
	
	// DFT on time domain buffer data
	public double dft(double[] buffer, double frequency) {
		double a = 0, b = 0;
		double cFreq = 2 * Math.PI * frequency;
		double dt = 1. / format.getSampleRate();
		for(int i = 0; i < buffer.length; i++){
			a += buffer[i] * Math.cos(cFreq * i * dt);
			b += buffer[i] * Math.sin(cFreq * i * dt);
		}
		return Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2)) / 88.;
	}
	
	public double average(double[] data){
		double sum = 0;
		for(double d : data)
			sum += Math.abs(d);
		return sum / data.length;
	}
	
	public double rms(double[] data){
		double sum = 0;
		for(double d : data)
			sum += Math.pow(d, 2);
		return Math.sqrt(sum / data.length);
	}
	
	// Normalize an array
	public double[] normalize(double[] data) {
		double norm = 0;
		for(double d : data)
			norm += Math.pow(d, 2);
		norm = Math.sqrt(norm);
		for(int i = 0; i < data.length; i++)
			data[i] /= norm;
		return data;
	}
	
	public boolean noteLikelyPlayed() {
		double max = Double.MIN_VALUE;
		for(double d : frequencySpectrum) {
			if(d > max)
				max = d;
		}
		return max > 2 * noiseThreshold;
	}
	
	// Post processing (write audio file)
	public void processRecording(int length){
		System.out.println("Processing...");
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        AudioInputStream ais = new AudioInputStream(in, format, (int) length);
        try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
        line.close();
        try {
			AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File("out.wav"));
		} catch (IOException e) {
			e.printStackTrace();
		}
        System.out.println("Done.");
        init();
	}
	
	public void pauseRecording(){
		setPaused(!paused);
		if(paused){
			setRecording(false);
			System.out.println("Paused.");
		}
		else{
			setRecording(true);
			System.out.println("Recording...");
		}
	}
	
	public TargetDataLine getLine() {
		return line;
	}

	public AudioFormat getFormat() {
		return format;
	}

	public DataLine.Info getInfo() {
		return info;
	}

	public ByteArrayOutputStream getOut() {
		return out;
	}

	public boolean isRecording() {
		return recording;
	}

	public void setRecording(boolean recording) {
		this.recording = recording;
		if(recording)
			synchronized(lock) {
				lock.notify();
			}
	}
	
	public void run(){
		init();
	}

	public boolean isPaused() {
		return paused;
	}

	public void setPaused(boolean paused) {
		this.paused = paused;
	}

	public void toggleTraining() {
		this.training = !this.training;
		if(training)
			System.out.println("Training");
		else {
			System.out.println("Done training");
			writeTrainData();
		}
	}
	
	// Write training data to file
	public void writeTrainData() {
		try {
			PrintWriter out = new PrintWriter(new File("trainData.csv"));
			for(int i = 0; i < 88; i++) {
				for(int j = 0; j < 87; j++)
					out.print(trainData[i][j] + ",");
				out.print(trainData[i][87] + "\n");
			}
			out.close();
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void incrementNote() {
		noteLabelCounter++;
		if(noteLabelCounter >= hi)
			noteLabelCounter = lo;
			
	}
	
	public double getLevel() {
		return level;
	}

	public void setLevel(double level) {
		this.level = level;
	}

	public int getBufferSize() {
		return bufferSize;
	}
	
	public double[] getBuffer() {
		return buffer;
	}
	
	public double[] getFrequencySpectrum() {
		return frequencySpectrum;
	}
	
	public int getNoteLabelCounter() {
		return noteLabelCounter;
	}
	
	public double getNoteTrainingProgress() {
		return Math.min((learningRateReciprocals[noteLabelCounter] - 5) / 20., 1);
	}
	
	public int getLo() {
		return lo;
	}
	
	public int getHi() {
		return hi;
	}
	
	public int argmax(double[] arr) {
		double max = Double.MIN_VALUE;
		int argmax = 0;
		for(int i = 0; i < arr.length; i++) {
			if(arr[i] > max) {
				max = arr[i];
				argmax = i;
			}
		}
		return argmax;
	}
}