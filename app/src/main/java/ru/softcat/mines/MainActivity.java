package ru.softcat.mines;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.widget.GridLayout.LayoutParams;
import android.util.Size;
import android.graphics.*;
import java.util.Random;
import java.util.function.Consumer;
import java.util.List;
import java.util.ArrayList;
import android.view.View.*;
import android.content.res.*;

public class MainActivity
	extends Activity
	implements OnClickListener, OnLongClickListener
{
	private enum CellType {
		FREE,
		MINE,
		OPENED
	}
	
	private enum GameEndType {
		WIN,
		LOSE
	}
	
	final int GRID_SIZE = 10;
	final int MINES_COUNT = 20;
	
	private GridLayout grid;
	private TextView messageText;
	
	private int maxIndex;
	
	private CellType[] field;
	
	private boolean isPlaying;
	
	private int cellsLeft;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(getResources().getString(R.string.new_text));
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		resetGame();
		return super.onOptionsItemSelected(item);
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        setContentView(R.layout.main);
		
		grid = findViewById(R.id.mainGrid);
		grid.setRowCount(GRID_SIZE);
		grid.setColumnCount(GRID_SIZE);
		
		messageText = findViewById(R.id.message);
		
		resetGame();
    }
	
	private void resetGame() {
		isPlaying = true;
		cellsLeft = GRID_SIZE * GRID_SIZE;
		
		initializeField();
		createLayoutButtons();
		displayHowMuchLeft();
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
		
		boolean isHit = field[vw.getId()] == CellType.MINE;
		
		if(isHit) {
			setGameOverState(GameEndType.LOSE);
		} else {
			openCell(vw.getId());
			displayHowMuchLeft();
			
			if(cellsLeft <= MINES_COUNT) {
				setGameOverState(GameEndType.WIN);
			}
		}
	}
	
	private void setGameOverState(GameEndType type) {
		isPlaying = false;
		String message;
		
		Resources resources = getResources();
		
		if(type == GameEndType.WIN) {
			message = resources.getString(R.string.win_msg);
		} else {
			message = resources.getString(R.string.lose_msg);
			revealAllMines();
		}
		
		disableAllCellButtons();
		
		messageText.setText(message);
	}
	
	private void disableAllCellButtons() {
		for(int i = 0; i < field.length; i++) {
			disableCellButton(getButtonFromId(i));
		}
	}

	private void disableCellButton(Button btn) {
		btn.setClickable(false);
		btn.setLongClickable(false);
	}
	
	private void revealAllMines() {
		for(int i = 0; i < field.length; i++) {
			if(field[i] == CellType.MINE) {
				Button btn = getButtonFromId(i);
				btn.setText("*");
				btn.setTag(R.id.tag_marked, null);
				applyCellTint(btn);
			}
		}
	}

	@Override
	public boolean onLongClick(View vw) {
		Button btn = (Button)vw;
		
		if(btn.getTag(R.id.tag_marked) != null) {
			btn.setTag(R.id.tag_marked, null);
		} else {
			btn.setTag(R.id.tag_marked, true);
		}
		
		applyCellTint(btn);
		
		return true;
	}
	
	private void applyCellTint(Button btn) {
		int id = btn.getId();
		CellType type = field[id];
		int cellColor = 0;
		
		if(btn.getTag(R.id.tag_marked) != null) {
			cellColor = Color.GREEN;
		} else if(type == CellType.MINE) {
			cellColor = Color.RED;
		} else if(type == CellType.OPENED) {
			cellColor = Color.YELLOW;
		} else {
			btn.getBackground().clearColorFilter();
			return;
		}
		
		btn.getBackground().setColorFilter(cellColor, PorterDuff.Mode.MULTIPLY);
	}
	
	private void openCell(int id) {
		if(id < 0 || id >= maxIndex || field[id] != CellType.FREE) {
			return;
		}
		
		field[id] = CellType.OPENED;
		cellsLeft--;
		
		Button btn = getButtonFromId(id);
		disableCellButton(btn);
		applyCellTint(btn);
		
		int numMines = countAdjacentMines(id);
		if(numMines > 0) {
			btn.setText(String.valueOf(numMines));
		} else {
			forAdjacent(id, new Consumer<Integer>() {
				@Override
				public void accept(Integer id) {
					openCell(id);
				}
			});
		}
	}
	
	private Button getButtonFromId(int id) {
		return (Button)((FrameLayout)grid.getChildAt(id)).getChildAt(0);
	}
	
	private int countAdjacentMines(int id) {
		final List<Integer> adjacentIds = new ArrayList<>();
		
		forAdjacent(id, new Consumer<Integer>() {
			@Override
			public void accept(Integer testId) {
				if(field[testId] == CellType.MINE) {
					adjacentIds.add(1);
				}
			}
		});
		
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
	
	private void displayHowMuchLeft() {
		String leftFmt = getResources().getString(R.string.left_text);
		messageText.setText(String.format(leftFmt, cellsLeft)); 
	}
	
	public void exitClicked(View v) {
		finishAndRemoveTask();
	}
}
