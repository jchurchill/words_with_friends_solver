package wwf.solver;
import java.util.ArrayList;

import wwf.solver.GameBoard.Dir;

public class WWFTester {

	private static Dictionary dict = new Dictionary();
	private static String[] dictionaryFiles = {"/resource/CROSSWD.TXT", "/resource/CRSWD-D.TXT"};
	
	public static void main(String[] args) {
		for(String s : dictionaryFiles) {
			try {
				Dictionary.makeDictionary(dict, s);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		int size = 15;
		GameBoard board = new GameBoard(size, GameBoard.WWF);
		
		char[][] squares = new char[][] {
			{'-', '-', '-', 'A', 'B', 'I', 'D', 'E', '-', '-', '-', 'A', '-', '-', '-'},
			{'-', 'F', 'U', 'Z', 'E', 'D', '-', '-', 'S', 'C', 'U', 'B', 'A', '-', '-'},
			{'-', '-', 'H', 'I', 'T', '-', '-', '-', '-', 'L', '-', 'O', '-', 'S', '-'},
			{'-', '-', '-', 'D', '-', '-', '-', 'G', '-', 'E', '-', 'V', 'E', 'T', 'O'},
			{'-', '-', 'P', 'E', '-', '-', 'A', 'R', '-', 'R', '-', 'E', '-', 'E', '-'},
			{'-', 'F', 'E', '-', 'U', 'N', 'M', 'A', 'S', 'K', '-', '-', '-', 'M', 'Y'},
			{'G', 'E', 'N', '-', '-', '-', 'I', 'D', '-', '-', '-', '-', '-', '-', 'E'},
			{'L', '-', 'T', 'R', 'O', 'V', 'E', 'S', '-', '-', '-', 'Q', 'U', 'I', 'T'},
			{'O', '-', '-', '-', '-', 'O', '-', '-', '-', '-', '-', '-', '-', '-', '-'},
			{'A', 'T', '-', '-', 'O', 'X', '-', '-', '-', '-', '-', '-', '-', '-', '-'},
			{'T', 'O', 'P', '-', 'I', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-'},
			{'E', 'R', 'A', '-', 'L', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-'},
			{'D', 'A', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-'},
			{'-', 'S', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-'},
			{'-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-'},
		};
		
		for(int i=0; i<size; i++) {
			for(int j=0; j<size; j++) {
				if(squares[i][j] != '-') {
					board.setSquare(i,j,squares[i][j],false);
				}
			}
		}
		// Special blank spots
		board.getTile(4, 11).setBlank();
		board.getTile(12, 0).setBlank();
		
		board.dispBoard();
		
		long time = System.currentTimeMillis();
		ArrayList<TilePlacement> ss = dict.findMoves(board, "IWGYREA");
		for(TilePlacement t : ss) {
			System.out.println(t.word + " - " + t.points + " pts - (" + t.r + "," + t.c + ") - " + (t.dir == Dir.DOWN ? "down" : "right"));
		}
		System.out.println(ss.size() + " results.");
		System.out.println("Analysis took " + (System.currentTimeMillis()-time) + " ms.");
		
	}
	

}




