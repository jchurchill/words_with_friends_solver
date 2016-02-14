package wwf.solver;

public class DictionaryNode {

	private DictionaryNode[] nextLetters = new DictionaryNode[26];
	private boolean isEndOfWord = false;

	public DictionaryNode addLetter(char c) {
		int cc = Character.toUpperCase(c) - 'A';
		if(nextLetters[cc] == null) {
			nextLetters[cc] = new DictionaryNode();
		}
		return nextLetters[cc];
	}
	
	public DictionaryNode addLastLetter(char c) throws Exception {
		DictionaryNode result = addLetter(c);
		if(result.isEndOfWord) {
			throw new Exception("Word may not be added twice to the dictionary.");
		}
		result.isEndOfWord = true;
		return result;
	}
	
	public boolean isEndOfWord() {
		return isEndOfWord;
	}
	
	public DictionaryNode getNextLetter(char c) {
		int cc = Character.toUpperCase(c) - 'A';
		if(cc < 0 || cc > nextLetters.length) {
			return null;
		}
		return nextLetters[cc];
	}
}
