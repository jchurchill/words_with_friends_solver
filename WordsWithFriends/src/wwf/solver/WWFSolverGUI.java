package wwf.solver;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Stack;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import wwf.solver.GameBoard.Dir;

public class WWFSolverGUI extends JFrame {

	private static final long serialVersionUID = 1L;
	public static final int BOARD_SIZE = 15; // all games have board size 15
	public static final int SEL_TILE_DARKNESS = 75; // 75 is a decent value
	
	// Menu bar
	JMenuBar menuBar = new JMenuBar();
	JMenuItem saveGameItem;
	JMenuItem deleteGameItem;
	JMenu gameTypeMenu;
	
	// Current game's save file (if created)
	GameFile currentGame;
	boolean changeSinceLastSave = false;
	
	/////// Find Moves Tab ///////
	// Game selection box
	JComboBox gameSelectBox;
	// Squares on board for input
	JTextField boardSquares[][] = new JTextField[BOARD_SIZE][BOARD_SIZE];
	// Button for querying for results
	JButton queryButton;
	// Input for letter tray
	JTextField letterTrayField;
	// Result count label
	JLabel queryResultCountLabel;
	// Result Display List
	JList queryDataList;
	// Result Display Area
	JScrollPane queryResultsPane;
	// Search time report
	JLabel queryTimeLabel;
	// Point Count for selection
	JLabel pointDispLabel;
	// Place Word button
	JButton placeWordButton;
	// Opponent word result count label
	JLabel opponentResultCountLabel;
	// Opponent word possibilities display list
	JList opponentDataList;
	// Opponent word possibilities display area
	JScrollPane opponentResultsPane;
	// Opponent word input
	JTextField opponentWordField;
	// Opponent word find
	JButton opponentWordFind;
	// Opponent word place
	JButton opponentWordPlace;
	
	// Stack for undoing moves made
	Stack<BoardMove> undoList = new Stack<BoardMove>();
	public static final int MAX_UNDO_CT = 30;
	JButton undoButton;
	
	// Engines that handle all back-end work
	GameBoard board;
	Dictionary dict;
	private static String[] dictionaryFiles = {"/resource/CROSSWD.TXT", "/resource/CRSWD-D.TXT"};

	@SuppressWarnings("unused")
	public static void main(String args[]) {
		// Run the GUI
		WWFSolverGUI gui = new WWFSolverGUI();
	}
	
	public WWFSolverGUI() {
		super("Words With Friends Solver v3.0 by Justin Churchill");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JPanel everything = new JPanel();
		everything.setLayout(new BoxLayout(everything, BoxLayout.LINE_AXIS));
		everything.add(Box.createHorizontalStrut(30));
		everything.add(makeBoardAndTrayPanel());
		everything.add(Box.createHorizontalStrut(30));
		everything.add(makeWordResultsPanel());
		everything.add(Box.createHorizontalStrut(30));
		
		getContentPane().add(everything);
		
		// Add listener for game-saving check when closing
		this.addWindowListener(new WindowListener() {
			@Override
			public void windowActivated(WindowEvent arg0) {}
			@Override
			public void windowClosed(WindowEvent arg0) {}
			@Override
			public void windowClosing(WindowEvent arg0) {
				if(changeSinceLastSave) {
					boolean b = askForSave("Exit", JOptionPane.YES_NO_OPTION);
					if(b) {
						dispose();
					}
				}
			}
			@Override
			public void windowDeactivated(WindowEvent arg0) {}
			@Override
			public void windowDeiconified(WindowEvent arg0) {}
			@Override
			public void windowIconified(WindowEvent arg0) {}
			@Override
			public void windowOpened(WindowEvent arg0) {}
		});
		
		// Create menu bar
		this.setJMenuBar(menuBar);
		setupMenuBar();
		
		// Load solving engine (load dictionaries)
		dict = new Dictionary();
		for(String s : dictionaryFiles) {
			try {
				Dictionary.makeDictionary(dict, s);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// Setup board
		board = new GameBoard(BOARD_SIZE, GameBoard.WWF);
		
		// Dont enable gameplay until new game started or game loaded
		queryButtonPressed();
		setGameplayEnabled(false);
		
		// Make the window appear once all is finished
		setResizable(false);
		pack();
		setVisible(true);
	}
	
	private void setupMenuBar() {
		JMenu menu;
		JMenuItem menuItem;
		
		///// Game menu /////
		menu = new JMenu("Game");
		menuBar.add(menu);
		// New game
		menuItem = new JMenuItem("New Game",
                new ImageIcon("resource/tile.gif"));
		menuItem.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_N, ActionEvent.CTRL_MASK));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				// Check if user wants to save current game (if stale savestate)
				if(changeSinceLastSave) {
					boolean b = askForSave("Create New Game", JOptionPane.YES_NO_CANCEL_OPTION);
					if(!b) {
						return;
					}
				}
				
				// Make a new game
				String name = JOptionPane.showInputDialog(
						"Whom are you playing against?",
						"J. Churchill");
				
				if(name != null) {
					GameFile g = new GameFile(name, board.getGameType());
					clearBoard();
					currentGame = GameSaver.saveBoard(boardSquares, g, "");
					changeSinceLastSave = false;
					gameSelectBox.addItem(currentGame);
					gameSelectBox.setSelectedItem(currentGame);
					// Enable buttons and textfields
					setGameplayEnabled(true);
					undoList.clear();
					queryButtonPressed();
					opponentWordField.setText("");
					opponentWordFindPressed();
				}
			}
		});
		menu.add(menuItem);
		// Save current game
		saveGameItem = new JMenuItem("Save Game",
                new ImageIcon("resource/tile.gif"));
		saveGameItem.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_S, ActionEvent.CTRL_MASK));
		saveGameItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				// Check if user wants to save current game (if stale savestate)
				if(changeSinceLastSave) {
					GameSaver.saveBoard(boardSquares, currentGame, letterTrayField.getText());
					changeSinceLastSave = false;
				}
			}
		});
		menu.add(saveGameItem);
		// Delete current game
		deleteGameItem = new JMenuItem("Delete Game",
                new ImageIcon("resource/tile.gif"));
		deleteGameItem.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_R, ActionEvent.CTRL_MASK));
		deleteGameItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				// Ask if sure
				int choice = JOptionPane.showConfirmDialog(
						new JFrame(),
						"Are you sure you are finished with this game?\n" +
						"It will be permanently deleted.",
						"Delete Game",
						JOptionPane.YES_NO_OPTION);
				
				if(choice == JOptionPane.YES_OPTION) {
					GameSaver.deleteGame(currentGame);
					currentGame = null;
					changeSinceLastSave = false;
					undoList.clear();
					undoButton.setEnabled(false);
					updateComboBox();
					setGameplayEnabled(false);
				}
			}
		});
		menu.add(deleteGameItem);
		
		// select game type
		gameTypeMenu = new JMenu("Game Type");
		menu.add(gameTypeMenu);
		// WWF
		menuItem = new JMenuItem("Words With Friends",
                new ImageIcon("resource/tile.gif"));
		menuItem.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_1, ActionEvent.CTRL_MASK));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				// Set to WWF settings
				if(board.getGameType() != GameBoard.WWF) {
					changeSinceLastSave = true;
				}
				board.setGameType(GameBoard.WWF);
				currentGame.setGameType(GameBoard.WWF);
				// Update GUI
				resetHighlighting();
				queryButtonPressed();
				opponentWordFindPressed();
			}
		});
		gameTypeMenu.add(menuItem);
		//Scrabble
		menuItem = new JMenuItem("Scrabble",
                new ImageIcon("resource/tile.gif"));
		menuItem.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_2, ActionEvent.CTRL_MASK));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				// Set to Scrabble settings
				if(board.getGameType() != GameBoard.SCRABBLE) {
					changeSinceLastSave = true;
				}
				board.setGameType(GameBoard.SCRABBLE);
				currentGame.setGameType(GameBoard.SCRABBLE);
				// Update GUI
				resetHighlighting();
				queryButtonPressed();
				opponentWordFindPressed();
			}
		});
		gameTypeMenu.add(menuItem);
		
		///// HELP BAR /////
		menu = new JMenu("Help");
		menuBar.add(menu);
		// Instructions
		// Delete current game
		menuItem = new JMenuItem("Instructions",
                new ImageIcon("resource/tile.gif"));
		menuItem.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_I, ActionEvent.CTRL_MASK));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				// Show instructions
				JOptionPane.showMessageDialog(
						new JFrame(),
						"-STARTING A NEW GAME-\n" +
						"Start a new game by going to Game > New Game or by pressing " +
						"(Ctrl+N).\n" +
						"\n" +
						"-THE BOARD-\n" +
						"Enter the letters on your game board into the squares provided. If " +
						"the letter on the board is a blank tile which has been assigned some\n" +
						"letter already, enter it with an asterisk (*) after the letter.\n" +
						"\n" +
						"-THE LETTER TRAY-\n" +
						"Enter the letters in your tray into the space provided. If you have " +
						"a blank tile, enter it in as an asterisk (*). Click \"Find Words!\"\n" +
						"to display a list of every possible move you can make, along with " +
						"each move's point value. Navigate through the list to see where each\n" +
						"word fits on the board to make the best selection. Remember to be " +
						"careful about opening up good opportunities for your opponent!\n" +
						"\n" +
						"-PLACING OPPONENT'S WORDS-\n" +
						"When your opponent plays a word, you'll need to place it on the board. " +
						"You can do this manually by entering each letter onto the board\n" +
						"squares, or you can use the word-placing utility. Simply enter your " +
						"opponent's word into the designated box and hit the button marked\n" +
						"\"Fit Word to Board\". Then, select from the list the placement that " +
						"your opponent actually played, and use the \"Place Friend's Word\"\n" +
						"button to place it on the board! If your opponent used a blank tile " +
						"in their word, enter an asterisk (*) after the letter corresponding\n" +
						"to the blank tile. For example, \"BLAN*K\" has the letter N as a " +
						"blank tile.\n" +
						"\n" +
						"-GAME TYPE-\n" +
						"This program can also help you in regular Scrabble! To switch boards " +
						"and tile point values, select the desired game type from the Game\n" +
						"dropdown menu. Words With Friends: (Ctrl+1). Scrabble: (Ctrl+2).\n" +
						"\n" +
						"-SAVING GAMES IN PROGRESS-\n" +
						"Save your games as you go to avoid re-entering the letters into the " +
						"board again (Ctrl+S). Delete games you are finished with to keep from\n" +
						"cluttering your active games list (Ctrl+R). Note: saved games will be " +
						"kept in a folder called \"wwf_saved\" in the same directory where this\n" +
						"program is located.\n" +
						"\n" +
						"Enjoy!",
						"Instructions",
						JOptionPane.INFORMATION_MESSAGE);
			}
		});
		menu.add(menuItem);
		
	}
	
	private JPanel makeBoardAndTrayPanel() {
		JPanel result = new JPanel();
		result.setLayout(new BoxLayout(result, BoxLayout.PAGE_AXIS));
		
		JPanel p0 = new JPanel();
		p0.setLayout(new BoxLayout(p0, BoxLayout.LINE_AXIS));
		p0.add(Box.createHorizontalGlue());
		gameSelectBox = new JComboBox();
		updateComboBox();
		gameSelectBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Object o = gameSelectBox.getSelectedItem();
				if((o instanceof GameFile)) { // Strings are sometimes in here for info purposes
					GameFile g = (GameFile) o;
					if(!(currentGame != null && g.getFilename().equals(currentGame.getFilename()))) {
						if(currentGame != null && changeSinceLastSave) {
							askForSave("Moving to different game", JOptionPane.YES_NO_CANCEL_OPTION);
						}
						boolean loadSuccess = GameSaver.loadBoard(g, boardSquares, letterTrayField);
						if(loadSuccess) {
							currentGame = g;
							changeSinceLastSave = false;
							board.setGameType(g.getGameType());
							resetHighlighting();
							setGameplayEnabled(true);
							undoList.clear();
							queryButtonPressed();
							opponentWordField.setText("");
							opponentWordFindPressed();
						}
						else {
							// Display error that save file is corrupt
							JOptionPane.showMessageDialog(
									new JFrame(),
									"Error: Save file is corrupt. Will be deleted.",
									"Error",
									JOptionPane.ERROR_MESSAGE);
							// Delete it
							GameSaver.deleteGame(g);
							currentGame = null;
							changeSinceLastSave = false;
							undoList.clear();
							undoButton.setEnabled(false);
							updateComboBox();
							setGameplayEnabled(false);
						}
					}
				}
			}
		});
		p0.add(gameSelectBox);
		p0.add(Box.createHorizontalGlue());
		result.add(Box.createVerticalStrut(30));
		result.add(p0);
		result.add(Box.createVerticalStrut(10));
		
		JPanel squares = new JPanel();
		squares.setLayout(new BoxLayout(squares, BoxLayout.PAGE_AXIS));
		for(int i=0; i<BOARD_SIZE; i++) {
			JPanel row = new JPanel();
			row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS));
			for(int j=0; j<BOARD_SIZE; j++) {
				boardSquares[i][j] = new JTextField();
				boardSquares[i][j].setPreferredSize(new Dimension(30,30));
				boardSquares[i][j].setHorizontalAlignment(JTextField.CENTER);
				boardSquares[i][j].setFont(boardSquares[i][j].getFont().deriveFont(Font.BOLD, 16));
				boardSquares[i][j].setDocument(new FixedLenDocument(2));
				boardSquares[i][j].addKeyListener(new KeyListener() {
					@Override
					public void keyPressed(KeyEvent arg0) {}
					@Override
					public void keyReleased(KeyEvent arg0) {}
					@Override
					public void keyTyped(KeyEvent arg0) {
						changeSinceLastSave = true;
					}
				});
				row.add(boardSquares[i][j]);
			}
			row.add(Box.createHorizontalGlue());
			squares.add(row);
		}
		result.add(squares);
		result.add(Box.createVerticalStrut(15));
		
		JPanel p4 = new JPanel();
		p4.setLayout(new BoxLayout(p4, BoxLayout.LINE_AXIS));
		p4.add(Box.createHorizontalGlue());
		pointDispLabel = new JLabel();
		p4.add(pointDispLabel);
		p4.add(Box.createHorizontalGlue());
		result.add(p4);
		result.add(Box.createVerticalStrut(30));
		
		JPanel p5 = new JPanel();
		p5.setLayout(new BoxLayout(p5, BoxLayout.LINE_AXIS));
		p5.add(Box.createHorizontalGlue());
		p5.add(new JLabel("Your Letter Tray (blank = *):"));
		p5.add(Box.createHorizontalGlue());
		result.add(p5);
		result.add(Box.createVerticalStrut(5));
		
		letterTrayField = new JTextField();
		letterTrayField.setHorizontalAlignment(JTextField.CENTER);
		letterTrayField.setFont(letterTrayField.getFont().deriveFont(Font.BOLD, 20));
		letterTrayField.setDocument(new FixedLenDocument(7));
		letterTrayField.setPreferredSize(new Dimension(250, 40));
		letterTrayField.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent arg0) {}
			@Override
			public void keyReleased(KeyEvent arg0) {}
			@Override
			public void keyTyped(KeyEvent arg0) {
				changeSinceLastSave = true;
			}
		});
		result.add(letterTrayField);
		result.add(Box.createVerticalStrut(5));
		JPanel p1 = new JPanel();
		p1.setLayout(new BoxLayout(p1, BoxLayout.LINE_AXIS));
		p1.add(Box.createHorizontalGlue());
		queryButton = new JButton("Find words!");
		queryButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				queryButtonPressed();
			}
		});
		p1.add(queryButton);
		p1.add(Box.createHorizontalGlue());
		result.add(p1);
		result.add(Box.createVerticalStrut(15));
		result.add(Box.createVerticalGlue());
	
		return result;
	}
	
	private JPanel makeWordResultsPanel() {
		JPanel result = new JPanel();
		result.setLayout(new BoxLayout(result, BoxLayout.PAGE_AXIS));
		
		// Upper part with display panes and "place selected word" buttons
		JPanel upper = new JPanel();
		upper.setLayout(new BoxLayout(upper, BoxLayout.LINE_AXIS));
		
		// Player side (left)
		JPanel upperLeft = new JPanel();
		upperLeft.setLayout(new BoxLayout(upperLeft, BoxLayout.PAGE_AXIS));
		
		JPanel p0 = new JPanel();
		p0.setLayout(new BoxLayout(p0, BoxLayout.LINE_AXIS));
		p0.add(Box.createHorizontalGlue());
		queryResultCountLabel = new JLabel("Your Results");
		p0.add(queryResultCountLabel);
		p0.add(Box.createHorizontalGlue());
		upperLeft.add(Box.createVerticalStrut(30));
		upperLeft.add(p0);
		queryDataList = new JList();
		queryDataList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		queryDataList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				// Clear all highlighting
				resetHighlighting();
				// Highlight the location on board of the word selected
				Object o = queryDataList.getSelectedValue();
				if(o instanceof TilePlacement) {
					TilePlacement t = (TilePlacement) o;
					if(t.dir == Dir.RIGHT) {
						int r = t.r;
						int c = t.c;
						for(int i=0; i<t.word.length(); i++) {
							Color cc = boardSquares[r][c+i].getBackground();
							int red = (cc.getRed() >= SEL_TILE_DARKNESS ? cc.getRed()-SEL_TILE_DARKNESS : 0);
							int green = (cc.getGreen() >= SEL_TILE_DARKNESS ? cc.getGreen()-SEL_TILE_DARKNESS : 0);
							int blue = (cc.getBlue() >= SEL_TILE_DARKNESS ? cc.getBlue()-SEL_TILE_DARKNESS : 0);
							Color newColor = new Color(red,green,blue);
							boardSquares[r][c+i].setBackground(newColor);
						}
					}
					else { // DOWN
						int r = t.r;
						int c = t.c;
						for(int i=0; i<t.word.length(); i++) {
							Color cc = boardSquares[r+i][c].getBackground();
							int red = (cc.getRed() >= SEL_TILE_DARKNESS ? cc.getRed()-SEL_TILE_DARKNESS : 0);
							int green = (cc.getGreen() >= SEL_TILE_DARKNESS ? cc.getGreen()-SEL_TILE_DARKNESS : 0);
							int blue = (cc.getBlue() >= SEL_TILE_DARKNESS ? cc.getBlue()-SEL_TILE_DARKNESS : 0);
							Color newColor = new Color(red,green,blue);
							boardSquares[r+i][c].setBackground(newColor);
						}
					}
					pointDispLabel.setText(t.word.word + " for " + t.points + " pts");
				}
			}
		});
		queryResultsPane = new JScrollPane(queryDataList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		queryResultsPane.setPreferredSize(new Dimension(170, 450));
		upperLeft.add(Box.createVerticalStrut(10));
		upperLeft.add(queryResultsPane);
		
		JPanel p1 = new JPanel();
		p1.setLayout(new BoxLayout(p1, BoxLayout.LINE_AXIS));
		p1.add(Box.createHorizontalGlue());
		placeWordButton = new JButton("Place Your Word");
		placeWordButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				placeYourWordButtonPressed();
			}
		});
		p1.add(placeWordButton);
		p1.add(Box.createHorizontalGlue());
		upperLeft.add(Box.createVerticalStrut(10));
		upperLeft.add(p1);
		
		// Friend side (right)
		JPanel upperRight = new JPanel();
		upperRight.setLayout(new BoxLayout(upperRight, BoxLayout.PAGE_AXIS));
		
		JPanel p2 = new JPanel();
		p2.setLayout(new BoxLayout(p2, BoxLayout.LINE_AXIS));
		p2.add(Box.createHorizontalGlue());
		opponentResultCountLabel = new JLabel("Friend's Results (0)");
		p2.add(opponentResultCountLabel);
		p2.add(Box.createHorizontalGlue());
		upperRight.add(Box.createVerticalStrut(30));
		upperRight.add(p2);
		opponentDataList = new JList();
		opponentDataList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		opponentDataList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				// Clear all highlighting
				resetHighlighting();
				// Highlight the location on board of the word selected
				Object o = opponentDataList.getSelectedValue();
				if(o instanceof TilePlacement) {
					TilePlacement t = (TilePlacement) o;
					if(t.dir == Dir.RIGHT) {
						int r = t.r;
						int c = t.c;
						for(int i=0; i<t.word.length(); i++) {
							Color cc = boardSquares[r][c+i].getBackground();
							int red = (cc.getRed() >= SEL_TILE_DARKNESS ? cc.getRed()-SEL_TILE_DARKNESS : 0);
							int green = (cc.getGreen() >= SEL_TILE_DARKNESS ? cc.getGreen()-SEL_TILE_DARKNESS : 0);
							int blue = (cc.getBlue() >= SEL_TILE_DARKNESS ? cc.getBlue()-SEL_TILE_DARKNESS : 0);
							Color newColor = new Color(red,green,blue);
							boardSquares[r][c+i].setBackground(newColor);
						}
					}
					else { // DOWN
						int r = t.r;
						int c = t.c;
						for(int i=0; i<t.word.length(); i++) {
							Color cc = boardSquares[r+i][c].getBackground();
							int red = (cc.getRed() >= SEL_TILE_DARKNESS ? cc.getRed()-SEL_TILE_DARKNESS : 0);
							int green = (cc.getGreen() >= SEL_TILE_DARKNESS ? cc.getGreen()-SEL_TILE_DARKNESS : 0);
							int blue = (cc.getBlue() >= SEL_TILE_DARKNESS ? cc.getBlue()-SEL_TILE_DARKNESS : 0);
							Color newColor = new Color(red,green,blue);
							boardSquares[r+i][c].setBackground(newColor);
						}
					}
					pointDispLabel.setText(t.word.word + " for " + t.points + " pts");
				}
			}
		});
		opponentResultsPane = new JScrollPane(opponentDataList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		opponentResultsPane.setPreferredSize(new Dimension(170, 390));
		upperRight.add(Box.createVerticalStrut(10));
		upperRight.add(opponentResultsPane);
		
		JPanel p3 = new JPanel();
		p3.setLayout(new BoxLayout(p3, BoxLayout.LINE_AXIS));
		p3.add(Box.createHorizontalGlue());
		opponentWordPlace = new JButton("Place Friend's Word");
		opponentWordPlace.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				placeOpponentWordButtonPressed();
			}
		});
		p3.add(opponentWordPlace);
		p3.add(Box.createHorizontalGlue());
		upperRight.add(Box.createVerticalStrut(10));
		upperRight.add(p3);
		
		JPanel p6 = new JPanel();
		p6.setLayout(new BoxLayout(p6, BoxLayout.LINE_AXIS));
		p6.add(Box.createHorizontalGlue());
		opponentWordField = new JTextField();
		opponentWordField.setHorizontalAlignment(JTextField.CENTER);
		opponentWordField.setFont(letterTrayField.getFont().deriveFont(Font.BOLD, 12));
		opponentWordField.setDocument(new FixedLenDocument(BOARD_SIZE+2));
		opponentWordField.setPreferredSize(new Dimension(170, 30));
		p6.add(opponentWordField);
		p6.add(Box.createHorizontalGlue());
		upperRight.add(Box.createVerticalStrut(10));
		upperRight.add(p6);
		
		JPanel p7 = new JPanel();
		p7.setLayout(new BoxLayout(p7, BoxLayout.LINE_AXIS));
		p7.add(Box.createHorizontalGlue());
		opponentWordFind = new JButton("Fit Word to Board");
		opponentWordFind.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				opponentWordFindPressed();
			}
		});
		p7.add(opponentWordFind);
		p7.add(Box.createHorizontalGlue());
		upperRight.add(Box.createVerticalStrut(10));
		upperRight.add(p7);
		
		// Add halves to upper, add upper to result.
		result.add(Box.createVerticalStrut(9));
		upper.add(upperLeft);
		upper.add(Box.createHorizontalStrut(30));
		upper.add(upperRight);
		result.add(upper);
		result.add(Box.createVerticalStrut(15));
		
		// Undo button and search report
		JPanel p4 = new JPanel();
		p4.setLayout(new BoxLayout(p4, BoxLayout.LINE_AXIS));
		p4.add(Box.createHorizontalGlue());
		undoButton = new JButton("Undo Last Placement");
		undoButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				undoButtonPressed();
			}
		});
		p4.add(undoButton);
		p4.add(Box.createHorizontalGlue());
		result.add(Box.createVerticalStrut(10));
		result.add(p4);
		
		JPanel p5 = new JPanel();
		p5.setLayout(new BoxLayout(p5, BoxLayout.LINE_AXIS));
		p5.add(Box.createHorizontalGlue());
		queryTimeLabel = new JLabel("Waiting for search.");
		p5.add(queryTimeLabel);
		p5.add(Box.createHorizontalGlue());
		result.add(Box.createVerticalStrut(10));
		result.add(p5);
		result.add(Box.createVerticalStrut(15));
		result.add(Box.createVerticalGlue());
		
		return result;
	}
	
	private void queryButtonPressed() {
		String letters = letterTrayField.getText();
		letters = letters.replace(" ", "");
		// Check validity of input
		boolean valid = true;
		int blankCt = 0;
		for(int i=0; i<letters.length(); i++) {
			if(!Character.isLetter(letters.charAt(i))) {
				if(letters.charAt(i) == '*') {
					blankCt++;
				}
				else {
					valid = false;
				}
			}
		}
		// Update board state from inputs
		boolean okayBoard = takeBoardInput();
		
		// Clear all highlighting
		resetHighlighting();
		// Error handling
		if(!okayBoard) {
			queryDataList.setListData(new String[] {"Error.","Please check your", "board letters","and try again."});
			queryTimeLabel.setText("Waiting for search.");
			pointDispLabel.setText("Error.");
			queryResultCountLabel.setText("Your Results");
		}
		else if(!valid) {
			queryDataList.setListData(new String[] {"Error.","Please check your", "tray letters","and try again."});
			queryTimeLabel.setText("Waiting for search.");
			pointDispLabel.setText("Error.");
			queryResultCountLabel.setText("Your Results");
		}
		else if(blankCt > 2) {
			queryDataList.setListData(new String[] {"Error.","You may not perform", "a search with more", "than 2 blank tiles."});
			queryTimeLabel.setText("Waiting for search.");
			pointDispLabel.setText("Error.");
			queryResultCountLabel.setText("Your Results");
		}
		else {
			long timeTaken = System.currentTimeMillis();
			ArrayList<TilePlacement> found;
			if(board.isEmpty()) {
				found = dict.findFirstMoves(board, letters);
			}
			else {
				found = dict.findMoves(board, letters);
			}
			if(found.size() == 0) {
				queryDataList.setListData(new String[] {"<none>"});
				pointDispLabel.setText("No possible moves with these letters.");
			}
			else {
				queryDataList.setListData(found.toArray());
				pointDispLabel.setText("Select a word to the right!");
			}
			timeTaken = System.currentTimeMillis() - timeTaken;
			queryTimeLabel.setText(String.format("Search took %.3f seconds.", (double)timeTaken/1000.0));
			queryResultCountLabel.setText("Your Results (" + found.size() + ")");
			if(undoList.size() == 0) {
				undoButton.setEnabled(false);
			}
		}
	}
	
	private void opponentWordFindPressed() {
		String wordInput = opponentWordField.getText();
		wordInput = wordInput.replace(" ", "");
		// Check validity of input
		// Build up word as you do it
		Word word = new Word(null);
		boolean valid = true;
		int blankCt = 0;
		for(int i=0; i<wordInput.length(); i++) {
			if(!Character.isLetter(wordInput.charAt(i))) {
				valid = false;
				break;
			}
			else {
				if(i < wordInput.length()-1 && wordInput.charAt(i+1) == '*') {
					word.addLetter(wordInput.charAt(i), true);
					blankCt++;
					i++;
				}
				else {
					word.addLetter(wordInput.charAt(i), false);
				}
			}
		}
		// Update board state from inputs
		boolean okayBoard = takeBoardInput();
		
		// Clear all highlighting
		resetHighlighting();
		// Error handling
		if(!okayBoard) {
			opponentDataList.setListData(new String[] {"Error.","Please check your", "board letters","and try again."});
			opponentResultCountLabel.setText("Friend's Results");
		}
		else if(!valid) {
			opponentDataList.setListData(new String[] {"Error.","Please check your", "friend's word","and try again."});
			opponentResultCountLabel.setText("Friend's Results");
		}
		else if(blankCt > 2) {
			opponentDataList.setListData(new String[] {"Error.","Your friend's word", "cannot contain more", "than 2 blank tiles."});
			opponentResultCountLabel.setText("Friend's Results");
		}
		else {
			ArrayList<TilePlacement> found;
			found = board.findLocationsForWord(word, dict);
			
			if(found.size() == 0) {
				opponentDataList.setListData(new String[] {"<none>"});
			}
			else {
				opponentDataList.setListData(found.toArray());
			}
		}
	}
	
	private void resetHighlighting() {
		for(int i=0; i<BOARD_SIZE; i++) {
			for(int j=0; j<BOARD_SIZE; j++) {
				switch(board.getMul(i, j)) {
				case DL:
					boardSquares[i][j].setBackground(Color.CYAN);
					break;
				case TL:
					boardSquares[i][j].setBackground(Color.GREEN);
					break;
				case DW:
					boardSquares[i][j].setBackground(Color.PINK);
					break;
				case TW:
					boardSquares[i][j].setBackground(Color.YELLOW);
					break;
				case XX:
					boardSquares[i][j].setBackground(Color.LIGHT_GRAY);
					break;
				}
			}
		}
	}
	
	/**
	 * Returns true if board input is all valid.
	 */
	private boolean takeBoardInput() {
		boolean okayBoard = true;
		for(int i=0; i<BOARD_SIZE; i++) {
			for(int j=0; j<BOARD_SIZE; j++) {
				String text = boardSquares[i][j].getText();
				if(text.length() == 0) {
					board.clearSquare(i,j);
				}
				else if(text.length() == 1) {
					if(!Character.isLetter(text.charAt(0))) {
						okayBoard = false;
					}
					else {
						board.setSquare(i,j,text.charAt(0),false);
					}
				}
				else if(text.length() == 2) {
					if(!Character.isLetter(text.charAt(0))) {
						okayBoard = false;
					}
					else {
						if(text.charAt(1) != '*') {
							okayBoard = false;
						}
						else {
							board.setSquare(i,j,text.charAt(0),true);
						}
					}
				}
				else if(text.length() > 2) {
					okayBoard = false;
				}
			}
		}
		return okayBoard;
	}
	
	private void placeYourWordButtonPressed() {
		Object o = queryDataList.getSelectedValue();
		if(o instanceof TilePlacement) {
			TilePlacement t = (TilePlacement) o;
			ArrayList<SquareStatus> tilesPlaced = new ArrayList<SquareStatus>();
			changeSinceLastSave = true;
			if(t.dir == Dir.RIGHT) {
				for(int i=0; i<t.word.length(); i++) {
					tilesPlaced.add(new SquareStatus(boardSquares[t.r][t.c+i].getText(), t.r, t.c+i));
					boardSquares[t.r][t.c+i].setText(
							t.word.word.charAt(i) +
							(t.word.blankmask.charAt(i) == 'B' ? "*" : "")
							);
				}
			}
			else { // DOWN
				for(int i=0; i<t.word.length(); i++) {
					tilesPlaced.add(new SquareStatus(boardSquares[t.r+i][t.c].getText(), t.r+i, t.c));
					boardSquares[t.r+i][t.c].setText(
							t.word.word.charAt(i) +
							(t.word.blankmask.charAt(i) == 'B' ? "*" : "")
							);
				}
			}
			
			BoardMove b = new BoardMove(t, tilesPlaced);
			// Dont add to undo stack if this is the same boardmove as the last one
			if(!(undoList.size() > 0 && undoList.peek().equals(b))) {
				undoList.push(b);
				if(undoList.size() > MAX_UNDO_CT) {
					undoList.remove(undoList.size()-1);
				}
				undoButton.setEnabled(true);
			}
		}
	}
	
	private void placeOpponentWordButtonPressed() {
		Object o = opponentDataList.getSelectedValue();
		if(o instanceof TilePlacement) {
			TilePlacement t = (TilePlacement) o;
			ArrayList<SquareStatus> tilesPlaced = new ArrayList<SquareStatus>();
			changeSinceLastSave = true;
			if(t.dir == Dir.RIGHT) {
				for(int i=0; i<t.word.length(); i++) {
					tilesPlaced.add(new SquareStatus(boardSquares[t.r][t.c+i].getText(), t.r, t.c+i));
					boardSquares[t.r][t.c+i].setText(
							t.word.word.charAt(i) +
							(t.word.blankmask.charAt(i) == 'B' ? "*" : "")
							);
				}
			}
			else { // DOWN
				for(int i=0; i<t.word.length(); i++) {
					tilesPlaced.add(new SquareStatus(boardSquares[t.r+i][t.c].getText(), t.r+i, t.c));
					boardSquares[t.r+i][t.c].setText(
							t.word.word.charAt(i) +
							(t.word.blankmask.charAt(i) == 'B' ? "*" : "")
							);
				}
			}
			
			BoardMove b = new BoardMove(t, tilesPlaced);
			// Dont add to undo stack if this is the same boardmove as the last one
			if(!(undoList.size() > 0 && undoList.peek().equals(b))) {
				undoList.push(b);
				if(undoList.size() > MAX_UNDO_CT) {
					undoList.remove(undoList.size()-1);
				}
				undoButton.setEnabled(true);
			}
		}
	}
	
	private void undoButtonPressed() {
		BoardMove b = undoList.pop();
		for(SquareStatus ss : b.tilesPlaced) {
			boardSquares[ss.r][ss.c].setText(ss.s);
		}
		if(undoList.size() == 0) {
			undoButton.setEnabled(false);
		}
	}
	
	private void setGameplayEnabled(boolean state) {
		queryButton.setEnabled(state);
		placeWordButton.setEnabled(state);
		opponentWordPlace.setEnabled(state);
		opponentWordFind.setEnabled(state);
		letterTrayField.setFocusable(state);
		saveGameItem.setEnabled(state);
		deleteGameItem.setEnabled(state);
		gameTypeMenu.setEnabled(state);
		for(int i=0; i<BOARD_SIZE; i++) {
			for(int j=0; j<BOARD_SIZE; j++) {
				boardSquares[i][j].setFocusable(state);
			}
		}
		if(state) {
			pointDispLabel.setText("Enter letters, press find, and choose a result to the right!");
		}
		if(!state) {
			pointDispLabel.setText("Select a game from the above menu or create a new game.");
			clearBoard();
			queryDataList.setListData(new String[] {""});
			queryResultCountLabel.setText("Your Results");
			opponentResultCountLabel.setText("Friend's Results");
			queryTimeLabel.setText("Waiting for search.");
		}
	}
	
	private void clearBoard() {
		letterTrayField.setText("");
		for(int i=0; i<BOARD_SIZE; i++) {
			for(int j=0; j<BOARD_SIZE; j++) {
				boardSquares[i][j].setText("");
			}
		}
	}
	
	private boolean askForSave(String popupTitle, int optionPaneType) {
		int response = JOptionPane.showConfirmDialog(
			    new JFrame(),
			    "Do you want to save your current game?",
			    popupTitle,
			    optionPaneType);
		if(response == JOptionPane.YES_OPTION) {
			// Save game
			GameSaver.saveBoard(boardSquares, currentGame, letterTrayField.getText());
			changeSinceLastSave = false;
		}
		if(response == JOptionPane.CANCEL_OPTION) {
			return false;
		}
		return true;
	}
	
	private void updateComboBox() {
		gameSelectBox.removeAllItems();
		ArrayList<GameFile> games = GameSaver.getSavedFiles();
		gameSelectBox.addItem("Select a Game");
		if(games.size() > 0) {
			for(GameFile gf : games) {
				gameSelectBox.addItem(gf);
			}
		}
	}

}