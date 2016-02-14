package wwf.solver;

public class SquareStatus {
	public int r;
	public int c;
	public String s;
	
	public SquareStatus(String s, int r, int c) {
		this.s = new String(s);
		this.r = r;
		this.c = c;
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof SquareStatus)) {
			return false;
		}
		SquareStatus ss = (SquareStatus) o;
		return ss.r == r && ss.c == c && ss.s.equals(s);
	}
}