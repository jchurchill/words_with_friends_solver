package wwf.solver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JTextField;

public class GameSaver {
	
	public static final String saveDir = "wwf_saved";
	
	public static GameFile saveBoard(JTextField[][] boardInputArray, GameFile g, String letterTray) {
		// create saved game directory if doesn't exist
		try {
			(new File(saveDir)).mkdir();
		}
		catch (SecurityException s) {
			// Permissions not allowing this directory to be created
		}
		
		// Create filename if g is empty (new) gamefile
		String filename;
		String oppName = (g.getOpponentName() == null || g.getOpponentName().length() == 0 ? 
				"WWFriend" : g.getOpponentName());
		String boardType = g.gameTypeString();
		long timeCreated;
		if(g.isNewFile()) {
			timeCreated = System.currentTimeMillis();
			filename = "WWFGame" + timeCreated + ".wwf";
		}
		else {
			filename = g.getFilename();
			timeCreated = g.getTimeBegun();
		}
		
		// Save board state to file
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(saveDir + "/" + filename));
			bw.write(saveDir + "\n"); // marker
			
			bw.write(timeCreated + "\n"); // time created in ms
			bw.write(oppName + "\n"); // name of opponent
			bw.write(letterTray + "\n"); // letter tray
			bw.write(boardType + "\n"); // board type
			for(int i=0; i<WWFSolverGUI.BOARD_SIZE; i++) {
				String line = "";
				for(int j=0; j<WWFSolverGUI.BOARD_SIZE; j++) {
					line += "[" + boardInputArray[i][j].getText() + "]";
				}
				bw.write(line + "\n");
			}
			bw.close();
		} catch (IOException e) {
			return null;
		}
		
		return new GameFile(filename, oppName, g.getGameType(), timeCreated);
	}
	
	public static ArrayList<GameFile> getSavedFiles() {
		ArrayList<GameFile> savedGames = new ArrayList<GameFile>();
		// Navigate to directory
		File dir = new File(saveDir);
		if(!dir.isDirectory()) {
			return savedGames;
		}
		File[] files = dir.listFiles();
		for(File f : files) {
			if(f.getName().endsWith("wwf")) {
				try {
					BufferedReader b = new BufferedReader(new FileReader(f));
					String lin = b.readLine();
					if(lin.equals(saveDir)) {
						// Is valid save file unless user is trolling us
						String time = b.readLine();
						String name = b.readLine();
						// For backwards compatibility, don't assume this exists
						int btype = GameBoard.convertBoardTypeToInt(b.readLine());
						// Assume WWF if it doesn't exist.
						btype = (btype == -1 ? GameBoard.WWF : btype);
						
						if(time != null && name != null) {
							long t = Long.parseLong(time);
							GameFile g = new GameFile(f.getName(), name, btype, t);
							savedGames.add(g);
						}
					}
					b.close();
				} catch (Exception e) {
					// If anything goes wrong, we want to do nothing for this file.
				}
			}
		}
		return savedGames;
	}
	
	public static boolean loadBoard(GameFile g, JTextField[][] boardInputArray, JTextField letterTray) {
		if(g == null) return false;
		File f = new File(saveDir + "/" + g.getFilename());
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			// First line must be saveDir
			String lin;
			lin = br.readLine();
			if(!saveDir.equals(lin)) {
				return false;
			}
			// Next line is time, not needed
			br.readLine();
			// Next line is name, not needed
			br.readLine();
			// Next line is letter tray
			lin = br.readLine();
			if(lin == null) {
				return false;
			}
			else {
				letterTray.setText(lin);
			}
			// Next line is either game type (v2.5+) or board (previous)
			lin = br.readLine();
			int btype = GameBoard.convertBoardTypeToInt(lin);
			boolean skipFirstBoardLine;
			if(btype != -1) {
				g.setGameType(btype);
				skipFirstBoardLine = false;
			}
			else {
				skipFirstBoardLine = true;
			}
			// Next lines are board
			for(int i=0; i<WWFSolverGUI.BOARD_SIZE; i++) {
				// Backwards compatibility... ugh
				if(i > 0 || (i==0 && !skipFirstBoardLine)) {
					lin = br.readLine();
				}
				if(lin == null) {
					return false;
				}
				else {
					for(int j=0; j<WWFSolverGUI.BOARD_SIZE; j++) {
						int p1 = lin.indexOf('[');
						int p2 = lin.indexOf(']');
						if(p1 < 0 || p2 < 0 || p2 < p1) {
							return false;
						}
						else {
							boardInputArray[i][j].setText(lin.substring(p1+1,p2));
						}
						lin = lin.substring(p2+1);
					}
				}
			}
			br.close();
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	public static void deleteGame(GameFile g) {
		File f = new File(saveDir + "/" + g.getFilename());
		try {
			f.delete();
		}
		catch (SecurityException se) {
			return;
		}
	}
}
