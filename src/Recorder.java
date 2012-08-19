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
	
	public Recorder(){
		line = null;
		format = new AudioFormat(16000, 16, 1, true, true);
		info = new DataLine.Info(TargetDataLine.class, format);
		recording = false;
		paused = false;
		level = 0;
		bufferSize = -1;
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
		if(bufferSize == -1)
			bufferSize = Math.min(400, 8 * line.getBufferSize() / (format.getSampleSizeInBits() * format.getChannels()));
		while(!recording);
		startRecording();
	}

	public void startRecording(){
		System.out.println("Recording...");
		line.start();
		out = new ByteArrayOutputStream();
		int numBytesRead;
		byte[] data = new byte[bufferSize * format.getSampleSizeInBits() * format.getChannels() / 8]; //line.getBufferSize() / 20;
		double[] normalizedData = new double[bufferSize]; //data.length / (format.getSampleSizeInBits() / 8 * format.getChannels())

		// Begin audio capture.
		line.start();

		int i = 0;
		
		// Here, stopped is a global boolean set by another thread.
		while(recording){
			// Read the next chunk of data from the TargetDataLine.
			numBytesRead = line.read(data, 0, data.length);
			normalizeData(data, normalizedData);
			setLevel(rms(normalizedData));
			// Save this chunk of data.
			out.write(data, 0, numBytesRead);		 
			i++;
			while(paused);
		}
		processRecording(data.length / 2 * i);
	}
	
	public void normalizeData(byte[] data, double[] normalizedData){
		double max = 0.75 * Math.pow(2, format.getSampleSizeInBits() - 1);
		if(format.getSampleSizeInBits() == 8){
			if(format.getChannels() == 1)
				for(int i = 0; i < normalizedData.length; i++){
					normalizedData[i] = data[i] / max;
					if(Math.abs(normalizedData[i]) > 1)
						normalizedData[i] = Math.signum(normalizedData[i]);
				}
			else if(format.getChannels() == 2)
				for(int i = 0; i < normalizedData.length; i++){
					normalizedData[i] = (data[2 * i] + data[2 * i + 1]) / (2 * max);
					if(Math.abs(normalizedData[i]) > 1)
						normalizedData[i] = Math.signum(normalizedData[i]);
				}
		} else if(format.getSampleSizeInBits() == 16){
			if(format.getChannels() == 1)
				for(int i = 0; i < normalizedData.length; i++){
					normalizedData[i] = (256 * data[2 * i] + data[2 * i + 1]) / max;
					if(Math.abs(normalizedData[i]) > 1)
						normalizedData[i] = Math.signum(normalizedData[i]);
				}
			else if(format.getChannels() == 2)
				for(int i = 0; i < normalizedData.length; i++){
					normalizedData[i] = (256 * data[4 * i] + data[4 * i + 1] + 256 * data[4 * i + 2] + data[4 * i + 3]) / (2 * max);
					if(Math.abs(normalizedData[i]) > 1)
						normalizedData[i] = Math.signum(normalizedData[i]);
				}
		}
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

	public void setLine(TargetDataLine line) {
		this.line = line;
	}

	public AudioFormat getFormat() {
		return format;
	}

	public void setFormat(AudioFormat format) {
		this.format = format;
	}

	public DataLine.Info getInfo() {
		return info;
	}

	public void setInfo(DataLine.Info info) {
		this.info = info;
	}

	public ByteArrayOutputStream getOut() {
		return out;
	}

	public void setOut(ByteArrayOutputStream out) {
		this.out = out;
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

	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}
}