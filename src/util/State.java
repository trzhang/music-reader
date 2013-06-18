package util;

import java.util.Arrays;

import sound.Recorder;


public class State {

	private long start;
	private long end;
	private int[] notesPlayed;
	
	public State(long start, long end, int[] notes) {
		this.start = start;
		this.end = end;
		this.notesPlayed = parseNotesPlayed(notes);
	}
	
	public long getStart() {
		return start;
	}
	
	public long getEnd() {
		return end;
	}
	
	public long getLength() {
		return end - start;
	}
	
	public double getTransitionProb() {
		// ticks / second / (ticks * samples / second) = 1 / samples
		return MidiSequencer.ticksPerSecond / (getLength() * Recorder.BUFFER_SAMPLE_RATE);
	}
	
	public int[] parseNotesPlayed(int[] notes) {
		int size = 0;
		for(int n : notes)
			if(n > 0)
				size += 1;
		int[] ret = new int[size];
		if(ret.length > 0) {
			size = 0;
			for(int i = 0; i < 88; i++)
				if(notes[i] > 0)
					ret[size++] = i;
		}
		return ret;
	}
	
	public int[] getNotesPlayed() {
		return notesPlayed;
	}
	
	public String toString() {
		String[] noteNames = new String[notesPlayed.length];
		for(int i = 0; i < notesPlayed.length; i++)
			noteNames[i] = MusicUtil.NOTE_NAMES[notesPlayed[i]];
		return "ticks: " + start + "-" + end + ", length: " + getLength() + ", transition prob: " + getTransitionProb() + ", notes: " + Arrays.toString(noteNames);
	}
}
