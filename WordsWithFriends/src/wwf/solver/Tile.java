package wwf.solver;

public class Tile {
	public static final int WWF = 1;
	public static final int SCR = 2;
	
	private static final int[] LETTER_VALUE_WWF = {
        1, // A
		4, // B
		4, // C
		2, // D
		1, // E
		4, // F
		3, // G
		3, // H
		1, // I
		10, // J
		5, // K
		2, // L
		4, // M
		2, // N
		1, // O
		4, // P
		10, // Q
		1, // R
		1, // S
		1, // T
		2, // U
		5, // V
		4, // W
		8, // X
		3, // Y
		10, // Z
		};
	
	private static final int[] LETTER_VALUE_SCR = {
		1, // A
		3, // B
		3, // C
		2, // D
		1, // E
		4, // F
		2, // G
		4, // H
		1, // I
		8, // J
		5, // K
		1, // L
		3, // M
		1, // N
		1, // O
		3, // P
		10, // Q
		1, // R
		1, // S
		1, // T
		1, // U
		4, // V
		4, // W
		8, // X
		4, // Y
		10, // Z
		};
	
	private char letter;
	private int type;
	private boolean isBlank;
	
	public Tile(char letter, int type) {
		this.letter = Character.toUpperCase(letter);
		this.type = type;
	}
	
	public void setBlank() {
		isBlank = true;
	}
	
	public int pointValue() {
		if(isBlank) { return 0; }
		else {
			switch(type) {
			case WWF:
				return LETTER_VALUE_WWF[letter - 'A'];
			case SCR:
				return LETTER_VALUE_SCR[letter - 'A'];
			default:
				return -1;
			}
		}
	}
	
	public char letter() {
		return letter;
	}
	
	public boolean isBlank() {
		return isBlank;
	}
}
