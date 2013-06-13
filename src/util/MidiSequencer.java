package util;
import java.io.File;
import java.util.HashMap;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

public class MidiSequencer {
	public static final int NOTE_ON = 0x90;
	public static final int NOTE_OFF = 0x80;
	public static final int MIDI_OFFSET = 21;
	private int[] noteLastPlayed;
	private IntervalTree<String> itree;

	public MidiSequencer() {
		noteLastPlayed = new int[88];
		itree = new IntervalTree<String>();
	}
	
	public void noteOn(int note, int tick) {
		if(noteLastPlayed[note] == -1)
			noteLastPlayed[note] = tick;
	}
	
	public Interval1D noteOff(int note, int tick) {
		int start = noteLastPlayed[note];
		noteLastPlayed[note] = -1;
		return new Interval1D(start, tick);
	}
	
	public void addNoteDuration(int note, Interval1D duration) {
		itree.put(duration, String.valueOf(note));
	}
	
	public IntervalTree<String> getIntervalTree() {
		return itree;
	}
	
	public static void main(String[] args) throws Exception {
		Sequence sequence = MidiSystem.getSequence(new File("fur_elise.mid"));
		MidiSequencer ms = new MidiSequencer();
		
//		int trackNumber = 0;
		for (Track track :  sequence.getTracks()) {
//			trackNumber++;
//			System.out.println("Track " + trackNumber + ": size = " + track.size());
//			System.out.println();
			for (int i=0; i < track.size(); i++) { 
				MidiEvent event = track.get(i);
				System.out.print("@" + event.getTick() + " ");
				int tick = (int) event.getTick();
				MidiMessage message = event.getMessage();
				if (message instanceof ShortMessage) {
					ShortMessage sm = (ShortMessage) message;
//					System.out.print("Channel: " + sm.getChannel() + " ");
					if (sm.getCommand() == NOTE_ON) {
						int note = sm.getData1() - MIDI_OFFSET;
						ms.noteOn(note, tick);
						String noteName = MusicUtil.NOTE_NAMES[note];
						int velocity = sm.getData2();
						System.out.println("Note on, " + noteName + " key=" + note + " velocity: " + velocity);
						
					} else if (sm.getCommand() == NOTE_OFF) {
						int note = sm.getData1() - MIDI_OFFSET;
						Interval1D duration = ms.noteOff(note, tick);
						ms.addNoteDuration(note, duration);
						String noteName = MusicUtil.NOTE_NAMES[note];
						int velocity = sm.getData2();
						System.out.println("Note off, " + noteName + " key=" + note + " velocity: " + velocity);
					} else {
//						System.out.println("Command:" + sm.getCommand());
					}
				} else {
//					System.out.println("Other message: " + message.getClass());
				}
			}
		}
		System.out.println(ms.getIntervalTree().searchAll(new Interval1D(185761, 185761)));
	}
}
