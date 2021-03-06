package yio.tro.antiyoy.behaviors.editor;

import yio.tro.antiyoy.menu.ButtonYio;
import yio.tro.antiyoy.behaviors.ReactBehavior;

/**
 * Created by ivan on 03.12.2015.
 */
public class RbClearLevel extends ReactBehavior {

    @Override
    public void reactAction(ButtonYio buttonYio) {
        buttonYio.menuControllerYio.hideEditorConfirmClearLevelMenu();
        getGameController(buttonYio).clearLevel();
    }
}
