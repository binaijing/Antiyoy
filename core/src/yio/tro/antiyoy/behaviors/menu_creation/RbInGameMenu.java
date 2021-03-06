package yio.tro.antiyoy.behaviors.menu_creation;

import yio.tro.antiyoy.menu.ButtonYio;
import yio.tro.antiyoy.behaviors.ReactBehavior;

/**
 * Created by ivan on 06.08.14.
 */
public class RbInGameMenu extends ReactBehavior {

    @Override
    public void reactAction(ButtonYio buttonYio) {
        getGameController(buttonYio).selectionController.deselectAll(); // fix to prevent flickering of selection
        buttonYio.menuControllerYio.createInGameMenu();
        getYioGdxGame(buttonYio).setGamePaused(true);
    }
}
