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
    private List<Worm> targets;
    static int selectToken = 5;

    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.opponent = gameState.opponents[0];
        this.currentWorm = getCurrentWorm(gameState);
        this.targets = designate_target();
    }

    private MyWorm getCurrentWorm(GameState gameState) {
        return Arrays.stream(gameState.myPlayer.worms)
                .filter(myWorm -> myWorm.id == gameState.currentWormId)
                .findFirst()
                .get();
    }

    private boolean canBananaBombThem(MyWorm playerWorm, Worm target){
        //Check if playerWorm can cast bananabomb
        if (playerWorm.id!=2){
            return false;
        }
        return (playerWorm.bananaBombs.count>0 && euclideanDistance(target.position.x, target.position.y, playerWorm.position.x, playerWorm.position.y) <= playerWorm.bananaBombs.range && euclideanDistance(target.position.x, target.position.y, playerWorm.position.x, playerWorm.position.y) > playerWorm.bananaBombs.damageRadius * 0.75);
    }

    private boolean canSnowballThem(MyWorm playerWorm, Worm target){
        //Check if playerWorm can cast snowball
        if (playerWorm.id!=3){
            return false;
        }
        return (playerWorm.snowballs.count>0 && euclideanDistance(target.position.x, target.position.y, playerWorm.position.x, playerWorm.position.y) <= playerWorm.snowballs.range && euclideanDistance(target.position.x, target.position.y, playerWorm.position.x, playerWorm.position.y) > playerWorm.snowballs.freezeRadius * Math.sqrt(2) && target.roundsUntilUnfrozen<=0);
    }

    private List<Worm> designate_target(){
        List<Integer> wormsJob = new ArrayList<>(); 
        List<Worm> enemyWorms = new ArrayList<>(); //List worm musuh
        for (Worm enemyWorm : opponent.worms) {
            enemyWorms.add(enemyWorm);
            wormsJob.add(enemyWorm.id);
        }
        List<Worm> targets = new ArrayList<>(); //make array of sorted enemies based on id
        while (enemyWorms.size()!=0){
            targets.add(enemyWorms.get(wormsJob.indexOf(Collections.max(wormsJob))));
            enemyWorms.remove(wormsJob.indexOf(Collections.max(wormsJob)));
            wormsJob.remove((wormsJob.indexOf(Collections.max(wormsJob))));
        }
        return targets;
    }
    
    private String createCommandShoot(Direction direction){
        return String.format(";shoot %s", direction.name());
    }
    private String createCommandBananaBomb(int x, int y){
        return String.format(";banana %d %d", x, y);
    }
    private String createCommandSnowball(int x, int y){
        return String.format(";snowball %d %d", x, y);
    }
    //HOW TO USE SELECT COMMAND
    //CONTOH :
    //return new SelectCommand(wormID, createCommandShoot(direction);
    
    public Command run() {
        //Mencari worm musuh terdekat
        /*Worm enemyWorm = getNearestWorm();
        Worm secondWormInBBRadius = getNearestWormInRadius(enemyWorm, 2);
        Worm secondWormInSBRadius = getNearestWormInRadius(enemyWorm, 1);*/
        for (int i = 0; i < targets.size(); i++){
            if (targets.get(i).health <= 0){
                targets.remove(i);
            }
        }

        for (int i = 0; i < targets.size(); i++){
            Worm enemyWorm = targets.get(i);
            if (canBananaBombThem(currentWorm, enemyWorm)){  //Jika bisa di bananabomb, maka langsung dibananabomb
                return new BananaBombCommand(enemyWorm.position.x, enemyWorm.position.y);
            }
            if (canSnowballThem(currentWorm, enemyWorm)){ //Jika bisa di snowball, maka langsung di snowbomb
                return new SnowballCommand(enemyWorm.position.x, enemyWorm.position.y);
            }
    }
        // Select Command
        if (selectToken > 0) {
            for (MyWorm pWorm : gameState.myPlayer.worms) {
                if (pWorm.id != currentWorm.id) {
                    /*
                    if (pWorm.id == 2) {
                        enemyWorm = getNearestWorm(pWorm);
                        if (canBananaBombThem(pWorm, enemyWorm)) {
                            selectToken -= 1;
                            return new SelectCommand(pWorm.id, createCommandBananaBomb(enemyWorm.position.x, enemyWorm.position.y));
                        }
                    }
                    if (pWorm.id == 3) {
                        enemyWorm = getNearestWorm(pWorm);
                        if (canBananaBombThem(pWorm, enemyWorm)) {
                            selectToken -= 1;
                            return new SelectCommand(pWorm.id, createCommandSnowball(enemyWorm.position.x, enemyWorm.position.y));
                        }
                    }
                    */
                    Worm enemyWormDefault = getFirstWormInRange(pWorm);
                    if (enemyWormDefault != null) {
                        Direction direction = resolveDirection(pWorm.position, enemyWormDefault.position);
                        selectToken -= 1;
                        return new SelectCommand(pWorm.id, createCommandShoot(direction));
                    }
                }
            }
        }   
        Worm enemyWormDefault = getFirstWormInRange(currentWorm); //Ini mendetect worm musuh pakai fungsi bawaan, biar menghindari weird error
        if (enemyWormDefault != null){ //Kalau ada musuh disekitar, maka bisa ditembak. Berlaku ke semua jenis worm/this is the default command
            Direction direction = resolveDirection(currentWorm.position, enemyWormDefault.position);
            return new ShootCommand(direction);
        }
        //Me list surrounding block, terus mencari block mana yg pathnya paling pendek
        List<Cell> surroundingBlocks = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);
        for (int i = 0; i < targets.size(); i++){
            Cell block = findNextCellInPath(surroundingBlocks, targets.get(i).position.x, targets.get(i).position.y);
            //Kalau ada dirt, dig. Kalau ngga, langsung jalan
            if (block.type == CellType.AIR) {
            return new MoveCommand(block.x, block.y);
        }   else if (block.type == CellType.DIRT) {
            return new DigCommand(block.x, block.y);
        }
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

    private Worm getFirstWormInRange(MyWorm playerWorm) {
        Set<String> cells = constructFireDirectionLines(playerWorm, playerWorm.weapon.range)
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

    private Worm getNearestWorm(MyWorm playerWorm){
        //get nearest worm
        List<Integer> wormsRange = new ArrayList<>(); //Jarak worm musuh dengan current worm
        List<Worm> enemyWorms = new ArrayList<>(); //List worm musuh
        for (Worm enemyWorm : opponent.worms){
            if (enemyWorm.health>0) { //Syaratnya worm musuh harus hidup
                enemyWorms.add(enemyWorm);
                wormsRange.add(euclideanDistance(playerWorm.position.x, playerWorm.position.y, enemyWorm.position.x, enemyWorm.position.y));
            }
        }
        return enemyWorms.get(wormsRange.indexOf(Collections.min(wormsRange)));
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

    private List<List<Cell>> constructFireDirectionLines(MyWorm playerWorm, int range) {
        List<List<Cell>> directionLines = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            List<Cell> directionLine = new ArrayList<>();
            for (int directionMultiplier = 1; directionMultiplier <= range; directionMultiplier++) {

                int coordinateX = playerWorm.position.x + (directionMultiplier * direction.x);
                int coordinateY = playerWorm.position.y + (directionMultiplier * direction.y);

                if (!isValidCoordinate(coordinateX, coordinateY)) {
                    break;
                }

                if (euclideanDistance(playerWorm.position.x, playerWorm.position.y, coordinateX, coordinateY) > range) {
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

    private boolean isOccupied(Cell cell){
        for (int i=0;i<gameState.myPlayer.worms.length;i++){
            if (cell.x==gameState.myPlayer.worms[i].position.x && cell.y==gameState.myPlayer.worms[i].position.y){
                return true;
            }
        }
        return false;
    }

    private List<Cell> getSurroundingCells(int x, int y) {
        ArrayList<Cell> cells = new ArrayList<>();
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                // Don't include the current position
                if (i != x && j != y && isValidCoordinate(i, j)) {
                    if (!isOccupied(gameState.map[j][i])) {
                        cells.add(gameState.map[j][i]);
                    }
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
