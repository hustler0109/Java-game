package cubyz.gui;

import static org.lwjgl.opengl.GL43.*;

import java.util.ArrayDeque;
import java.util.ArrayList;

import org.lwjgl.glfw.GLFW;

import cubyz.client.ClientSettings;
import cubyz.gui.input.Keyboard;
import cubyz.gui.input.Mouse;
import cubyz.rendering.Graphics;
import cubyz.rendering.Window;

/**
 * UI system working in the background, to add effects like transition.
 */

public class UISystem {
	
	private MenuGUI gui;
	/** kept only for transition effect */
	private MenuGUI oldGui;
	private final ArrayList<MenuGUI> overlays = new ArrayList<>();
	private final ArrayDeque<MenuGUI> menuQueue = new ArrayDeque<>();
	
	private Transition curTransition;
	private long lastAnimTime = System.currentTimeMillis();
	private float transitionTime;

	public boolean showOverlay = true;

	public UISystem() {}
	
	public void addOverlay(MenuGUI over) {
		over.init();
		synchronized(overlays) {
			overlays.add(over);
		}
	}
	
	public boolean removeOverlay(MenuGUI over) {
		synchronized(overlays) {
			return overlays.remove(over);
		}
	}
	
	public MenuGUI[] getOverlays() {
		synchronized(overlays) {
			return overlays.toArray(new MenuGUI[0]);
		}
	}
	
	public void back() {
		setMenu(menuQueue.pollLast(), false, new Transition.FadeOutIn());
	}
	
	public void setMenu(MenuGUI gui) {
		setMenu(gui, true, new Transition.FadeOutIn());
	}
	
	public void setMenu(MenuGUI gui, boolean addQueue) {
		setMenu(gui, addQueue, new Transition.FadeOutIn());
	}
	
	public void setMenu(MenuGUI gui, Transition style) {
		setMenu(gui, true, style);
	}
	
	public void setMenu(MenuGUI gui, boolean addQueue, Transition style) {
		this.curTransition = style;
		this.transitionTime = 0;
		
		// If the transition is none, we don't have to bother with setting oldGui variable
		if (!(style instanceof Transition.None)) {
			oldGui = this.gui;
		}
		if (this.gui != null && addQueue) {
			menuQueue.add(this.gui);
		}
		if (this.gui != null && this.gui.ungrabsMouse() && (gui == null || !gui.ungrabsMouse())) {
			Mouse.setGrabbed(true);
		}
		if (this.gui != null) {
			this.gui.close();
		}
		if (gui != null) {
			gui.init();
		} else {
			// Delete the queue if null is set as menu.
			// This prevents issues in game.
			menuQueue.clear();
		}
		if (gui != null && gui.ungrabsMouse() && (this.gui == null || !this.gui.ungrabsMouse())) {
			Mouse.setGrabbed(false);
		}
		this.gui = gui;
	}
	
	public MenuGUI getMenuGUI() {
		return gui;
	}
	
	public boolean doesGUIBlockInput() {
		if (gui == null)
			return false;
		else
			return gui.doesPauseGame() || gui.ungrabsMouse();
	}
	
	public boolean doesGUIPauseGame() {
		return gui != null && gui.doesPauseGame();
	}

	public void updateGUIScale() {
		int guiScale = Math.min(Window.getWidth()/480, Window.getHeight()/270);
		guiScale = Math.max(1, guiScale);
		ClientSettings.GUI_SCALE = guiScale;
		if (gui != null)
			gui.updateGUIScale();
		for(MenuGUI gui : overlays) {
			gui.updateGUIScale();
		}
		for(MenuGUI gui : menuQueue) {
			gui.updateGUIScale();
		}
	}

	public void render() {
		if (Keyboard.isKeyPressed(GLFW.GLFW_KEY_F1)) {
			Keyboard.setKeyPressed(GLFW.GLFW_KEY_F1, false);
			showOverlay = !showOverlay;
		}
		if (showOverlay) {
			glDisable(GL_DEPTH_TEST);
			glDisable(GL_CULL_FACE);
			glEnable(GL_BLEND);
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			glActiveTexture(GL_TEXTURE0);
			transitionTime += System.currentTimeMillis() - lastAnimTime;
			lastAnimTime = System.currentTimeMillis();
			Graphics.setGlobalAlphaMultiplier(1);
			Graphics.setColor(0x000000);
			if (curTransition != null) {
				float alpha1 = curTransition.getCurrentGuiOpacity(transitionTime / curTransition.getDuration());
				float alpha2 = curTransition.getOldGuiOpacity(transitionTime / curTransition.getDuration());
				if (transitionTime >= curTransition.duration) {
					curTransition = null;
					oldGui = null;
				}
				if (gui != null) {
					Graphics.setGlobalAlphaMultiplier(alpha1);
					gui.render();
				}
				if (oldGui != null) {
					Graphics.setGlobalAlphaMultiplier(alpha2);
					oldGui.render();
				}
			} else {
				if (gui != null) {
					gui.render();
				}
			}
			Graphics.setGlobalAlphaMultiplier(1f);
			for(MenuGUI overlay : getOverlays()) {
				overlay.render();
			}
		}
		Window.restoreState();
	}

}