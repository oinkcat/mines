package ru.softcat.mines;

import java.util.*;
import java.util.function.Consumer;

/** Minesweeper game internal logic */
public class GameLogic
{
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
	final int MINES_COUNT = 20;

	private int maxIndex;

	private CellType[] field;

	private boolean isPlaying;

	private int cellsLeft;
	
	private MinesGameListener listener;
	
	public int getFieldSize() {
		return GRID_SIZE;
	}
	
	public int getCellsLeft() {
		return cellsLeft;
	}
	
	public boolean isPlayable() {
		return isPlaying;
	}
	
	public void initialize(MinesGameListener listener) {
		this.listener = listener;
		
		isPlaying = true;
		cellsLeft = GRID_SIZE * GRID_SIZE;
		
		initializeField();
	}
	
	private void initializeField() {
		maxIndex = GRID_SIZE * GRID_SIZE;
		field = new CellType[maxIndex];
		for(int i = 0; i < maxIndex; i++) {
			field[i] = CellType.FREE;
		}

		Random rng = new Random();

		for(int i = 0; i < MINES_COUNT; i++) {
			int idx = -1;
			do {
				idx = rng.nextInt(maxIndex);
			} while(field[idx] == CellType.MINE);
			field[idx] = CellType.MINE;
		}
	}
	
	public void openCell(int id) {
		if(!isPlaying) { return; }
		
		boolean isHit = field[id] == CellType.MINE;

		if(isHit) {
			listener.OnLoseGame(getAllMineCellIds());
			setGameOverState();
		} else {
			ArrayList<CellInfo> allOpen = new ArrayList<>();
			tryOpenAdjacentCells(id, allOpen);
			listener.OnCellsOpened(allOpen);

			if(cellsLeft <= MINES_COUNT) {
				setGameOverState();
				listener.OnWinGame();
			}
		}
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
