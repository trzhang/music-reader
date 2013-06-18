package util;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

public class MidiSequencer {
	
	public static final int NOTE_ON = 0x90;
	public static final int NOTE_OFF = 0x80;
	
	public static double ticksPerSecond;
	
	private Sequence sequence;
	private Track[] tracks;
	public int[] trackPositions;
	private long lastTick;
	private int[] notes;

	public MidiSequencer(String filename) {
		try {
			sequence = MidiSystem.getSequence(new File(filename));
		} catch (InvalidMidiDataException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		tracks = sequence.getTracks();
		trackPositions = new int[tracks.length];
		lastTick = 0;
		notes = new int[88];
		ticksPerSecond = 1000000. * sequence.getTickLength() / sequence.getMicrosecondLength();
	}
	
	public State advanceToNextState() {
		long[] possibleNextTicks = new long[tracks.length];
		Arrays.fill(possibleNextTicks, Long.MAX_VALUE);
		for(int i = 0; i < tracks.length; i++) {
			for(int pos = trackPositions[i] + 1; pos < tracks[i].size(); pos++) {
				MidiEvent event = tracks[i].get(pos);
//				MidiMessage message = event.getMessage();
//				if(message instanceof ShortMessage) {
				if(event.getTick() > lastTick) {
					possibleNextTicks[i] = event.getTick();
					break;
				}
//				}
			}
		}
		long nextTick = MathUtil.min(possibleNextTicks);
		if(nextTick == Long.MAX_VALUE) // no more events
			return null;
		for(int i = 0; i < tracks.length; i++) {
			int pos;
			for(pos = trackPositions[i] + 1; pos < tracks[i].size(); pos++) {
				MidiEvent event = tracks[i].get(pos);
				if(event.getTick() >= nextTick)
					break; // returns with tick at pos = nextTick
				MidiMessage message = event.getMessage();
				if(message instanceof ShortMessage) {
					ShortMessage sm = (ShortMessage) message;
					if(sm.getCommand() == NOTE_ON) {
						int note = MusicUtil.getNote(sm);
						notes[note] += 1;
					} else if(sm.getCommand() == NOTE_OFF) {
						int note = MusicUtil.getNote(sm);
						notes[note] -= 1;
					}
				}
			}
			trackPositions[i] = pos - 1;
		}
		State state = new State(lastTick, nextTick, notes);
		lastTick = nextTick;
		return state;
	}
	
	public LinkedList<State> sequence() {
		LinkedList<State> states = new LinkedList<State>();
		State s;
		while((s = advanceToNextState()) != null) {
			states.add(s);
			System.out.println(s);
		}
		return states;
	}
	
	public LinkedList<State> sequence(int n) {
		LinkedList<State> states = new LinkedList<State>();
		for(int i = 0; i < n; i++) {
			State s = advanceToNextState();
			if(s == null)
				break;
			states.add(s);
			System.out.println(s);
		}
		return states;
	}
}
