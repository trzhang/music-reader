import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;


public class Recorder extends Thread{
	
	private TargetDataLine line;
	private AudioFormat format;
	private DataLine.Info info;
	private ByteArrayOutputStream out;
	private boolean recording;
	private boolean paused;
	private double level;
	private int bufferSize;
	private double[] buffer;
	private double[] frequencySpectrum;
	private double[] noiseThresholds;
	private static double[] frequencies = {27.5, 29.1352, 30.8677, 32.7032, 34.6478, 36.7081, 38.8909, 41.2034, 43.6535, 46.2493, 48.9994, 51.9131, 55, 58.2705, 61.7354, 65.4064, 69.2957, 73.4162, 77.7817, 82.4069, 87.3071, 92.4986, 97.9989, 103.826, 110, 116.541, 123.471, 130.813, 138.591, 146.832, 155.563, 164.814, 174.614, 184.997, 195.998, 207.652, 220, 233.082, 246.942, 261.626, 277.183, 293.665, 311.127, 329.628, 349.228, 369.994, 391.995, 415.305, 440, 466.164, 493.883, 523.251, 554.365, 587.33, 622.254, 659.255, 698.456, 739.989, 783.991, 830.609, 880, 932.328, 987.767, 1046.5, 1108.73, 1174.66, 1244.51, 1318.51, 1396.91, 1479.98, 1567.98, 1661.22, 1760, 1864.66, 1975.53, 2093, 2217.46, 2349.32, 2489.02, 2637.02, 2793.83, 2959.96, 3135.96, 3322.44, 3520, 3729.31, 3951.07, 4186.01};
	
	public Recorder(){
		line = null;
		format = new AudioFormat(16000, 16, 1, true, true);
		info = new DataLine.Info(TargetDataLine.class, format);
		recording = false;
		paused = false;
		level = 0;
		bufferSize = -1;
		frequencySpectrum = new double[88];
		noiseThresholds = new double[88];
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
			bufferSize = Math.min(400, 8 * line.getBufferSize() / (format.getSampleSizeInBits() * format.getChannels()));
			buffer = new double[bufferSize];
		}
		calibrate();
		while(!recording);
		startRecording();
	}
	
	public void calibrate(){
		double[] buffer = new double[bufferSize];
		System.out.println("Calibrating...");
		byte[] data = new byte[bufferSize * format.getSampleSizeInBits() * format.getChannels() / 8]; //line.getBufferSize() / 20;
		line.start();
		for(int count = 0; count < 1000; count++){
			line.read(data, 0, data.length);
			normalizeData(data, buffer);
			for(int i = 0; i < 88; i++)
				noiseThresholds[i] += frequencyComponent(buffer, frequencies[i]);
		}
		for(int i = 0; i < 88; i++)
			noiseThresholds[i] /= 1000.;
		System.out.println("Done");
		System.out.println(Arrays.toString(noiseThresholds));
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
			normalizeData(data, buffer);
			setLevel(rms(buffer));
			for(int i = 0; i < 88; i++)
				frequencySpectrum[i] = Math.max(frequencyComponent(buffer, frequencies[i]) - noiseThresholds[i], 0);
			System.out.println(Arrays.toString(buffer));
			System.out.println(Arrays.toString(frequencySpectrum));
			// Save this chunk of data.
			out.write(data, 0, numBytesRead);	 
			count++;
			while(paused);
		}
		processRecording(count * bufferSize);
	}
	
	public void normalizeData(byte[] data, double[] buffer){
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
	
	public double frequencyComponent(double[] buffer, double frequency) {
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
}