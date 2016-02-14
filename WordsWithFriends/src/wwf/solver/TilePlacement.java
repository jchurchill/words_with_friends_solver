package wwf.solver;
import wwf.solver.GameBoard.Dir;

public class TilePlacement implements Comparable<TilePlacement>{
	public Word word; // Main word
	public int points; // Value of this move
	public int r; // Row on board of first letter of the main word for this tile placement
	public int c; // Col on board of first letter of the main word for this tile placement
	public Dir dir; // Direction of the main word (DOWN or RIGHT)
	
	public TilePlacement(Word word, int points, int r, int c, Dir dir) {
		this.word = word;
		this.points = points;
		this.r = r;
		this.c = c;
		this.dir = dir;
	}
	
	@Override
	public int compareTo(TilePlacement t) {
		int ptComp = t.points - points;
		int blankComp = t.word.blankCount() - word.blankCount();
		int lenComp = t.word.length() - word.length();
		if(ptComp == 0) {
			if(blankComp == 0) {
				return lenComp;
			}
			return blankComp;
		}
		return ptComp;
	}
	
	@Override
	public String toString() {
		return word.toString() + " - " + points + " pts";
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof TilePlacement)) {
			return false;
		}
		TilePlacement t = (TilePlacement) o;
		return t.r == r && t.c == c && t.points == points && t.word.equals(word) && t.dir == dir;
	}
}
