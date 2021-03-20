package ru.softcat.mines;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.widget.GridLayout.LayoutParams;
import android.util.Size;
import android.graphics.*;
import java.util.List;
import java.util.ArrayList;
import android.view.View.*;
import android.content.res.*;

/** Game UI and interaction */
public class MainActivity
	extends Activity
	implements OnClickListener, OnLongClickListener, MinesGameListener
{
	private enum CellUiState {
		DEFAULT,
		MARKED,
		OPEN,
		EXPLODED
	}
	
	private GridLayout grid;
	private TextView messageText;
	
	private GameLogic game;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(getResources().getString(R.string.exit_text));
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		this.finishAndRemoveTask();
		return super.onOptionsItemSelected(item);
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
		game = new GameLogic();
		
		super.onCreate(savedInstanceState);
		
        setContentView(R.layout.main);
		
		grid = findViewById(R.id.mainGrid);
		grid.setRowCount(game.getFieldSize());
		grid.setColumnCount(game.getFieldSize());
		
		messageText = findViewById(R.id.message);
		
		resetGame();
    }
	
	private void resetGame() {
		game.initialize(this);
		
		createLayoutButtons();
		displayHowMuchLeft();
	}

	public void OnCellsOpened(ArrayList<GameLogic.CellInfo> openCells) {
		for(GameLogic.CellInfo info: openCells) {
			if(info.getNumAdjacent() > 0) {
				Button btn = getButtonFromId(info.getId());
				btn.setText(Integer.toString(info.getNumAdjacent()));
			}
			applyCellTint(info.getId(), CellUiState.OPEN);
		}
		
		displayHowMuchLeft();
	}

	public void OnWinGame() {
		setGameOverUiState(true);
	}

	public void OnLoseGame(ArrayList<Integer> mineCellIds) {
		for(int cellId : mineCellIds) {
			Button btn = getButtonFromId(cellId);
			btn.setText("*");
			applyCellTint(cellId, CellUiState.EXPLODED);
		}
		setGameOverUiState(false);
	}
	
	private void createLayoutButtons() {
		grid.removeViews(0, grid.getChildCount());
		grid.setClipChildren(false);
		
		int totalBtns = grid.getRowCount() * grid.getColumnCount();
		for(int i = 0; i < totalBtns; i++) {
			grid.addView(createCell(i));
		}
	}
	
	private View createCell(int id) {
		FrameLayout parent = new FrameLayout(this);
		GridLayout.LayoutParams params = new LayoutParams();
		params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
		params.width = 0;
		parent.setLayoutParams(params);
		
		Button cellBtn = new Button(this);
		cellBtn.setId(id);
		cellBtn.setOnClickListener(this);
		cellBtn.setLongClickable(true);
		cellBtn.setOnLongClickListener(this);
		parent.addView(cellBtn);
		
		return parent;
	}

	@Override
	public void onClick(View vw) {
		Button btn = (Button)vw;
		btn.setTag(R.id.tag_marked, null);
		
		game.openCell(btn.getId());
	}
	
	private void setGameOverUiState(boolean isaWin) {
		int msgResId = isaWin ? R.string.win_msg : R.string.lose_msg;
		String message = getResources().getString(msgResId);
		messageText.setText(message);
		
		disableAllCellButtons();
	}
	
	private void disableAllCellButtons() {
		int numCells = game.getFieldSize() * game.getFieldSize();
		
		for(int i = 0; i < numCells; i++) {
			disableCellButton(getButtonFromId(i));
		}
	}

	private void disableCellButton(Button btn) {
		btn.setClickable(false);
		btn.setLongClickable(false);
	}

	@Override
	public boolean onLongClick(View vw) {
		Button btn = (Button)vw;
		
		boolean prevMarked = btn.getTag(R.id.tag_marked) != null;
		if(prevMarked) {
			btn.setTag(R.id.tag_marked, null);
		} else {
			btn.setTag(R.id.tag_marked, true);
		}
		
		CellUiState newUiState = prevMarked 
			? CellUiState.DEFAULT
			: CellUiState.MARKED;
		applyCellTint(btn.getId(), newUiState);
		
		return true;
	}
	
	private void applyCellTint(int cellId, CellUiState state) {
		Button btn = getButtonFromId(cellId);
		
		int cellColor = 0;
		
		if(state == CellUiState.MARKED) {
			cellColor = Color.GREEN;
		} else if(state == CellUiState.EXPLODED) {
			cellColor = Color.RED;
		} else if(state == CellUiState.OPEN) {
			cellColor = Color.YELLOW;
		} else {
			btn.getBackground().clearColorFilter();
			return;
		}
		
		btn.getBackground().setColorFilter(cellColor, PorterDuff.Mode.MULTIPLY);
	}
	
	private Button getButtonFromId(int id) {
		return (Button)((FrameLayout)grid.getChildAt(id)).getChildAt(0);
	}
	
	private void displayHowMuchLeft() {
		String leftFmt = getResources().getString(R.string.left_text);
		messageText.setText(String.format(leftFmt, game.getCellsLeft()));
	}
	
	public void newGameClicked(View v) {
		resetGame();
	}
}
