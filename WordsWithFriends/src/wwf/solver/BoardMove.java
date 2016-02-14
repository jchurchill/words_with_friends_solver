package wwf.solver;

import java.util.ArrayList;

public class BoardMove {
	public TilePlacement placement;
	ArrayList<SquareStatus> tilesPlaced;
	
	public BoardMove(TilePlacement t, ArrayList<SquareStatus> tileLocs) {
		placement = t;
		tilesPlaced = new ArrayList<SquareStatus>();
		for(SquareStatus op : tileLocs) {
			tilesPlaced.add(op);
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof BoardMove)) {
			return false;
		}
		BoardMove b = (BoardMove) o;
		return placement.equals(b.placement);
	}
	
}