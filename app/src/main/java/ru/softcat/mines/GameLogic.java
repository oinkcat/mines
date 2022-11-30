package ru.softcat.mines;

import java.util.*;
import java.util.function.Consumer;

/** Minesweeper game internal logic */
public class GameLogic
{
	public enum GameDifficulty {
		EASY,
		HARD
	}
	
	public class CellInfo {
		private int id;
		
		private int numAdjacent;
		
		public int getId() {
			return id;
		}
		
		public int getNumAdjacent() {
			return numAdjacent;
		}
		
		public CellInfo(int id, int numAdjacent) {
			this.id = id;
			this.numAdjacent = numAdjacent;
		}
	}
	
	private enum CellType {
		FREE,
		MINE,
		OPENED
	}

	final int GRID_SIZE = 10;
	
	final int MINES_COUNT_EASY = 10;
	final int MINES_COUNT_HARD = 20;

	private int maxIndex;

	private CellType[] field;

	private boolean isPlaying;
	
	private boolean firstCellOpen;

	private int cellsLeft;
	
	private int minesCount;
	
	private MinesGameListener listener;
	
	public int getFieldSize() {
		return GRID_SIZE;
	}
	
	public int getMinesCount() {
		return minesCount;
	}
	
	public int getCellsLeft() {
		return cellsLeft;
	}
	
	public boolean isPlayable() {
		return isPlaying;
	}
	
	public void initialize(MinesGameListener listener, GameDifficulty diff) {
		this.listener = listener;
		
		isPlaying = true;
		cellsLeft = GRID_SIZE * GRID_SIZE;
		firstCellOpen = false;
		
		minesCount = (diff == GameDifficulty.EASY)
			? MINES_COUNT_EASY
			: MINES_COUNT_HARD;
		
		initializeField();
	}
	
	private void initializeField() {
		maxIndex = GRID_SIZE * GRID_SIZE;
		field = new CellType[maxIndex];
		for(int i = 0; i < maxIndex; i++) {
			field[i] = CellType.FREE;
		}

		for(int i = 0; i < minesCount; i++) {
			int idx = findCellForMine();
			field[idx] = CellType.MINE;
		}
	}
	
	private int findCellForMine() {
		Random rng = new Random();
		
		int idx = -1;
		do {
			idx = rng.nextInt(maxIndex);
		} while(field[idx] == CellType.MINE);
		
		return idx;
	}
	
	public void openCell(int id) {
		if(!isPlaying) { return; }
		
		boolean isHit = field[id] == CellType.MINE;
		
		if(isHit && !firstCellOpen) {
			moveMineAway(id);
			isHit = false;
		}
		firstCellOpen = true;

		if(isHit) {
			listener.OnLoseGame(getAllMineCellIds());
			setGameOverState();
		} else {
			ArrayList<CellInfo> allOpen = new ArrayList<>();
			tryOpenAdjacentCells(id, allOpen);
			listener.OnCellsOpened(allOpen);

			if(cellsLeft <= minesCount) {
				setGameOverState();
				listener.OnWinGame();
			}
		}
	}
	
	private void moveMineAway(int prevCellId) {
		int newCellId = prevCellId;
		
		while(newCellId == prevCellId) {
			newCellId = findCellForMine();
		}
		
		field[prevCellId] = CellType.FREE;
		field[newCellId] = CellType.MINE;
	}
	
	private ArrayList<Integer> getAllMineCellIds() {
		ArrayList<Integer> mineIds = new ArrayList<>();
		
		for(int i = 0; i < field.length; i++) {
			if(field[i] == CellType.MINE) {
				mineIds.add(i);
			}
		}
		
		return mineIds;
	}
	
	private void setGameOverState() {
		isPlaying = false;
	}

	private void tryOpenAdjacentCells(int id, ArrayList<CellInfo> allOpen) {
		if(id < 0 || id >= maxIndex || field[id] != CellType.FREE) {
			return;
		}

		field[id] = CellType.OPENED;
		cellsLeft--;

		int numMines = countAdjacentMines(id);
		allOpen.add(new CellInfo(id, numMines));
		
		if(numMines == 0) {
			final ArrayList<CellInfo> openAdjacent = allOpen;
			forAdjacent(id, new Consumer<Integer>() {
				@Override
				public void accept(Integer id) {
					tryOpenAdjacentCells(id, openAdjacent);
				}
			});
		}
	}
	
	private int countAdjacentMines(int id) {
		final List<Integer> adjacentIds = new ArrayList<>();
		
		forAdjacent(id, new Consumer<Integer>() {
			@Override
			public void accept(Integer testId) {
				if(field[testId] == CellType.MINE) {
					adjacentIds.add(1);
				}
			}});
			
		return adjacentIds.size();
	}

	private void forAdjacent(int id, Consumer<Integer> action) {
		int row = id / GRID_SIZE;
		int col = id % GRID_SIZE;

		for(int rd = -1; rd <= 1; rd++) {
			if(row + rd < 0 || row + rd >= GRID_SIZE) {
				continue;
			}
			for(int cd = -1; cd <= 1; cd++) {	
				if(col + cd >= 0 && col + cd < GRID_SIZE) {
					int testId = id + GRID_SIZE * rd + cd;	
					action.accept(testId);
				}
			}
		}
	}
}
