package wwf.solver;

public class Word {
	public String word;
	public String blankmask; // "...B..." (e.g., 4th tile is blank)
	
	public Word(Word word) {
		if(word == null) {
			this.word = "";
			this.blankmask = "";
		}
		else {
			this.word = new String(word.word);
			this.blankmask = new String(word.blankmask);
		}
	}
	
	public void addLetter(char letter, boolean isBlank) {
		word += Character.toUpperCase(letter);
		blankmask += (isBlank ? 'B' : '.');
	}
	
	public int length() {
		return word.length();
	}
	
	public int blankCount() {
		int ct = 0;
		for(int i=0; i<blankmask.length(); i++) {
			if(blankmask.charAt(i) == 'B') ct++;
		}
		return ct;
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof Word)) {
			return false;
		}
		Word w = (Word) o;
		return w.word.equals(this.word) && w.blankmask.equals(this.blankmask);
	}
	
	@Override
	public int hashCode() {
		return word.hashCode() + blankmask.hashCode();
	}
	
	@Override
	public String toString() {
		return word;
	}
}
