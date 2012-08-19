import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

public class MusicReader extends JFrame {
	
	static MusicReader musicReader;
	static Recorder recorder;
	static int width = 800, height = 600;
	
	public static void main(String[] args) {
		recorder = new Recorder();
		musicReader = new MusicReader();
		recorder.start();
	}
	
	public MusicReader(){
		this.setSize(width, height);
		this.setPreferredSize(new Dimension(width, height));
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		JPanel panel = new JPanel(new BorderLayout());
		panel.setPreferredSize(new Dimension(width, height));
		
		JPanel controlPanel = new JPanel(new BorderLayout());
		controlPanel.setPreferredSize(new Dimension(width, 50));
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setPreferredSize(new Dimension(300, 50));
		
		final ImageIcon startIcon = new ImageIcon("images/play.png");
		final ImageIcon recordIcon = new ImageIcon("images/record.png");
		final JButton start = new JButton(startIcon);
		start.setFocusPainted(false);
		start.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				if(!recorder.isRecording())
					recorder.setRecording(true);
				if(recorder.isPaused())
					recorder.pauseRecording();
				start.setIcon(recordIcon);
				recorder.setRecording(true);
			}
		});
		start.setVisible(true);
		ImageIcon pauseIcon = new ImageIcon("images/pause.png");
		JButton pause = new JButton(pauseIcon);
		pause.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				if(recorder.isRecording())
					start.setIcon(startIcon);
				else
					start.setIcon(recordIcon);
				recorder.pauseRecording();
			}
		});
		pause.setVisible(true);
		ImageIcon stopIcon = new ImageIcon("images/stop.png");
		JButton stop = new JButton(stopIcon);
		stop.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				start.setIcon(startIcon);
				recorder.setRecording(false);
				recorder.setPaused(false);
			}
		});
		stop.setVisible(true);
		buttonPanel.add(start);
		buttonPanel.add(pause);
		buttonPanel.add(stop);
		
		JPanel infoPanel = new JPanel(new GridLayout(2, 0));
		infoPanel.setPreferredSize(new Dimension(width - 300, height));
		
		JLabel encoding = new JLabel("Encoding: " + recorder.getFormat().getEncoding(), JLabel.CENTER);
		JLabel samplesPerSec = new JLabel("Samples/sec: " + recorder.getFormat().getSampleRate(), JLabel.CENTER);
		JLabel bitsPerSample = new JLabel("Bits/sample: " + recorder.getFormat().getSampleSizeInBits(), JLabel.CENTER);
		JLabel numChannels = new JLabel("", JLabel.CENTER);

		if(recorder.getFormat().getChannels() == 1)
			numChannels.setText("Channels: 1 (mono)");
		else
			numChannels.setText("Channels: 2 (stereo)");
		
		infoPanel.add(samplesPerSec);
		infoPanel.add(encoding);
		infoPanel.add(bitsPerSample);
		infoPanel.add(numChannels);
		infoPanel.setVisible(true);
		
		controlPanel.add(buttonPanel, BorderLayout.WEST);
		controlPanel.add(infoPanel, BorderLayout.EAST);
		controlPanel.setVisible(true);
		
		JPanel graphPanel = new JPanel(new BorderLayout());
		graphPanel.setPreferredSize(new Dimension(width, height - 50));
		
		VolumeMeter volumeMeter = new VolumeMeter();
		volumeMeter.setPreferredSize(new Dimension(20, 200));
		volumeMeter.setVisible(true);
		
		SoundWaveGraph soundWaveGraph = new SoundWaveGraph();
		soundWaveGraph.setPreferredSize(new Dimension(width - 20, height));
		soundWaveGraph.setVisible(true);
		
		graphPanel.add(volumeMeter, BorderLayout.WEST);
		graphPanel.add(soundWaveGraph, BorderLayout.EAST);
		graphPanel.setVisible(true);
		
		panel.add(controlPanel, BorderLayout.NORTH);
		panel.add(graphPanel, BorderLayout.SOUTH);
		panel.setVisible(true);
		
		this.add(panel);
		this.pack();
		this.setVisible(true);
	}
	
	private class VolumeMeter extends JPanel{
		
		private double level;
		
		public VolumeMeter(){
			Timer timer = new Timer(100, new ActionListener(){
				public void actionPerformed(ActionEvent e){
					level = recorder.getLevel();
					//System.out.println(level);
					repaint();
				}
			});
			timer.start();
		}
		
		public void paintComponent(Graphics g){
			super.paintComponent(g);
			g.setColor(Color.green);
			int meterHeight = (int) (level * getHeight());
			g.fillRect(getX(), getY() + getHeight() - meterHeight, getWidth(), meterHeight);
		}
	}
	
	private class SoundWaveGraph extends JPanel{
		
		Queue<Double> data = new LinkedList<Double>();
		int updatesPerSec = (int) recorder.getFormat().getSampleRate() / recorder.getBufferSize();
		int repaintsPerSec = 2;
		int maxLength = 100;
		
		public SoundWaveGraph(){
			this.data = new LinkedList<Double>();
		}
		
		public void addData(double[] normalizedData){
			int overflow = data.size() + normalizedData.length - maxLength;
			for(double d : normalizedData){
				data.add(d);
				data.poll();
			}
		}
		
		public void paintComponent(Graphics g){
			super.paintComponent(g);
			g.setColor(Color.black);
			int startX = this.getX(), y0 = this.getY() + this.getHeight() / 2, width = this.getWidth() / data.size();
			Iterator<Double> itr = data.iterator();
			for(int i = 0; i < data.size(); i++){
				g.fillOval(startX + i * width, (int) (y0 - itr.next() * this.getHeight() / 2), 2, 2);			}
		}
	}
}