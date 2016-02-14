package wwf.solver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import wwf.solver.GameBoard.Dir;

public class Dictionary {
	private DictionaryNode root = new DictionaryNode();
	private int wordCount = 0;
	
	public int addWord(String word) throws IllegalArgumentException {
		DictionaryNode dn = root;
		for(int i=0; i<word.length(); i++) {
			// Throw out characters which aren't letters, raise an alert
			if(!Character.isLetter(word.charAt(i))) {
				System.out.println("Warning: \"" + word + "\" added to dictionary with non-letter character \"" + word.charAt(i) + "\" removed.");
			}
			else {
				if(i == word.length()-1) {
					try {
						dn.addLastLetter(word.charAt(i));
					}
					catch (Exception e) {
						System.out.println("Word \"" + word + "\" attemped to be added to dictionary twice, was rejected.");
					}
				}
				else {
					dn = dn.addLetter(word.charAt(i));
				}
			}
		}
		return wordCount++;
	}
	
	public static Dictionary makeDictionary(Dictionary dict, String dictfilename) throws IllegalArgumentException, IOException {
		long timetaken = System.currentTimeMillis();
		System.out.println("Filling dictionary with " + dictfilename + "...");
		String word;
		InputStream is = dict.getClass().getResourceAsStream(dictfilename);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		while((word = br.readLine()) != null) {
			dict.addWord(word);
		}
		timetaken = System.currentTimeMillis() - timetaken;
		System.out.println(String.format("Dictionary filled. Time: %.3f s", (double)timetaken/1000));
		return dict;
	}
	
	public boolean isWord(String s) {
		String t = s.toUpperCase();
		DictionaryNode dn = root;
		for(int i = 0; i<t.length(); i++) {
			dn = dn.getNextLetter(t.charAt(i));
			if(dn == null) {
				return false;
			}
		}
		return dn.isEndOfWord();
	}
	
	public ArrayList<TilePlacement> findMoves(GameBoard g, String letterTray) {
		// List of moves which are valid
		ArrayList<TilePlacement> moves = new ArrayList<TilePlacement>();
		// Iterate over every square as a possible starting location
		// RIGHTWARD DIRECTION CHECKS
		for(int i=0; i<g.size(); i++) { // row
			for(int j=0; j<g.size(); j++) { // col
				// Only if not at rightmost square
				// Only if there is no tile to the left of this starting location
				if(j<g.size()-1 && g.getTile(i, j-1) == null) {
					// Build up a restrictions string to find words that may fit here
					int col = j;
					int emptiesLeft = letterTray.length();
					String restrictions = "";
					String blanks = "";
					while(col < g.size() && emptiesLeft >= 0) {
						Tile t = g.getTile(i, col);
						if(t!=null) {
							restrictions += t.letter();
							blanks += (t.isBlank() ? 'B' : '.');
						}
						else {
							if(emptiesLeft > 0) {
								restrictions += '.';
								blanks += '.';
							}
							emptiesLeft--;
						}
						col++;
					}
					// The words that may fit here
					ArrayList<Word> fittingWords = findWordsRestricted(letterTray, restrictions, blanks);
					// Now check if each one fits with any other words on the board it may have created
					for(Word w : fittingWords) {
						int pointVal = g.wordFits(w.word, i, j, Dir.RIGHT, w.blankmask, this);
						if(pointVal >= 0) {
							// It works!
							moves.add(new TilePlacement(w, pointVal, i, j, Dir.RIGHT));
						}
					}
				}
			}
		}
		// DOWNWARD DIRECTION CHECKS
		for(int i=0; i<g.size(); i++) { // row
			for(int j=0; j<g.size(); j++) { // col
				// Only if not at bottom-most square
				// Only if there is no tile above this starting location
				if(i<g.size()-1 && g.getTile(i-1, j) == null) {
					// Build up a restrictions string to find words that may fit here
					int row = i;
					int emptiesLeft = letterTray.length();
					String restrictions = "";
					String blanks = "";
					while(row < g.size() && emptiesLeft >= 0) {
						Tile t = g.getTile(row, j);
						if(t!=null) {
							restrictions += t.letter();
							blanks += (t.isBlank() ? 'B' : '.');
						}
						else {
							if(emptiesLeft > 0) {
								restrictions += '.';
								blanks += '.';
							}
							emptiesLeft--;
						}
						row++;
					}
					// The words that may fit here
					ArrayList<Word> fittingWords = findWordsRestricted(letterTray, restrictions, blanks);
					// Now check if each one fits with any other words on the board it may have created
					for(Word w : fittingWords) {
						int pointVal = g.wordFits(w.word, i, j, Dir.DOWN, w.blankmask, this);
						if(pointVal >= 0) {
							// It works!
							moves.add(new TilePlacement(w, pointVal, i, j, Dir.DOWN));
						}
					}
				}
			}
		}
		
		// Sort by pointvalue
		Collections.sort(moves);
		return moves;
	}
	
	/**
	 * Restrictions are of the form "BR..KS", where any [A-Z] represents a letter
	 * that must be present in the word at that location, and a . represents a
	 * blank. Just as in scrabble, this might find the words BRO and BREAKS, for example,
	 * but would not find the word BROS because of the presence of the K which would force
	 * the word to be BROSKS (not a word). Additionally, note that BREAK is not a possible
	 * word since the S must be used. Finally, the first letter of the word MUST match up
	 * with the first restriction (i.e., .B...R will NOT return "OR" as a valid result).
	 * 
	 * letters can fill in the blanks in the restrictions.
	 * Does not find one-letter words.
	 */
	private ArrayList<Word> findWordsRestricted(String letters, String restrictions, String blanks) {
		return findWordsRestricted1(letters, restrictions, blanks, root, null, new HashSet<Word>(), false);
	}
	
	private ArrayList<Word> findWordsRestricted1(String letters, String restrictions, String blanks, DictionaryNode n, Word wordSoFar, HashSet<Word> members, boolean anyTileUsed) {
		ArrayList<Word> results = new ArrayList<Word>();
		if(restrictions.length() == 0) {
			return results;
		}
		if(Character.isLetter(restrictions.charAt(0))) {
			DictionaryNode next = n.getNextLetter(restrictions.charAt(0));
			if(next != null) {
				Word newWordSoFar = new Word(wordSoFar);
				newWordSoFar.addLetter(restrictions.charAt(0), blanks.charAt(0) == 'B');
				// If this path represents a new word which is 2 or more letters long, and at least 1 tile was placed
				if(newWordSoFar.length() >= 2 && next.isEndOfWord() && !members.contains(newWordSoFar) && anyTileUsed) {
					// If the next restriction is a defined tile, the word cannot fit there
					if(!(restrictions.length() >= 2 && Character.isLetter(restrictions.charAt(1)))) {
						results.add(newWordSoFar);
						members.add(newWordSoFar);
					}
				}
				results.addAll(findWordsRestricted1(letters, restrictions.substring(1), blanks.substring(1), next, newWordSoFar, members, anyTileUsed));
			}
		}
		else {
			// the next letter can be any one of these letters
			for(int i=0; i<letters.length(); i++) {
				if(Character.isLetter(letters.charAt(i))) {
					DictionaryNode next = n.getNextLetter(letters.charAt(i));
					if(next != null) {
						Word newWordSoFar = new Word(wordSoFar);
						newWordSoFar.addLetter(letters.charAt(i), false);
						// If this path represents a new word which is 2 or more letters long
						if(newWordSoFar.length() >= 2 && next.isEndOfWord() && !members.contains(newWordSoFar)) {
							// If the next restriction is a defined tile, the word cannot fit there
							if(!(restrictions.length() >= 2 && Character.isLetter(restrictions.charAt(1)))) {
								results.add(newWordSoFar);
								members.add(newWordSoFar);
							}
						}
						String newLetterSet = letters.substring(0,i) + letters.substring(i+1);
						results.addAll(findWordsRestricted1(newLetterSet, restrictions.substring(1), blanks.substring(1), next, newWordSoFar, members, true));
					}
				}
				else if(letters.charAt(i) == '*') { // Blank tile
					for(char c = 'A'; c <= 'Z'; c++) {
						DictionaryNode next = n.getNextLetter(c);
						if(next != null) {
							Word newWordSoFar = new Word(wordSoFar);
							newWordSoFar.addLetter(c, true);
							// If this path represents a new word which is 2 or more letters long
							if(newWordSoFar.length() >= 2 && next.isEndOfWord() && !members.contains(newWordSoFar)) {
								// If the next restriction is a defined tile, the word cannot fit there
								if(!(restrictions.length() >= 2 && Character.isLetter(restrictions.charAt(1)))) {
									results.add(newWordSoFar);
									members.add(newWordSoFar);
								}
							}
							String newLetterSet = letters.substring(0,i) + letters.substring(i+1);
							results.addAll(findWordsRestricted1(newLetterSet, restrictions.substring(1), blanks.substring(1), next, newWordSoFar, members, true));
						}
					}
				}
			}
		}
		return results;
	}
	
	/**
	 * Find all possible first moves on gameboard g, using the letters in letterTray.
	 * This means it must cover the board's center square.
	 * Blank letter represented by a *.
	 */
	public ArrayList<TilePlacement> findFirstMoves(GameBoard g, String letterTray) {
		// Kind of inefficient, but it's guaranteed to be faster than findMoves(...)
		// anyway, which is already fast enough.

		// List of moves which are valid
		ArrayList<TilePlacement> moves = new ArrayList<TilePlacement>();
		int center = g.size()/2;
		
		// The words that may fit
		ArrayList<Word> fittingWords = findWords(letterTray);
		
		int startMin = (center + 1 - letterTray.length() >= 0 ? center + 1 - letterTray.length() : 0);
		int startMax = center;
		
		// Iterate over every square in range of first move locations
		for(int i=startMin; i <= startMax; i++) {
			// Now check if each one fits on the board in the "first move" sense
			for(Word w : fittingWords) {
				// RIGHT
				int pointVal = g.wordFitsFirst(w.word, center, i, Dir.RIGHT, w.blankmask);
				if(pointVal >= 0) {
					// It works!
					moves.add(new TilePlacement(w, pointVal, center, i, Dir.RIGHT));
				}
				// DOWN
				pointVal = g.wordFitsFirst(w.word, i, center, Dir.DOWN, w.blankmask);
				if(pointVal >= 0) {
					// It works!
					moves.add(new TilePlacement(w, pointVal, i, center, Dir.DOWN));
				}
			}
		}
	
		// Sort by pointvalue
		Collections.sort(moves);
		return moves;
	}
	
	public ArrayList<Word> findWords(String letters) {
		// The "special" character '*' indicates a wildcard, any letter
		return findWords1(letters, root, null);
	}
	
	private ArrayList<Word> findWords1(String letters, DictionaryNode node, Word wordSoFar) {
		ArrayList<Word> matches = new ArrayList<Word>();
		HashSet<Character> lettersSeen = new HashSet<Character>();
		String avail = letters.toUpperCase();
		// Perform a depth-first search on the dictionary using recursion.
		// If node is marked as end of word, then the wordSoFar is a match.
		if(node.isEndOfWord()) {
			matches.add(wordSoFar);
		}
		for(int i=0; i<avail.length(); i++) {
			char choice = avail.charAt(i);
				// Don't look at repeat letters (same path down trie)
				if(!lettersSeen.contains(choice)) {
					lettersSeen.add(choice);
					String nowAvail = avail.substring(0,i) + avail.substring(i+1);
					if(choice == '*') {
						for(choice = 'A'; choice <= 'Z'; choice++) {
							DictionaryNode nextNode = node.getNextLetter(choice);
							// Base case: no match for chosen letter
							// Recursive case: match exists for chosen letter
							if(nextNode != null) {
								Word newWordSoFar = new Word(wordSoFar);
								newWordSoFar.addLetter(choice, true);
								matches.addAll(findWords1(nowAvail, nextNode, newWordSoFar));
							}
						}
					}
					else {
						DictionaryNode nextNode = node.getNextLetter(choice);
						// Base case: no match for chosen letter
						// Recursive case: match exists for chosen letter
						if(nextNode != null) {
							Word newWordSoFar = new Word(wordSoFar);
							newWordSoFar.addLetter(choice, false);
							matches.addAll(findWords1(nowAvail, nextNode, newWordSoFar));
						}
					}
				}
		}
		return matches;
	}
}
