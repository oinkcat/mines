package ru.softcat.mines;
import java.util.*;

/** Minesweeper game event callbacks */
public interface MinesGameListener
{
	void OnCellsOpened(ArrayList<GameLogic.CellInfo> openCells);
	
	void OnWinGame();
	
	void OnLoseGame(ArrayList<Integer> mineCellIds);
}
