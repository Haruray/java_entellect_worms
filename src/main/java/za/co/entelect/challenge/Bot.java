package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.CellType;
import za.co.entelect.challenge.enums.Direction;

import java.util.*;
import java.util.stream.Collectors;

public class Bot {

    private Random random;
    private GameState gameState;
    private Opponent opponent;
    private MyWorm currentWorm;

    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.opponent = gameState.opponents[0];
        this.currentWorm = getCurrentWorm(gameState);
    }

    private MyWorm getCurrentWorm(GameState gameState) {
        return Arrays.stream(gameState.myPlayer.worms)
                .filter(myWorm -> myWorm.id == gameState.currentWormId)
                .findFirst()
                .get();
    }

    private boolean canBananaBombThem(Worm target){
        //Check if currentWorm can cast bananabomb
        if (currentWorm.id!=2){
            return false;
        }
        return (currentWorm.bananaBombs.count>0 && euclideanDistance(target.position.x, target.position.y, currentWorm.position.x, currentWorm.position.y) <= currentWorm.bananaBombs.range && euclideanDistance(target.position.x, target.position.y, currentWorm.position.x, currentWorm.position.y) > currentWorm.bananaBombs.damageRadius * 0.75);
    }

    private boolean canSnowballThem(Worm target){
        //Check if currentworm can cast snowball
        if (currentWorm.id!=3){
            return false;
        }
        return (currentWorm.snowballs.count>0 && euclideanDistance(target.position.x, target.position.y, currentWorm.position.x, currentWorm.position.y) <= currentWorm.snowballs.range && euclideanDistance(target.position.x, target.position.y, currentWorm.position.x, currentWorm.position.y) > currentWorm.snowballs.freezeRadius * Math.sqrt(2) && target.roundsUntilUnfrozen<=0);
    }

    public Command run() {
        //Mencari worm musuh terdekat
        Worm enemyWorm = getNearestWorm();
        Worm secondWormInBBRadius = getNearestWormInRadius(enemyWorm, currentWorm.bananaBombs.damageRadius);
        Worm secondWormInSBRadius = getNearestWormInRadius(enemyWorm, currentWorm.snowballs.freezeRadius);

        if (canBananaBombThem(enemyWorm) && secondWormInBBRadius != null){  //Jika bisa di bananabomb, maka langsung dibananabomb
            return new BananaBombCommand(enemyWorm.position.x, enemyWorm.position.y);
        }
        if (canSnowballThem(enemyWorm) && secondWormInSBRadius != null){ //Jika bisa di snowball, maka langsung di snowbomb
            return new SnowballCommand(enemyWorm.position.x, enemyWorm.position.y);
        }
        Worm enemyWormDefault = getFirstWormInRange(); //Ini mendetect worm musuh pakai fungsi bawaan, biar menghindari weird error
        if (enemyWormDefault != null){ //Kalau ada musuh disekitar, maka bisa ditembak. Berlaku ke semua jenis worm/this is the default command
            Direction direction = resolveDirection(currentWorm.position, enemyWormDefault.position);
            return new ShootCommand(direction);
        }
        //Me list surrounding block, terus mencari block mana yg pathnya paling pendek
        List<Cell> surroundingBlocks = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);
        Cell block = findNextCellInPath(surroundingBlocks, enemyWorm.position.x, enemyWorm.position.y);
        //Kalau ada dirt, dig. Kalau ngga, langsung jalan
        if (block.type == CellType.AIR) {
            return new MoveCommand(block.x, block.y);
        } else if (block.type == CellType.DIRT) {
            return new DigCommand(block.x, block.y);
        }
        //Default
        return new DoNothingCommand();

    }

    private Cell findNextCellInPath(List<Cell> surroundingBlocks,int x,int y){
        //Parameter x dan y adalah koordinat target
        List<Integer> pathDistance = new ArrayList<>();
        //dari semua surrounding block, dicari yg jaraknya paling dekat
        pathDistance = surroundingBlocks.stream().map(c -> euclideanDistance(c.x, c.y,x,y)).collect(Collectors.toList());
        //Return yg jaraknya paling dekat
        return surroundingBlocks.get(pathDistance.indexOf(Collections.min(pathDistance)));
    }

    private Worm getFirstWormInRange() {
        Set<String> cells = constructFireDirectionLines(currentWorm.weapon.range)
                .stream()
                .flatMap(Collection::stream)
                .map(cell -> String.format("%d_%d", cell.x, cell.y))
                .collect(Collectors.toSet());

        for (Worm enemyWorm : opponent.worms) {
            String enemyPosition = String.format("%d_%d", enemyWorm.position.x, enemyWorm.position.y);
            if (cells.contains(enemyPosition) && enemyWorm.health>0) { //kalau musuh masih hidup
                return enemyWorm;
            }
        }

        return null;
    }

    private Worm getNearestWormInRadius(Worm w, int radius) {
        //get nearest worm
        List<Integer> wormsRange = new ArrayList<>(); //Jarak worm musuh dengan current worm
        List<Worm> enemyWorms = new ArrayList<>(); //List worm musuh
        for (Worm enemyWorm : opponent.worms) {
            if (enemyWorm.health > 0 && enemyWorm.id != w.id && euclideanDistance(w.position.x, w.position.y, enemyWorm.position.x, enemyWorm.position.y) <= radius) { 
                enemyWorms.add(enemyWorm);
                wormsRange.add(euclideanDistance(w.position.x, w.position.y, enemyWorm.position.x, enemyWorm.position.y));
                
            }
        }
        if (!enemyWorms.isEmpty()) {
            return enemyWorms.get(wormsRange.indexOf(Collections.min(wormsRange)));
        } else {
            return null;
        }

    private List<Worm> getWormsInRange(){
        //JANGAN DIPAHAMI, INI BELUM BISA DIPAKAI
        List<Worm> nearbyWorms = new ArrayList<>();
        int range;
        if (currentWorm.id==2){
            range=currentWorm.bananaBombs.range;
        }
        else if (currentWorm.id==3){
            range=currentWorm.snowballs.range;
        }
        else{
            range=currentWorm.weapon.range;
        }
        Set<String> cells = constructFireDirectionLines(range)
                .stream()
                .flatMap(Collection::stream)
                .map(cell -> String.format("%d_%d", cell.x, cell.y))
                .collect(Collectors.toSet());

        for (Worm enemyWorm : opponent.worms) {
            String enemyPosition = String.format("%d_%d", enemyWorm.position.x, enemyWorm.position.y);
            if (cells.contains(enemyPosition)) {
                nearbyWorms.add(enemyWorm);
            }
        }
        if (nearbyWorms!=null){
            return nearbyWorms;
        }
        return null;
    }

    private Worm getNearestWorm(){
        //get nearest worm
        List<Integer> wormsRange = new ArrayList<>(); //Jarak worm musuh dengan current worm
        List<Worm> enemyWorms = new ArrayList<>(); //List worm musuh
        for (Worm enemyWorm : opponent.worms){
            if (enemyWorm.health>0) { //Syaratnya worm musuh harus hidup
                enemyWorms.add(enemyWorm);
                wormsRange.add(euclideanDistance(currentWorm.position.x, currentWorm.position.y, enemyWorm.position.x, enemyWorm.position.y));
            }
        }
        return enemyWorms.get(wormsRange.indexOf(Collections.min(wormsRange)));
    }

    private List<List<Cell>> constructFireDirectionLines(int range) {
        List<List<Cell>> directionLines = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            List<Cell> directionLine = new ArrayList<>();
            for (int directionMultiplier = 1; directionMultiplier <= range; directionMultiplier++) {

                int coordinateX = currentWorm.position.x + (directionMultiplier * direction.x);
                int coordinateY = currentWorm.position.y + (directionMultiplier * direction.y);

                if (!isValidCoordinate(coordinateX, coordinateY)) {
                    break;
                }

                if (euclideanDistance(currentWorm.position.x, currentWorm.position.y, coordinateX, coordinateY) > range) {
                    break;
                }

                Cell cell = gameState.map[coordinateY][coordinateX];
                if (cell.type != CellType.AIR) {
                    break;
                }

                directionLine.add(cell);
            }
            directionLines.add(directionLine);
        }

        return directionLines;
    }

    private List<Cell> getSurroundingCells(int x, int y) {
        ArrayList<Cell> cells = new ArrayList<>();
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                // Don't include the current position
                if (i != x && j != y && isValidCoordinate(i, j)) {
                    cells.add(gameState.map[j][i]);
                }
            }
        }

        return cells;
    }

    private int euclideanDistance(int aX, int aY, int bX, int bY) {
        return (int) (Math.sqrt(Math.pow(aX - bX, 2) + Math.pow(aY - bY, 2)));
    }

    private boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x < gameState.mapSize
                && y >= 0 && y < gameState.mapSize;
    }

    private Direction resolveDirection(Position a, Position b) {
        StringBuilder builder = new StringBuilder();

        int verticalComponent = b.y - a.y;
        int horizontalComponent = b.x - a.x;

        if (verticalComponent < 0) {
            builder.append('N');
        } else if (verticalComponent > 0) {
            builder.append('S');
        }

        if (horizontalComponent < 0) {
            builder.append('W');
        } else if (horizontalComponent > 0) {
            builder.append('E');
        }

        return Direction.valueOf(builder.toString());
    }
}
