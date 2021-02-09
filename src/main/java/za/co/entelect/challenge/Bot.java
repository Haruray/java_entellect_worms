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
//    private ArrayList<Cell> mapCells = new ArrayList<>();

    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.opponent = gameState.opponents[0];
        this.currentWorm = getCurrentWorm(gameState);
//        for (int i=0;i< gameState.mapSize ;i++){
//            for (int j=0;j<gameState.mapSize;j++){
//                if (isValidCoordinate(i, j)){
//                    this.mapCells.add(gameState.map[i][j]);
//                }
//            }
//        }
    }

    private MyWorm getCurrentWorm(GameState gameState) {
        return Arrays.stream(gameState.myPlayer.worms)
                .filter(myWorm -> myWorm.id == gameState.currentWormId)
                .findFirst()
                .get();
    }

    private boolean canBananaBombThem(Worm target){
        //Check if currentWorm can cast bananabomb
        return (currentWorm.bananaBombs.count>0 && euclideanDistance(target.position.x, target.position.y, currentWorm.position.x, currentWorm.position.y) <= currentWorm.bananaBombs.range && euclideanDistance(target.position.x, target.position.y, currentWorm.position.x, currentWorm.position.y) > currentWorm.bananaBombs.damageRadius * 0.75);
    }

    private boolean canSnowballThem(Worm target){
        //Check if currentworm can cast snowball
        return (currentWorm.snowballs.count>0 && euclideanDistance(target.position.x, target.position.y, currentWorm.position.x, currentWorm.position.y) <= currentWorm.snowballs.range && euclideanDistance(target.position.x, target.position.y, currentWorm.position.x, currentWorm.position.y) > currentWorm.snowballs.freezeRadius * Math.sqrt(2) && target.roundsUntilUnfrozen<=0);
    }

    public Command run() {
        Worm enemyWorm = getFirstWormInRange(); //Cari worm musuh terdekat
        if (enemyWorm != null) { //Kalau ketemu
            Direction direction = resolveDirection(currentWorm.position, enemyWorm.position);
            if (currentWorm.id==2){ //Kalau wormnya bisa cast bananabomb
                if (canBananaBombThem(enemyWorm)){
                    return new BananaBombCommand(enemyWorm.position.x, enemyWorm.position.y);
                }
            }
            else if (currentWorm.id==3){ //Kalau wormnya bisa cast snowball
                if (canSnowballThem(enemyWorm)){
                    return new SnowballCommand(enemyWorm.position.x, enemyWorm.position.y);
                }
            }
            return new ShootCommand(direction); //Default command ; just normal attack
        }

        List<Cell> surroundingBlocks = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);
//        int cellIdx = random.nextInt(surroundingBlocks.size());

//        Cell block = surroundingBlocks.get(cellIdx);
        Worm targetWorm = getNearestWorm();
        Cell block = findNextCellInPath(surroundingBlocks, targetWorm.position.x, targetWorm.position.y);
        if (block.type == CellType.AIR) {
            return new MoveCommand(block.x, block.y);
        } else if (block.type == CellType.DIRT) {
            return new DigCommand(block.x, block.y);
        }
        return new DoNothingCommand();

    }

    private Cell findNextCellInPath(List<Cell> surroundingBlocks,int x,int y){
        List<Integer> pathDistance = new ArrayList<>();
        pathDistance = surroundingBlocks.stream().map(c -> euclideanDistance(c.x, c.y,x,y)).collect(Collectors.toList());
        return surroundingBlocks.get(pathDistance.indexOf(Collections.min(pathDistance)));
    }

//    private Cell getNearestPowerUp(){
//        List<Cell> HP = new ArrayList<>();
//        List<Integer> HPDistance = new ArrayList<Integer>();
//        int idxMin;
//        Arrays.stream(gameState.map);
//        HP = Arrays.stream(gameState.map).filter(c->c.).collect(Collectors.toList());
//        HPDistance = HP.stream().map(c->euclideanDistance(currentWorm.position.x, currentWorm.position.y, c.x, c.y)).collect(Collectors.toList());
//        idxMin = HPDistance.indexOf(Collections.min(HPDistance));
//        return HP.get(idxMin);
//    }

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

    private List<Worm> getWormsInRange(){
        List<Worm> nearbyWorms = new ArrayList<>();
        Set<String> cells = constructFireDirectionLines(currentWorm.weapon.range)
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
        List<Integer> wormsRange = new ArrayList<>();
        List<Worm> enemyWorms = new ArrayList<>();
        for (Worm enemyWorm : opponent.worms){
            if (enemyWorm.health>0) {
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
