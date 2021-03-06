package yio.tro.antiyoy.ai;

import yio.tro.antiyoy.*;
import yio.tro.antiyoy.gameplay.GameController;
import yio.tro.antiyoy.gameplay.Hex;
import yio.tro.antiyoy.gameplay.Province;
import yio.tro.antiyoy.gameplay.Unit;
import yio.tro.antiyoy.gameplay.rules.GameRules;

import java.util.ArrayList;

public class AiExpertGenericRules extends ArtificialIntelligenceGeneric {

    private final Hex tempHex;


    public AiExpertGenericRules(GameController gameController, int color) {
        super(gameController, color);
        tempHex = new Hex(0, 0, new PointYio(), gameController.fieldController);
    }


    @Override
    public void makeMove() {
        updateUnitsReadyToMove();

        moveUnits();

        spendMoneyAndMergeUnits();

        moveAfkUnits();
    }


    @Override
    void decideAboutUnit(Unit unit, ArrayList<Hex> moveZone, Province province) {
        // cleaning palms has highest priority
        if (unit.strength <= 2 && checkToCleanSomePalms(unit, moveZone, province)) return;

        ArrayList<Hex> attackableHexes = findAttackableHexes(unit.getColor(), moveZone);
        if (attackableHexes.size() > 0) { // attack something
            tryToAttackSomething(unit, province, attackableHexes);
        } else { // nothing to attack
            boolean cleanedTrees = checkToCleanSomeTrees(unit, moveZone, province);
            if (!cleanedTrees) {
                if (unit.currentHex.isInPerimeter()) {
                    pushUnitToBetterDefense(unit, province);
                }
            }
        }
    }


    protected boolean isHexDefendedBySomethingElse(Hex hex, Unit unit) {
        for (int i = 0; i < 6; i++) {
            Hex adjHex = hex.getAdjacentHex(i);
            if (adjHex.active && adjHex.sameColor(hex)) {
                if (adjHex.containsBuilding()) return true;
                if (adjHex.containsUnit() && adjHex.unit != unit) return true;
            }
        }
        return false;
    }


    protected boolean unitCanMoveSafely(Unit unit) {
        int leftBehindNumber = 0;
        for (int i = 0; i < 6; i++) {
            Hex adjHex = unit.currentHex.getAdjacentHex(i);
            if (adjHex.active && adjHex.sameColor(unit.currentHex) && !isHexDefendedBySomethingElse(adjHex, unit) && adjHex.isInPerimeter())
                leftBehindNumber++;
        }
        return leftBehindNumber <= 3;
    }


    protected void tryToAttackSomething(Unit unit, Province province, ArrayList<Hex> attackableHexes) {
        if (!unitCanMoveSafely(unit)) return;
        Hex mostAttackableHex = findMostAttractiveHex(attackableHexes, province, unit.strength);
        gameController.moveUnit(unit, mostAttackableHex, province);
    }


    boolean hexHasFriendlyBuildingNearby(Hex hex) {
        for (int i = 0; i < 6; i++) {
            Hex adjHex = hex.getAdjacentHex(i);
            if (adjHex.active && adjHex.sameColor(hex) && adjHex.containsBuilding()) return true;
        }
        return false;
    }


    @Override
    Hex findMostAttractiveHex(ArrayList<Hex> attackableHexes, Province province, int strength) {
        if (strength == 3 || strength == 4) {
            Hex hex = findHexAttractiveToBaron(attackableHexes, strength);
            if (hex != null) return hex;
        }

        // special fix for expert difficulty
//        if (province.hexList.size() < 20) {
//            for (Hex attackableHex : attackableHexes) {
//                if (hexHasFriendlyBuildingNearby(attackableHex)) {
//                    return attackableHex;
//                }
//            }
//        }

        Hex result = null;
        int currMax = -1;
        for (Hex attackableHex : attackableHexes) {
            int currNum = getAttackAllure(attackableHex, province.getColor());
            if (currNum > currMax) {
                currMax = currNum;
                result = attackableHex;
            }
        }
        return result;
    }


    private Hex findRandomHexInPerimeter(Province province) {
        ArrayList<Hex> hexesInPerimeter = new ArrayList<Hex>();
        for (Hex hex : province.hexList) {
            if (hex.isInPerimeter()) hexesInPerimeter.add(hex);
        }
        if (hexesInPerimeter.size() == 0) return null;
        return hexesInPerimeter.get(random.nextInt(hexesInPerimeter.size()));
    }


    @Override
    void moveAfkUnit(Province province, Unit unit) {
        Hex hexToMove = findRandomHexInPerimeter(province);
        if (hexToMove == null) return;
        tempHex.set(unit.currentHex);
        unit.marchToHex(hexToMove, province);
        if (tempHex.equals(unit.currentHex)) super.moveAfkUnit(province, unit); // to prevent infinite loop
    }


    private boolean provinceHasEnoughIncomeForUnit(Province province, int strength) {
//        int newIncome = province.getIncome() - province.getTaxes() - Unit.getTax(strength);
//        if (newIncome >= -2) return true;
//        return false;
        return province.hasEnoughIncomeToAffordUnit(strength);
    }


    @Override
    void tryToBuildUnits(Province province) {
        tryToBuildUnitsOnPalms(province);

        for (int i = 1; i <= 4; i++) {
            if (!provinceHasEnoughIncomeForUnit(province, i)) break;
            boolean successfullyAttacked = false;
            if (province.canBuildUnit(i)) {
                successfullyAttacked = tryToAttackWithStrength(province, i);
            }
            if (successfullyAttacked) i = 0;
        }

        // this is to kick start province
        if (province.canBuildUnit(1) && howManyUnitsInProvince(province) <= 1)
            tryToAttackWithStrength(province, 1);
    }


    @Override
    void tryToBuildTowers(Province province) {
        // try to build normal towers
        while (province.hasMoneyForTower()) {
            Hex hex = findHexThatNeedsTower(province);
            if (hex == null) break;

            gameController.fieldController.buildTower(province, hex);
        }

        // try to build strong towers
        while (provinceCanAffordStrongTower(province)) {
            Hex hex = findHexForStrongTower(province);
            if (hex == null) break;

            gameController.fieldController.buildStrongTower(province, hex);
        }
    }


    protected Hex findHexForStrongTower(Province province) {
        for (Hex hex : province.hexList) {
            if (hex.objectInside != Hex.OBJECT_TOWER) continue;

            if (needsStrongTowerOnHex(province, hex)) {
                return hex;
            }
        }

        return null;
    }


    protected boolean needsStrongTowerOnHex(Province province, Hex hex) {
        updateNearbyProvinces(hex);

        if (nearbyProvinces.size() == 0) return false;

        for (Province nearbyProvince : nearbyProvinces) {
            if (nearbyProvince.hexList.size() > province.hexList.size() / 2) {
                return true;
            }
        }

        return false;
    }


    protected boolean provinceCanAffordStrongTower(Province province) {
        if (!province.hasMoneyForStrongTower()) return false;
        if (province.getIncome() - GameRules.TAX_STRONG_TOWER < GameRules.PRICE_UNIT / 2) return false;

        return true;
    }


    @Override
    boolean needTowerOnHex(Hex hex) {
        if (!hex.active) return false;
        if (!hex.isFree()) return false;

        return getPredictedDefenseGainByNewTower(hex) >= 4;
    }
}
