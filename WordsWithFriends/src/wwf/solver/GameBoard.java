package wwf.solver;

import java.util.ArrayList;
import java.util.Collections;

public class GameBoard {
	
	private static final int WWF_BONUS = 35;
	private static final int WWF_BONUS_TILE_CT = 7;
	private static final int SCRABBLE_BONUS = 50;
	private static final int SCRABBLE_BONUS_TILE_CT = 7;
	public static final int WWF = 1;
	public static final String WWF_S = "WWF";
	public static final int SCRABBLE = 2;
	public static final String SCRABBLE_S = "SCRABBLE";
	
	// Letters on board - null represents blank space
	private Tile[][] board;
	private int size;
	
	// Type of game sets these variables
	private int allTilesPlacedBonus;
	private int maxTiles;
	private int boardType;
	
	// Mask of word multipliers on board
	public enum Mul {
		DL, // Double letter
		TL, // Triple letter
		DW, // Double word
		TW, // Triple word
		XX  // No multiplier
	}
	private Mul[][] boardMult;
	
	public enum Dir {
		DOWN,
		RIGHT
	}
	
	public GameBoard(int size, int gameType) {
		board = new Tile[size][size];
		this.size = size;
		setGameType(gameType);
	}
	
	public void dispBoard() {
		System.out.print("+");
		for(int i=0; i<size*2+1; i++) {
			System.out.print("-");
		}
		System.out.println("+");
		for(int i=0; i<size; i++) {
			System.out.print("| ");
			for(int j=0; j<size; j++) {
				if(board[i][j] != null) {
					System.out.print(board[i][j].letter() + " ");
				}
				else {
					System.out.print("  ");
				}
			}
			System.out.println("|");
		}
		System.out.print("+");
		for(int i=0; i<size*2+1; i++) {
			System.out.print("-");
		}
		System.out.println("+");
	}
	
	/**
	 * Given a Word w, finds all TilePlacements that could have been done to
	 * place that word on this game board. 
	 */
	public ArrayList<TilePlacement> findLocationsForWord(Word w, Dictionary dict) {
		ArrayList<TilePlacement> result = new ArrayList<TilePlacement>();
		if(isEmpty()) {
			int center = size/2;
			int startMin = (center + 1 - w.length() >= 0 ? center + 1 - w.length() : 0);
			int startMax = center;
			
			// Iterate over every square in range of first move locations
			for(int i=startMin; i <= startMax; i++) {
				// Now check if word fits on the board in the "first move" sense
				// RIGHT
				int pointVal = wordFitsFirst(w.word, center, i, Dir.RIGHT, w.blankmask);
				if(pointVal >= 0) {
					// It works!
					result.add(new TilePlacement(w, pointVal, center, i, Dir.RIGHT));
				}
				// DOWN
				pointVal = wordFitsFirst(w.word, i, center, Dir.DOWN, w.blankmask);
				if(pointVal >= 0) {
					// It works!
					result.add(new TilePlacement(w, pointVal, i, center, Dir.DOWN));
				}
			}
		}
		else {
			// Iterate over every square as possible starting location
			for(int i=0; i<size; i++) { // row
				for(int j=0; j<size; j++) { // col
					// RIGHT
					int pointVal = wordFits(w.word, i, j, Dir.RIGHT, w.blankmask, dict);
					if(pointVal >= 0) {
						// It works!
						result.add(new TilePlacement(w, pointVal, i, j, Dir.RIGHT));
					}
					// DOWN
					pointVal = wordFits(w.word, i, j, Dir.DOWN, w.blankmask, dict);
					if(pointVal >= 0) {
						// It works!
						result.add(new TilePlacement(w, pointVal, i, j, Dir.DOWN));
					}
				}
			}
		}
		// Sort by pointvalue
		Collections.sort(result);
		return result;
	}
	
	
	/**
	 * Adds a word to the board if it will fit. Assumes that it can fit with
	 * already existing board letters. Overwriting current board tiles not allowed.
	 * Word starts at (r,c) on board and goes in direction d. If it contains any blank
	 * tiles, it is specified by "...B..." (e.g. the 4th tile is blank)
	 */
	public boolean addWord(String word, Dir d, int r, int c, String blanks) {
		word = word.trim().toUpperCase();
		if (d == Dir.DOWN && r > size - word.length() ||
			d == Dir.RIGHT && c > size - word.length()) {
			return false;
		}
		// Will fit on board. Assume it can fit with already existing board letters
		if (d == Dir.DOWN) {
			for(int i=0; i<word.length(); i++) {
				if(board[r+i][c] == null) {
					board[r+i][c] = new Tile(word.charAt(i), boardType);
					if(blanks.charAt(i) == 'B') {
						board[r+i][c].setBlank();
					}
				}
			}
		}
		else if (d == Dir.RIGHT) {
			for(int i=0; i<word.length(); i++) {
				if(board[r][c+i] == null) {
					board[r][c+i] = new Tile(word.charAt(i), boardType);
					if(blanks.charAt(i) == 'B') {
						board[r][c+i].setBlank();
					}
				}
			}
		}
		return true;
	}
	
	/**
	 * Sees if the word can be placed on the board starting at (r,c), going in
	 * the direction specified by d. If so, returns the point value of the placement.
	 * If not, returns -1.
	 * Uses the Dictionary dict to check for valid words that may be also created in
	 * the tile placement. Assumes that word itself is a valid word.
	 */
	public int wordFits(String word, int r, int c, Dir d, String blanks, Dictionary dict) {
		int totalPoints = 0;
		int mainWordPoints = 0;
		int mainWordMult = 1;
		int newTilesPlaced = 0;
		
		// Must be within board bounds
		if (d == Dir.DOWN && r > size - word.length() ||
				d == Dir.RIGHT && c > size - word.length()) {
				return -1;
		}
		// Check that it's adjacent to at least one tile already on the board
		// The below checks that no tile is beside the word's end points.
		
		if(d == Dir.DOWN) {
			if((r > 0 && board[r-1][c] != null) ||
			   (r+word.length() < size && board[r+word.length()][c] != null)) {
				return -1;
			}
		}
		else if(d == Dir.RIGHT) {
			if((c > 0 && board[r][c-1] != null) ||
			   (c+word.length() < size && board[r][c+word.length()] != null)) {
				return -1;
			}
		}
		
		boolean isConnected = false;
		
		// Check tiles adjacent to tiles which are part of the word but not already
		//     on the board - these must create valid words.
		
		for(int i=0; i<word.length(); i++) {
			Tile mainTile;
			Mul mainMult;
			if(d == Dir.DOWN) {
				mainTile = board[r+i][c];
				mainMult = boardMult[r+i][c];
			}
			else { // RIGHT
				mainTile = board[r][c+i];
				mainMult = boardMult[r][c+i];
			}
			// This letter is one of the tiles already on board
			if(mainTile != null) {
				isConnected = true;
				mainWordPoints += mainTile.pointValue();
				// Check that this tile is actually the letter we're on
				if(mainTile.letter() != word.charAt(i)) {
					return -1;
				}
			}
			// This letter is a tile we'd be placing
			else {
				newTilesPlaced++;
				mainTile = new Tile(word.charAt(i), boardType);
				if(blanks.charAt(i) == 'B') {
					mainTile.setBlank();
				}
				switch(mainMult) {
				case DL:
					mainWordPoints += mainTile.pointValue()*2;
					break;
				case TL:
					mainWordPoints += mainTile.pointValue()*3;
					break;
				case DW:
					mainWordMult *= 2;
					mainWordPoints += mainTile.pointValue();
					break;
				case TW:
					mainWordMult *= 3;
					mainWordPoints += mainTile.pointValue();
					break;
				case XX:
					mainWordPoints += mainTile.pointValue();
					break;
				}
				int exWordStart, exWordEnd;
				if(d == Dir.DOWN) {
					exWordStart = c; exWordEnd = c;
					// Does this spot have any string of tiles to the left?
					while(exWordStart > 0 && board[r+i][exWordStart-1] != null) {
						exWordStart--;
					}
					// Does this spot have any string of tiles to the right?
					while(exWordEnd+1 < size && board[r+i][exWordEnd+1] != null) {
						exWordEnd++;
					}
				}
				else { // RIGHT
					exWordStart = r; exWordEnd = r;
					// Does this spot have any string of tiles above?
					while(exWordStart > 0 && board[exWordStart-1][c+i] != null) {
						exWordStart--;
					}
					// Does this spot have any string of tiles below?
					while(exWordEnd+1 < size && board[exWordEnd+1][c+i] != null) {
						exWordEnd++;
					}
				}
				if(exWordEnd - exWordStart > 0) {
					isConnected = true;
					// There is a word! Check if it is valid
					String exWord = "";
					int exWordPts = 0;
					int exWordMult = 1;
					for(int j=0; j<=exWordEnd-exWordStart; j++) {
						Tile tile;
						Mul mult;
						if(d == Dir.DOWN) {
							if(exWordStart + j == c) {
								tile = mainTile;
								mult = mainMult;
								switch(mult) {
								case DL:
									exWordPts += tile.pointValue()*2;
									break;
								case TL:
									exWordPts += tile.pointValue()*3;
									break;
								case DW:
									exWordMult *= 2;
									exWordPts += tile.pointValue();
									break;
								case TW:
									exWordMult *= 3;
									exWordPts += tile.pointValue();
									break;
								case XX:
									exWordPts += tile.pointValue();
									break;
								}
							}
							else {
								tile = board[r+i][exWordStart+j];
								mult = boardMult[r+i][exWordStart+j];
								exWordPts += tile.pointValue();
							}
						}
						else { // RIGHT
							if(exWordStart + j == r) {
								tile = mainTile;
								mult = mainMult;
								switch(mult) {
								case DL:
									exWordPts += tile.pointValue()*2;
									break;
								case TL:
									exWordPts += tile.pointValue()*3;
									break;
								case DW:
									exWordMult *= 2;
									exWordPts += tile.pointValue();
									break;
								case TW:
									exWordMult *= 3;
									exWordPts += tile.pointValue();
									break;
								case XX:
									exWordPts += tile.pointValue();
									break;
								}
							}
							else {
								tile = board[exWordStart+j][c+i];
								mult = boardMult[exWordStart+j][c+i];
								exWordPts += tile.pointValue();
							}
						}
						
						exWord += tile.letter();
					}
					exWordPts *= exWordMult;
					if(!dict.isWord(exWord)) {
						return -1;
					}
					else {
						totalPoints += exWordPts;
					}
					
				}
			}
		}
		if(!isConnected) {
			return -1;
		}
		mainWordPoints *= mainWordMult;
		totalPoints += mainWordPoints;
		
		// Scrabble
		if(newTilesPlaced == maxTiles) {
			totalPoints += allTilesPlacedBonus;
		}
		
		return totalPoints;
	}
	
	/**
	 * Same operation as wordFits, but assuming an empty board, and will
	 * only be deemed "fitting" if it covers the center square.
	 */
	public int wordFitsFirst(String word, int r, int c, Dir d, String blanks) {
		int center = size/2;
		boolean centerCovered = false;
		int totalPoints = 0;
		int totalMult = 1;
		for(int i=0; i<word.length(); i++) {
			Tile t = new Tile(word.charAt(i), boardType);
			if(blanks.charAt(i) == 'B') {
				t.setBlank();
			}
			Mul squareMult;
			if(d == Dir.RIGHT) {
				squareMult = boardMult[r][c+i];
				if(r >= size || c+i >= size || r < 0 || c+i < 0) {
					return -1;
				}
				if(r == center && c+i == center) {
					centerCovered = true;
				}
			}
			else { // DOWN
				squareMult = boardMult[r+i][c];
				if(r+i >= size || c >= size || r+i < 0 || c < 0) {
					return -1;
				}
				if(r+i == center && c == center) {
					centerCovered = true;
				}
			}
			switch(squareMult) {
			case DL:
				totalPoints += t.pointValue()*2;
				break;
			case TL:
				totalPoints += t.pointValue()*3;
				break;
			case DW:
				totalMult *= 2;
				totalPoints += t.pointValue();
				break;
			case TW:
				totalMult *= 3;
				totalPoints += t.pointValue();
				break;
			case XX:
				totalPoints += t.pointValue();
				break;
			}
		}
		if (!centerCovered) {
			return -1;
		}
		totalPoints *= totalMult;
		return totalPoints;
	}

	public int size() {
		return size;
	}
	
	public Tile getTile(int r, int c) {
		if(r >= size || c >= size || r < 0 || c < 0) {
			return null;
		}
		return board[r][c];
	}
	
	public void setSquare(int r, int c, char t, boolean isBlank) {
		board[r][c] = new Tile(t, boardType);
		if(isBlank) board[r][c].setBlank();
	}
	
	public void clearSquare(int r, int c) {
		board[r][c] = null;
	}
	
	public static Mul[][] buildBoardMultWWF(int size) {
		Mul[][] b = new Mul[size][size];
		// Enumerate spots on 1/8 of the board (choose upper left upper)
		// and assume symmetry
		ArrayList<MulLoc> ms = new ArrayList<MulLoc>();
		ms.add(new MulLoc(3, 0, Mul.TW));
		ms.add(new MulLoc(2, 1, Mul.DL));
		ms.add(new MulLoc(3, 3, Mul.TL));
		ms.add(new MulLoc(4, 2, Mul.DL));
		ms.add(new MulLoc(5, 1, Mul.DW));
		ms.add(new MulLoc(5, 5, Mul.TL));
		ms.add(new MulLoc(6, 0, Mul.TL));
		ms.add(new MulLoc(6, 4, Mul.DL));
		ms.add(new MulLoc(3, 0, Mul.TW));
		ms.add(new MulLoc(7, 3, Mul.DW));
		// Fill board
		for(int i=0; i<size; i++) {
			for(int j=0; j<size; j++) {
				b[i][j] = Mul.XX;
			}
		}
		for(MulLoc m : ms) {
			b[m.r][m.c] = m.m;
			b[m.c][m.r] = m.m;
			b[size-m.r-1][m.c] = m.m;
			b[m.c][size-m.r-1] = m.m;
			b[m.r][size-m.c-1] = m.m;
			b[size-m.c-1][m.r] = m.m;
			b[size-m.r-1][size-m.c-1] = m.m;
			b[size-m.c-1][size-m.r-1] = m.m;
		}
		return b;
	}
	
	public static Mul[][] buildBoardMultScrabble(int size) {
		Mul[][] b = new Mul[size][size];
		// Enumerate spots on 1/8 of the board (choose upper left upper)
		// and assume symmetry
		ArrayList<MulLoc> ms = new ArrayList<MulLoc>();
		ms.add(new MulLoc(0, 0, Mul.TW));
		ms.add(new MulLoc(1, 1, Mul.DW));
		ms.add(new MulLoc(2, 2, Mul.DW));
		ms.add(new MulLoc(3, 3, Mul.DW));
		ms.add(new MulLoc(4, 4, Mul.DW));
		ms.add(new MulLoc(5, 5, Mul.TL));
		ms.add(new MulLoc(6, 6, Mul.DL));
		ms.add(new MulLoc(7, 7, Mul.DW));
		ms.add(new MulLoc(0, 3, Mul.DL));
		ms.add(new MulLoc(1, 5, Mul.TL));
		ms.add(new MulLoc(2, 6, Mul.DL));
		ms.add(new MulLoc(0, 7, Mul.TW));
		ms.add(new MulLoc(3, 7, Mul.DL));
		// Fill board
		for(int i=0; i<size; i++) {
			for(int j=0; j<size; j++) {
				b[i][j] = Mul.XX;
			}
		}
		for(MulLoc m : ms) {
			b[m.r][m.c] = m.m;
			b[m.c][m.r] = m.m;
			b[size-m.r-1][m.c] = m.m;
			b[m.c][size-m.r-1] = m.m;
			b[m.r][size-m.c-1] = m.m;
			b[size-m.c-1][m.r] = m.m;
			b[size-m.r-1][size-m.c-1] = m.m;
			b[size-m.c-1][size-m.r-1] = m.m;
		}
		return b;
	}
	
	public Mul getMul(int r, int c) {
		return boardMult[r][c];
	}
	
	public boolean isEmpty() {
		for(int i=0; i<size; i++) {
			for(int j=0; j<size; j++) {
				if(board[i][j] != null) {
					return false;
				}
			}
		}
		return true;
	}
	
	public void setGameType(int type) {
		switch(type) {
		case WWF:
			boardMult = buildBoardMultWWF(size);
			allTilesPlacedBonus = WWF_BONUS;
			maxTiles = WWF_BONUS_TILE_CT;
			boardType = WWF;
			break;
		case SCRABBLE:
			boardMult = buildBoardMultScrabble(size);
			allTilesPlacedBonus = SCRABBLE_BONUS;
			maxTiles = SCRABBLE_BONUS_TILE_CT;
			boardType = SCRABBLE;
			break;
		}
	}
	
	public int getGameType() {
		return boardType;
	}
	
	// return null if cannot convert this int.
	public static String convertBoardTypeToString(int bt) {
		switch(bt) {
		case WWF:
			return WWF_S;
		case SCRABBLE:
			return SCRABBLE_S;
		default:
			return null;
		}
	}
	
	// return -1 if cannot convert this string.
	public static int convertBoardTypeToInt(String bt) {
		if(SCRABBLE_S.equals(bt)) {
			return SCRABBLE;
		}
		else if(WWF_S.equals(bt)) {
			return WWF;
		}
		return -1;
	}

}
