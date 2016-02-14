package wwf.solver;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GameFile {
	private String filename;
	private String date;
	private String name;
	private int boardType;
	private long msBegun;
	private boolean isNewFile;
	
	public GameFile(String f, String n, int bt, long ms) {
		filename = f;
		name = n.trim();
		msBegun = ms;
		boardType = bt;
		date = (new SimpleDateFormat("MMM d, ''yy - h:mm a")).format(new Date(ms));
		isNewFile = false;
	}
	
	// For when filename unknown because it is newly created and never before saved
	public GameFile(String n, int bt) {
		name = n.trim();
		boardType = bt;
		isNewFile = true;
	}
	
	@Override
	public String toString() {
		// Don't let this get called on an empty GameFile
		return "Game against " + name + " - Started on " + date;
	}
	
	public String getFilename() {
		return filename;
	}
	public String getDateCreated() {
		return date;
	}
	public String getOpponentName() {
		return name;
	}
	public int getGameType() {
		return boardType;
	}
	public void setGameType(int gametype) {
		boardType = gametype;
	}
	public String gameTypeString() {
		if(boardType == GameBoard.WWF) {
			return "WWF";
		}
		else if(boardType == GameBoard.SCRABBLE) {
			return "SCRABBLE";
		}
		return null;
	}
	public long getTimeBegun() {
		return msBegun;
	}
	public boolean isNewFile() {
		return isNewFile;
	}
}
