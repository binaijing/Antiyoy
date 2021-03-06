package yio.tro.antiyoy.behaviors.gameplay;

import yio.tro.antiyoy.gameplay.RefuseStatistics;
import yio.tro.antiyoy.menu.ButtonYio;
import yio.tro.antiyoy.behaviors.ReactBehavior;

/**
 * Created by ivan on 06.12.2015.
 */
public class RbWinGame extends ReactBehavior {

    @Override
    public void reactAction(ButtonYio buttonYio) {
        getGameController(buttonYio).forceGameEnd();
        RefuseStatistics.getInstance().onEarlyGameEndAccept();
    }
}
