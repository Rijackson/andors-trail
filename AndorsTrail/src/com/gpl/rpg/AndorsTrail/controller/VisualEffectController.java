package com.gpl.rpg.AndorsTrail.controller;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.os.Handler;

import com.gpl.rpg.AndorsTrail.VisualEffectCollection;
import com.gpl.rpg.AndorsTrail.VisualEffectCollection.VisualEffect;
import com.gpl.rpg.AndorsTrail.context.ViewContext;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.controller.listeners.VisualEffectFrameListeners;
import com.gpl.rpg.AndorsTrail.model.actor.Monster;
import com.gpl.rpg.AndorsTrail.model.actor.MonsterType;
import com.gpl.rpg.AndorsTrail.model.map.PredefinedMap;
import com.gpl.rpg.AndorsTrail.resource.tiles.TileManager;
import com.gpl.rpg.AndorsTrail.util.Coord;
import com.gpl.rpg.AndorsTrail.util.CoordRect;
import com.gpl.rpg.AndorsTrail.util.Size;

public final class VisualEffectController {
	private int effectCount = 0;

	private final ViewContext view;
	private final WorldContext world;
	private final VisualEffectCollection effectTypes;
	
	public final VisualEffectFrameListeners visualEffectFrameListeners = new VisualEffectFrameListeners();
	
	public VisualEffectController(ViewContext view, WorldContext world) {
		this.view = view;
		this.world = world;
		this.effectTypes = world.visualEffectTypes;
	}

	public void startEffect(Coord position, int effectID, int displayValue, VisualEffectCompletedCallback callback, int callbackValue) {
		++effectCount;
		(new VisualEffectAnimation(effectTypes.effects[effectID], position, displayValue, callback, callbackValue))
		.start();
	}
	
	public final class VisualEffectAnimation extends Handler implements Runnable {

		@Override
		public void run() {
			update();
			if (currentFrame == effect.lastFrame) {
				onCompleted();
			} else {
				postDelayed(this, effect.millisecondPerFrame);
			}
		}
	      
	    private void update() {
        	++currentFrame;
        	int frame = currentFrame;
        	
    		int tileID = effect.frameIconIDs[frame];
			int textYOffset = -2 * (frame);
			if (frame >= beginFadeAtFrame && displayText != null) {
				this.textPaint.setAlpha(255 * (effect.lastFrame - frame) / (effect.lastFrame - beginFadeAtFrame)); 
			}
			area.topLeft.y = position.y - 1;
			visualEffectFrameListeners.onNewAnimationFrame(this, tileID, textYOffset);
		}

		private void onCompleted() {
    		--effectCount;
    		visualEffectFrameListeners.onAnimationCompleted(this);
			if (callback != null) callback.onVisualEffectCompleted(callbackValue);
		}
		
		public void start() {
			postDelayed(this, 0);
		}

		private int currentFrame = 0;
		
		private final VisualEffect effect;
		
		public final Coord position;
		public final String displayText;
		public final CoordRect area;
		public final Paint textPaint = new Paint();
		private final int beginFadeAtFrame;
		private final VisualEffectCompletedCallback callback;
		private final int callbackValue;
		
		public VisualEffectAnimation(VisualEffect effect, Coord position, int displayValue, VisualEffectCompletedCallback callback, int callbackValue) {
			this.position = position;
			this.callback = callback;
			this.callbackValue = callbackValue;
			this.area = new CoordRect(new Coord(position.x, position.y - 1), new Size(1, 2));
			this.effect = effect;
			this.displayText = (displayValue == 0) ? null : String.valueOf(displayValue);
			this.textPaint.setColor(effect.textColor);
			this.textPaint.setShadowLayer(2, 1, 1, Color.DKGRAY);
			this.textPaint.setTextSize(world.tileManager.viewTileSize * 0.5f); // 32dp.
			this.textPaint.setAlpha(255);
			this.textPaint.setTextAlign(Align.CENTER);
			this.beginFadeAtFrame = effect.lastFrame / 2;
		}
	}
	
	public static interface VisualEffectCompletedCallback {
		public void onVisualEffectCompleted(int callbackValue);
	}

	public boolean isRunningVisualEffect() {
		return effectCount > 0;
	}
	

	public static final class BloodSplatter {
		public final long removeAfter;
		public final long reduceIconAfter;
		public final Coord position;
		public int iconID;
		public boolean reducedIcon = false;
		public BloodSplatter(int iconID, Coord position) {
			this.iconID = iconID;
			this.position = position;
			final long now = System.currentTimeMillis();
			removeAfter = now + Constants.SPLATTER_DURATION_MS;
			reduceIconAfter = now + Constants.SPLATTER_DURATION_MS / 2;
		}
	}
	
	public void updateSplatters(PredefinedMap map) {
		long now = System.currentTimeMillis();
		for (int i = map.splatters.size() - 1; i >= 0; --i) {
			BloodSplatter b = map.splatters.get(i);
			if (b.removeAfter <= now) {
				map.splatters.remove(i);
				view.monsterSpawnController.monsterSpawnListeners.onSplatterRemoved(map, b.position);
			} else if (!b.reducedIcon && b.reduceIconAfter <= now) {
				b.reducedIcon = true;
				b.iconID++;
				view.monsterSpawnController.monsterSpawnListeners.onSplatterChanged(map, b.position);
			}
		}
	}
	
	public void addSplatter(PredefinedMap map, Monster m) {
		int iconID = getSplatterIconFromMonsterClass(m.getMonsterClass());
		if (iconID > 0) {
			map.splatters.add(new BloodSplatter(iconID, m.position));
			view.monsterSpawnController.monsterSpawnListeners.onSplatterAdded(map, m.position);
		}
	}
	
	private static int getSplatterIconFromMonsterClass(int monsterClass) {
		switch (monsterClass) {
		case MonsterType.MONSTERCLASS_INSECT: 
		case MonsterType.MONSTERCLASS_UNDEAD: 
		case MonsterType.MONSTERCLASS_REPTILE: 
			return TileManager.iconID_splatter_brown_1a + Constants.rnd.nextInt(2) * 2;
		case MonsterType.MONSTERCLASS_HUMANOID:
		case MonsterType.MONSTERCLASS_ANIMAL:
		case MonsterType.MONSTERCLASS_GIANT:
			return TileManager.iconID_splatter_red_1a + Constants.rnd.nextInt(2) * 2;
		case MonsterType.MONSTERCLASS_DEMON:
		case MonsterType.MONSTERCLASS_CONSTRUCT:
		case MonsterType.MONSTERCLASS_GHOST:
			return TileManager.iconID_splatter_white_1a;
		default:
			return -1;
		}
	}
}
