import java.util.ArrayList;
import java.util.Random;
import tester.*;
import javalib.impworld.*;
import java.util.Arrays;
import java.awt.Color;
import javalib.worldimages.*;

// represents a world for LightEmAll game
class LightEmAll extends World {
  // list of game pieces in a game board
  ArrayList<GamePiece> board;
  // a list of all nodes
  ArrayList<GamePiece> nodes;
  // a list of edges of the minimum spanning tree
  ArrayList<Edge> mst;
  // the width and height of the board
  int width;
  int height;
  // the current location of the power station,
  // as well as its effective radius
  int powerRow;
  int powerCol;
  int radius;
  
  //the current tile with the powerStation
  GamePiece currentPS;
  //the index of the currentPS
  int psIndex;
  //the tile that was pressed by the user
  GamePiece pressedTile;
  //the index of the pressed tile
  int pressedTileIndex;
  

  LightEmAll(int width, int height) {
    this.width = width;
    this.height = height;
    this.board = this.shuffleBoard();
    for (int i = 0; i < this.board.size(); i++) {
      if (this.board.get(i).powerStation) {
        this.currentPS = this.board.get(i);
        this.psIndex = i;
      }
    }
    this.radius = 0;
  }
  
  LightEmAll(ArrayList<GamePiece> board, int width, int height) {
    this.width = width;
    this.height = height;
    this.board = board;
    for (int i = 0; i < this.board.size(); i++) {
      if (this.board.get(i).powerStation) {
        this.currentPS = this.board.get(i);
        this.psIndex = i;
      }
    }
    this.radius = 0;
  }

  //manually makes a connected game board
  ArrayList<GamePiece> makeBoard() {
    ArrayList<GamePiece> board = new ArrayList<GamePiece>();
    int rows = this.height / 50;
    int cols = this.width / 50;

    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        if (i == rows / 2 && j == cols / 2) {
          board.add(new GamePiece(i, j, true, true, true, true, true, true));
        }
        else if (i == rows / 2 && j == 0) {
          board.add(new GamePiece(i, j, false, true, true, true, false, false));
        }
        else if (i == rows / 2 && j == cols - 1) {
          board.add(new GamePiece(i, j, true, false, true, true, false, false));
        }
        else if (i == rows / 2) {
          board.add(new GamePiece(i, j, true, true, true, true, false, false));
        }
        else if (i == rows - 1) {
          board.add(new GamePiece(i, j, false, false, true, false, false, false));
        }
        else if (i == 0) {
          board.add(new GamePiece(i, j, false, false, false, true, false, false));
        }
        else {
          board.add(new GamePiece(i, j, false, false, true, true, false, false));
        }
      }
    }
    return board;
  }
  
  //randomly rotates the wires for each tile
  //Effect: replaces the wire fields for each tile in the board
  ArrayList<GamePiece> shuffleBoard() {
    ArrayList<GamePiece> board = this.makeBoard();
    Random rand = new Random();
    
    for (int i = 0; i < board.size(); i++) {
      for (int j = 0; j < rand.nextInt(10); j++) {
        boolean temp = board.get(i).right;
        board.get(i).right = board.get(i).top; 
        board.get(i).top = board.get(i).left; 
        board.get(i).left = board.get(i).bottom;
        board.get(i).bottom = temp;
      }
    }
    return board;
  }
  
  //randomly rotates the wires for each tile
  //Effect: replaces the wire fields for each tile in the board
  ArrayList<GamePiece> shuffleBoardForTesting(Random rand) {
    ArrayList<GamePiece> board = this.makeBoard();
    
    for (int i = 0; i < board.size(); i++) {
      for (int j = 0; j < rand.nextInt(10); j++) {
        boolean temp = board.get(i).right;
        board.get(i).right = board.get(i).top; 
        board.get(i).top = board.get(i).left; 
        board.get(i).left = board.get(i).bottom;
        board.get(i).bottom = temp;
      }
    }
    return board;
  }

  //draws the board on the WorldScene
  //Effect: mutates the scene so that each row of tiles is placed on it
  public WorldScene makeScene() {
    WorldScene scene = new WorldScene(this.width, this.height);
    WorldImage prevRow = new EmptyImage();
    int rows = this.height / 50;
    int cols = this.width / 50;
    if (!allLit()) {
      for (int i = 0; i < rows; i++) {
        WorldImage topBox = new EmptyImage();
        for (int j = cols - 1; j >= 0; j--) {
          GamePiece piece = board.get(i * cols + j);
          if (piece.powered) {
            WorldImage currentBox = piece.tileImage(50, 10, Color.yellow, piece.powerStation);
            topBox = new BesideImage(topBox, currentBox);
          }
          else {
            WorldImage currentBox = piece.tileImage(50, 10, Color.gray, piece.powerStation);
            topBox = new BesideImage(topBox, currentBox);
          }
        }
        prevRow = new AboveImage(prevRow, topBox);
      }
      scene.placeImageXY(prevRow, this.width / 2, this.height / 2);
      return scene;
    }
    else {
      scene.placeImageXY(
          new OverlayImage(new TextImage("You Win!", Color.black),
              new RectangleImage(this.width, this.height, OutlineMode.SOLID, Color.green)),
          this.width / 2, this.height / 2);
      return scene;
    }
  }
  
  // checks that all tiles are powered
  public boolean allLit() {
    ArrayList<GamePiece> litList = new ArrayList<GamePiece>();
    for (int i = 0; i < this.board.size(); i++) {
      if (this.board.get(i).powered) {
        litList.add(this.board.get(i));
      }
    }
    return litList.size() == this.board.size();
  }
  
  //Effect: turns wires yellow if neighboring tiles are connected to it and are powered
  public void spreadPower(int index) {
    int cols = this.width / 50;
    int rows = this.height / 50;
    GamePiece current = this.board.get(index);
    int row = index / cols;
    int col = index % cols;
    // TOP
    if (row > 0) {
      int topIndex = (row - 1) * cols + col;
      GamePiece topNeighbor = this.board.get(topIndex);
      if (!topNeighbor.powered && current.top && topNeighbor.bottom) {
        topNeighbor.powered = true;
        spreadPower(topIndex);
      }
    }
    // BOTTOM
    if (row < rows - 1) {
      int bottomIndex = (row + 1) * cols + col;
      GamePiece bottomNeighbor = this.board.get(bottomIndex);
      if (!bottomNeighbor.powered && current.bottom && bottomNeighbor.top) {
        bottomNeighbor.powered = true;
        spreadPower(bottomIndex);
      }
    }
    // LEFT
    if (col < cols - 1) {
      int leftIndex = row * cols + (col + 1);
      GamePiece leftNeighbor = this.board.get(leftIndex);
      if (!leftNeighbor.powered && current.left && leftNeighbor.right) {
        leftNeighbor.powered = true;
        spreadPower(leftIndex);
      }
    }
    // RIGHT
    if (col > 0) {
      int rightIndex = row * cols + (col - 1);
      GamePiece rightNeighbor = this.board.get(rightIndex);
      if (!rightNeighbor.powered && current.right && rightNeighbor.left) {
        rightNeighbor.powered = true;
        spreadPower(rightIndex);
      }
    }
  }
  
  //Effect: for every tick, the power spreads to connected tiles from the source
  public void onTick() {
    int index = 0;
    for (int i = 0; i < this.board.size(); i++) {
      if (this.board.get(i).powerStation) {
        index = i;
      } 
      else {
        this.board.get(i).powered = false;
      }
    }
    this.spreadPower(index);
  }
  
  //Effect: moves the powerstation field of tiles corresponding to the key that was pressed
  public void onKeyEvent(String key) {
    if (key.equals("left")
        && this.psIndex + 1 < this.board.size()
        && this.board.get(this.psIndex + 1).col != 0
        && this.currentPS.left
        && this.board.get(this.psIndex + 1).right) {
      this.board.get(this.psIndex).powerStation = false;
      this.psIndex++;
      this.board.get(this.psIndex).powerStation = true;
      this.currentPS = this.board.get(this.psIndex);
    }
    
    if (key.equals("right")
        && this.psIndex - 1 >= 0
        && this.board.get(this.psIndex - 1).col != this.width / 50 - 1
        && this.currentPS.right
        && this.board.get(this.psIndex - 1).left) {
      this.board.get(this.psIndex).powerStation = false;
      this.psIndex--;
      this.board.get(this.psIndex).powerStation = true;
      this.currentPS = this.board.get(this.psIndex);
    }
    
    if (key.equals("up")
        && this.psIndex - this.width / 50 >= 0
        && this.board.get(this.psIndex - this.width / 50).row != -1
        && this.currentPS.top
        && this.board.get(this.psIndex - this.width / 50).bottom) {
      this.board.get(this.psIndex).powerStation = false;
      this.psIndex = this.psIndex - this.width / 50;
      this.board.get(this.psIndex).powerStation = true;
      this.currentPS = this.board.get(this.psIndex);
    }
    
    if (key.equals("down")
        && this.psIndex + this.width / 50 < this.board.size()
        && this.board.get(this.psIndex + this.width / 50).row != this.height / 50
        && this.currentPS.bottom
        && this.board.get(this.psIndex + this.width / 50).top) {
      this.board.get(this.psIndex).powerStation = false;
      this.psIndex = this.psIndex + this.width / 50;
      this.board.get(this.psIndex).powerStation = true;
      this.currentPS = this.board.get(this.psIndex);
    }
  }
  
  //Effect: rotates the wire fields of the tile that was pressed by the user
  //90 degrees to the right
  public void onMouseClicked(Posn pos) {
    this.pressedTileIndex = ((pos.y / 50) + 1) * (this.width / 50) - pos.x / 50 - 1;
    this.pressedTile = this.board.get(this.pressedTileIndex);
    boolean temp = this.board.get(this.pressedTileIndex).right;
    this.pressedTile.right = this.pressedTile.top; 
    this.pressedTile.top = this.pressedTile.left; 
    this.pressedTile.left = this.pressedTile.bottom;
    this.pressedTile.bottom = temp;
    System.out.println(pos);
    System.out.println(pressedTileIndex);
  }
}

//represents a tile with wires on it
class GamePiece {
  // in logical coordinates, with the origin
  // at the top-left corner of the screen
  int row;
  int col;
  // whether this GamePiece is connected to the
  // adjacent left, right, top, or bottom pieces
  boolean left;
  boolean right;
  boolean top;
  boolean bottom;
  // whether the power station is on this piece
  boolean powerStation;
  boolean powered;
  
  GamePiece(int row, int col, boolean left, boolean right, boolean top, boolean bottom,
      boolean powerStation, boolean powered) {
    this.row = row;
    this.col = col;
    this.left = left;
    this.right = right;
    this.top = top;
    this.bottom = bottom;
    this.powerStation = powerStation;
    this.powered = powered;
  }
 
  // Generate an image of this, the given GamePiece.
  // - size: the size of the tile, in pixels
  // - wireWidth: the width of wires, in pixels
  // - wireColor: the Color to use for rendering wires on this
  // - hasPowerStation: if true, draws a fancy star on this tile to represent the power station
  //
  WorldImage tileImage(int size, int wireWidth, Color wireColor, boolean hasPowerStation) {
    // Start tile image off as a blue square with a wire-width square in the middle,
    // to make image "cleaner" (will look strange if tile has no wire, but that can't be)
    WorldImage image = new OverlayImage(
        new RectangleImage(wireWidth, wireWidth, OutlineMode.SOLID, wireColor),
        new RectangleImage(size, size, OutlineMode.SOLID, Color.DARK_GRAY));
    WorldImage vWire = new RectangleImage(wireWidth, (size + 1) / 2, OutlineMode.SOLID, wireColor);
    WorldImage hWire = new RectangleImage((size + 1) / 2, wireWidth, OutlineMode.SOLID, wireColor);
 
    if (this.top) { 
      image = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        vWire, 0, 0, image);
    }
    if (this.right) { 
      image = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        hWire, 0, 0, image);
    }
    if (this.bottom) {
      image = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        vWire, 0, 0, image);
    }
    if (this.left) { 
      image = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        hWire, 0, 0, image);
    }
    if (hasPowerStation) {
      image = new OverlayImage(
                  new OverlayImage(
                      new StarImage(size / 3, 7, OutlineMode.OUTLINE, new Color(255, 128, 0)),
                      new StarImage(size / 3, 7, OutlineMode.SOLID, new Color(0, 255, 255))),
                  image);
    }
    return image;
  }
}

//represents an edge between two gamepieces
class Edge {
  GamePiece fromNode;
  GamePiece toNode;
  int weight;
}

class ExamplesLEA {
  LightEmAll game;
  ArrayList<GamePiece> board;
  LightEmAll gameB;
  ArrayList<GamePiece> boardB;
  LightEmAll game16;
  ArrayList<GamePiece> board16;
  LightEmAll hardCodedBoard;
  LightEmAll hardCodedRandomBoard;
  
  GamePiece topLeft;
  GamePiece topMiddle;
  GamePiece topRight;
  GamePiece middleLeft;
  GamePiece middleMiddle;
  GamePiece middleRight;
  GamePiece bottomLeft;
  GamePiece bottomMiddle;
  GamePiece bottomRight;
  ArrayList<GamePiece> threeByThree;
  
  GamePiece topLeftRandom;
  GamePiece topMiddleRandom;
  GamePiece topRightRandom;
  GamePiece middleLeftRandom;
  GamePiece middleMiddleRandom;
  GamePiece middleRightRandom;
  GamePiece bottomLeftRandom;
  GamePiece bottomMiddleRandom;
  GamePiece bottomRightRandom;
  ArrayList<GamePiece> threeByThreeRandom;
  
  GamePiece first;
  GamePiece second;
  GamePiece third;
  GamePiece fourth;
  GamePiece fifth;
  GamePiece sixth;
  GamePiece seventh;
  GamePiece eighth;
  GamePiece nineth;
  GamePiece tenth;
  GamePiece eleventh;
  GamePiece twelveth;
  GamePiece thirteenth;
  GamePiece fourteenth;
  GamePiece fifteenth;
  GamePiece sixteenth;
  ArrayList<GamePiece> fourByFour;
  
  GamePiece firstRandom;
  GamePiece secondRandom;
  GamePiece thirdRandom;
  GamePiece fourthRandom;
  GamePiece fifthRandom;
  GamePiece sixthRandom;
  GamePiece seventhRandom;
  GamePiece eighthRandom;
  GamePiece ninethRandom;
  GamePiece tenthRandom;
  GamePiece eleventhRandom;
  GamePiece twelvethRandom;
  GamePiece thirteenthRandom;
  GamePiece fourteenthRandom;
  GamePiece fifteenthRandom;
  GamePiece sixteenthRandom;
  ArrayList<GamePiece> fourByFourRandom;
  
  void initData() {
    this.game = new LightEmAll(150, 150);
    this.board = game.makeBoard();
    this.game.board = board;
   
    this.gameB = new LightEmAll(400, 600);
    this.boardB = gameB.makeBoard();
    this.gameB.board = boardB;
    
    this.topLeft = new GamePiece(0, 0, false, false, false, true, false, false);
    this.topMiddle = new GamePiece(0, 1, false, false, false, true, false, false);
    this.topRight = new GamePiece(0, 2, false, false, false, true, false, false);
    this.middleLeft = new GamePiece(1, 0, false, true, true, true, false, false);
    this.middleMiddle = new GamePiece(1, 1, true, true, true, true, true, true);
    this.middleRight = new GamePiece(1, 2, true, false, true, true, false, false);
    this.bottomLeft = new GamePiece(2, 0, false, false, true, false, false, false);
    this.bottomMiddle = new GamePiece(2, 1, false, false, true, false, false, false);
    this.bottomRight = new GamePiece(2, 2, false, false, true, false, false, false);
    this.threeByThree = new ArrayList<GamePiece>(Arrays.asList(
        this.topLeft, this.topMiddle, this.topRight,
        this.middleLeft, this.middleMiddle, this.middleRight,
        this.bottomLeft, this.bottomMiddle, this.bottomRight));
    
    this.topLeftRandom = new GamePiece(0, 0, true, false, false, false, false, false);
    this.topMiddleRandom = new GamePiece(0, 1, false, false, true, false, false, false);
    this.topRightRandom = new GamePiece(0, 2, false, true, false, false, false, false);
    this.middleLeftRandom = new GamePiece(1, 0, true, true, false, true, false, false);
    this.middleMiddleRandom = new GamePiece(1, 1, true, true, true, true, true, true);
    this.middleRightRandom = new GamePiece(1, 2, true, true, false, true, false, false);
    this.bottomLeftRandom = new GamePiece(2, 0, false, false, false, true, false, false);
    this.bottomMiddleRandom = new GamePiece(2, 1, false, true, false, false, false, false);
    this.bottomRightRandom = new GamePiece(2, 2, false, false, false, true, false, false);
    this.threeByThreeRandom = new ArrayList<GamePiece>(Arrays.asList(
        this.topLeftRandom, this.topMiddleRandom, this.topRightRandom,
        this.middleLeftRandom, this.middleMiddleRandom, this.middleRightRandom,
        this.bottomLeftRandom, this.bottomMiddleRandom, this.bottomRightRandom));
    
    this.game16 = new LightEmAll(200, 200);
    this.board16 = game16.makeBoard();
    this.game16.board = board16;
    
    this.first = new GamePiece(0, 0, false, false, false, true, false, false);
    this.second = new GamePiece(0, 1, false, false, false, true, false, false);
    this.third = new GamePiece(0, 2, false, false, false, true, false, false);
    this.fourth = new GamePiece(0, 3, false, false, false, true, false, false);
    this.fifth = new GamePiece(1, 0, false, false, true, true, false, false);
    this.sixth = new GamePiece(1, 1, false, false, true, true, false, false);
    this.seventh = new GamePiece(1, 2, false, false, true, true, false, false);
    this.eighth = new GamePiece(1, 3, false, false, true, true, false, false);
    this.nineth = new GamePiece(2, 0, false, true, true, true, false, false);
    this.tenth = new GamePiece(2, 1, true, true, true, true, false, false);
    this.eleventh = new GamePiece(2, 2, true, true, true, true, true, true);
    this.twelveth = new GamePiece(2, 3, true, false, true, true, false, false);
    this.thirteenth = new GamePiece(3, 0, false, false, true, false, false, false);
    this.fourteenth = new GamePiece(3, 1, false, false, true, false, false, false);
    this.fifteenth = new GamePiece(3, 2, false, false, true, false, false, false);
    this.sixteenth = new GamePiece(3, 3, false, false, true, false, false, false);
    this.fourByFour = new ArrayList<GamePiece>(Arrays.asList(
        this.first, this.second, this.third, this.fourth,
        this.fifth, this.sixth, this.seventh, this.eighth,
        this.nineth, this.tenth, this.eleventh, this.twelveth,
        this.thirteenth, this.fourteenth, this.fifteenth, this.sixteenth));
    
    this.firstRandom = new GamePiece(0, 0, true, false, false, false, false, false);
    this.secondRandom = new GamePiece(0, 1, false, false, true, false, false, false);
    this.thirdRandom = new GamePiece(0, 2, false, true, false, false, false, false);
    this.fourthRandom = new GamePiece(0, 3, true, false, false, false, false, false);
    this.fifthRandom = new GamePiece(1, 0, true, true, false, false, false, false);
    this.sixthRandom = new GamePiece(1, 1, true, true, false, false, false, false);
    this.seventhRandom = new GamePiece(1, 2, false, false, true, true, false, false);
    this.eighthRandom = new GamePiece(1, 3, true, true, false, false, false, false);
    this.ninethRandom = new GamePiece(2, 0, true, false, true, true, false, false);
    this.tenthRandom = new GamePiece(2, 1, true, true, true, true, false, false);
    this.eleventhRandom = new GamePiece(2, 2, true, true, true, true, true, true);
    this.twelvethRandom = new GamePiece(2, 3, true, false, true, true, false, false);
    this.thirteenthRandom = new GamePiece(3, 0, false, true, false, false, false, false);
    this.fourteenthRandom = new GamePiece(3, 1, false, false, true, false, false, false);
    this.fifteenthRandom = new GamePiece(3, 2, false, false, true, false, false, false);
    this.sixteenthRandom = new GamePiece(3, 3, false, true, false, false, false, false);
    this.fourByFourRandom = new ArrayList<GamePiece>(Arrays.asList(
        this.firstRandom, this.secondRandom, this.thirdRandom, this.fourthRandom,
        this.fifthRandom, this.sixthRandom, this.seventhRandom, this.eighthRandom,
        this.ninethRandom, this.tenthRandom, this.eleventhRandom, this.twelvethRandom,
        this.thirteenthRandom, this.fourteenthRandom, this.fifteenthRandom, this.sixteenthRandom));
  
    this.hardCodedBoard = new LightEmAll(this.fourByFour, 200, 200);
    this.hardCodedRandomBoard = new LightEmAll(this.threeByThreeRandom, 150, 150);
  }
  
  void testMakeBoard(Tester t) {
    this.initData();
    //tests a three by three
    t.checkExpect(this.game.makeBoard(), this.threeByThree);
    //tests a four by four
    t.checkExpect(this.game16.makeBoard(), this.fourByFour);
  }
  
  void testShuffleBoard(Tester t) {
    this.initData();
    //tests a three by three
    t.checkExpect(this.game.shuffleBoardForTesting(new Random(100)), this.threeByThreeRandom);
    //tests a four by four
    t.checkExpect(this.game16.shuffleBoardForTesting(new Random(100)), this.fourByFourRandom);
  }
  
  void testMakeScene(Tester t) {
    WorldScene scene = new WorldScene(200, 200);
    scene.placeImageXY(
        new OverlayImage(new TextImage("You Win!", Color.black),
            new RectangleImage(200, 200, OutlineMode.SOLID, Color.green)),
        200 / 2, 200 / 2);
    this.hardCodedBoard.spreadPower(10);
    t.checkExpect(this.hardCodedBoard.makeScene(), scene);
    t.checkExpect(this.hardCodedRandomBoard.makeScene(), null);
    
  }
  
  void testSpreadPower(Tester t) {
    //tests that every piece in a fully connected board is lit up
    this.initData();
    game.spreadPower(4); // center tile
    for (int i = 0; i < game.board.size(); i++) {
      GamePiece gp = game.board.get(i);
      t.checkExpect(gp.powered, true);
    }
   
    //tests that the wire above is not powered when disconnected
    this.initData();
    game.onMouseClicked(new Posn(125, 25));
    game.spreadPower(4);
    t.checkExpect(game.board.get(0).powered, false);
    t.checkExpect(game.board.get(3).powered, true);
    //tests that wire above is powered when connected by right side
    //3 way wire that was turned and that the wire bellow has been disconnected
    this.initData();
    game.onMouseClicked(new Posn(125, 75));
    game.spreadPower(4);
    t.checkExpect(game.board.get(0).powered, true);
    t.checkExpect(game.board.get(3).powered, true);
    t.checkExpect(game.board.get(6).powered, false);
   
    //tests that when 3 way wire is disconnected it and the wires above and bellow
    // are also not powered
    this.initData();
    game.onMouseClicked(new Posn(125, 75));
    game.onMouseClicked(new Posn(125, 75));
    game.spreadPower(4);
    t.checkExpect(game.board.get(0).powered, false);
    t.checkExpect(game.board.get(3).powered, false);
    t.checkExpect(game.board.get(6).powered, false);
    //tests that the wire above is not powered when disconnected
    this.initData();
    gameB.onMouseClicked(new Posn(25, 25));
    gameB.spreadPower(52);
    t.checkExpect(gameB.board.get(7).powered, false);
    t.checkExpect(gameB.board.get(15).powered, true);
   
    //tests that when 3 way wire is disconnected it and the wires above and bellow
    //are also not powered
    this.initData();
    gameB.onMouseClicked(new Posn(25, 325));
    gameB.onMouseClicked(new Posn(25, 325));
    gameB.onTick();
   
    gameB.spreadPower(52);
    t.checkExpect(gameB.board.get(47).powered, false);
    t.checkExpect(gameB.board.get(55).powered, false);
    t.checkExpect(gameB.board.get(63).powered, false);
    //tests that 4 way wire stays powered
    this.initData();
    gameB.onMouseClicked(new Posn(225, 325));
    gameB.spreadPower(52);
    t.checkExpect(gameB.board.get(50).powered, true);
    t.checkExpect(gameB.board.get(42).powered, true);
    t.checkExpect(gameB.board.get(50).powered, true);
    t.checkExpect(gameB.board.get(58).powered, true);
    t.checkExpect(gameB.board.get(49).powered, true);
    gameB.onMouseClicked(new Posn(225, 325));
    gameB.spreadPower(52);
    t.checkExpect(gameB.board.get(50).powered, true);
    t.checkExpect(gameB.board.get(42).powered, true);
    t.checkExpect(gameB.board.get(50).powered, true);
    t.checkExpect(gameB.board.get(58).powered, true);
    t.checkExpect(gameB.board.get(49).powered, true);
   
    gameB.onMouseClicked(new Posn(225, 325));
    gameB.spreadPower(52);
    t.checkExpect(gameB.board.get(50).powered, true);
    t.checkExpect(gameB.board.get(42).powered, true);
    t.checkExpect(gameB.board.get(50).powered, true);
    t.checkExpect(gameB.board.get(58).powered, true);
    t.checkExpect(gameB.board.get(49).powered, true);
   
    gameB.onMouseClicked(new Posn(225, 325));
    gameB.spreadPower(52);
    t.checkExpect(gameB.board.get(50).powered, true);
    t.checkExpect(gameB.board.get(42).powered, true);
    t.checkExpect(gameB.board.get(50).powered, true);
    t.checkExpect(gameB.board.get(58).powered, true);
    t.checkExpect(gameB.board.get(49).powered, true);
   
  }
  
  void testAllLit(Tester t) {
    // All tiles are powered
    // Since all tiles are powered, allLit() should return true
    ArrayList<GamePiece> fullyLitBoard = new ArrayList<>();
    fullyLitBoard.add(new GamePiece(0, 0, true, true, true, true, true, true));
    fullyLitBoard.add(new GamePiece(0, 1, true, true, true, true, true, true));
    fullyLitBoard.add(new GamePiece(0, 2, true, true, true, true, true, true));
    fullyLitBoard.add(new GamePiece(1, 0, true, true, true, true, true, true));
    fullyLitBoard.add(new GamePiece(1, 1, true, true, true, true, true, true));
    fullyLitBoard.add(new GamePiece(1, 2, true, true, true, true, true, true));
    fullyLitBoard.add(new GamePiece(2, 0, true, true, true, true, true, true));
    fullyLitBoard.add(new GamePiece(2, 1, true, true, true, true, true, true));
    fullyLitBoard.add(new GamePiece(2, 2, true, true, true, true, true, true));
    LightEmAll fullyLitGame = new LightEmAll(3, 3);
    fullyLitGame.board = fullyLitBoard;
    t.checkExpect(fullyLitGame.allLit(), true); 
    // Not all tiles are powered
    // Since tile 4 is not powered, it should return false
    ArrayList<GamePiece> notAllLitBoard = new ArrayList<>();
    notAllLitBoard.add(new GamePiece(0, 0, true, true, true, true, true, true));
    notAllLitBoard.add(new GamePiece(0, 1, true, true, true, true, true, true));
    notAllLitBoard.add(new GamePiece(0, 2, true, true, true, true, true, true));
    notAllLitBoard.add(new GamePiece(1, 0, true, true, true, true, true, true));
    notAllLitBoard.add(new GamePiece(1, 1, true, true, true, true, false, false)); 
    notAllLitBoard.add(new GamePiece(1, 2, true, true, true, true, true, true));
    notAllLitBoard.add(new GamePiece(2, 0, true, true, true, true, true, true));
    notAllLitBoard.add(new GamePiece(2, 1, true, true, true, true, true, true));
    notAllLitBoard.add(new GamePiece(2, 2, true, true, true, true, true, true));
    LightEmAll notAllLitGame = new LightEmAll(3, 3);
    notAllLitGame.board = notAllLitBoard;
    t.checkExpect(notAllLitGame.allLit(), false);
  }

  void testOnTick(Tester t) {
    //tests that powerStation is correctly assigned
    this.initData();
    game.onTick();
    t.checkExpect(game.board.get(4).powered, true);
    t.checkExpect(game.board.get(3).powered, false);
   
    //tests that powerStation is correctly assigned after moved right
    game.onKeyEvent("right");
    game.onTick();
    t.checkExpect(game.board.get(4).powerStation, false);
    t.checkExpect(game.board.get(3).powerStation, true);
    //tests that powerStation is correctly assigned after moved left
    this.initData();
    game.onKeyEvent("left");
    game.onTick();
    t.checkExpect(game.board.get(4).powerStation, false);
    t.checkExpect(game.board.get(5).powerStation, true);
   
    //tests that powerStation is correctly assigned after moved up
    this.initData();
    game.onKeyEvent("up");
    game.onTick();
    t.checkExpect(game.board.get(4).powerStation, false);
    t.checkExpect(game.board.get(1).powerStation, true);
   
    //tests that powerStation is correctly assigned after moved down
    this.initData();
    game.onKeyEvent("down");
    game.onTick();
    t.checkExpect(game.board.get(4).powerStation, false);
    t.checkExpect(game.board.get(7).powerStation, true);
  }
   
  void testOnKey(Tester t) {
    this.initData();
    //tests each movement of power station on a three by three
    t.checkExpect(this.fourByFour.get(10).powerStation, true);
    t.checkExpect(this.fourByFour.get(9).powerStation, false);
    this.hardCodedBoard.onKeyEvent("r");
    t.checkExpect(this.fourByFour.get(10).powerStation, true);
    t.checkExpect(this.fourByFour.get(9).powerStation, false);
    
    t.checkExpect(this.fourByFour.get(10).powerStation, true);
    t.checkExpect(this.fourByFour.get(9).powerStation, false);
    this.hardCodedBoard.onKeyEvent("right");
    t.checkExpect(this.fourByFour.get(10).powerStation, false);
    t.checkExpect(this.fourByFour.get(9).powerStation, true);
    
    t.checkExpect(this.fourByFour.get(9).powerStation, true);
    t.checkExpect(this.fourByFour.get(5).powerStation, false);
    this.hardCodedBoard.onKeyEvent("up");
    t.checkExpect(this.fourByFour.get(9).powerStation, false);
    t.checkExpect(this.fourByFour.get(5).powerStation, true);
    
    t.checkExpect(this.fourByFour.get(5).powerStation, true);
    t.checkExpect(this.fourByFour.get(9).powerStation, false);
    this.hardCodedBoard.onKeyEvent("down");
    t.checkExpect(this.fourByFour.get(5).powerStation, false);
    t.checkExpect(this.fourByFour.get(9).powerStation, true);
    
    t.checkExpect(this.fourByFour.get(9).powerStation, true);
    t.checkExpect(this.fourByFour.get(10).powerStation, false);
    this.hardCodedBoard.onKeyEvent("left");
    t.checkExpect(this.fourByFour.get(9).powerStation, false);
    t.checkExpect(this.fourByFour.get(10).powerStation, true);
  }
 
  void testOnMouseClicked(Tester t) {
    this.initData();
    //test with a top and bottom
    t.checkExpect(this.fourByFour.get(5).top, true);
    this.hardCodedBoard.onMouseClicked(new Posn(126, 81));
    t.checkExpect(this.fourByFour.get(5).top, false);
    
    //test with a top, right, and bottom
    t.checkExpect(this.fourByFour.get(8).left, false);
    this.hardCodedBoard.onMouseClicked(new Posn(175, 126));
    t.checkExpect(this.fourByFour.get(8).left, true);
    
    //test with a bottom
    t.checkExpect(this.fourByFour.get(0).left, false);
    this.hardCodedBoard.onMouseClicked(new Posn(175, 27));
    t.checkExpect(this.fourByFour.get(0).left, true);
  }
  
  // test that renders the connections game
  void testBigBang(Tester t) {
    LightEmAll world = new LightEmAll(200, 200);
    int w = 200;
    int h = 200;
    double tickRate = 0.1;
    world.bigBang(w, h, tickRate);
  }
}