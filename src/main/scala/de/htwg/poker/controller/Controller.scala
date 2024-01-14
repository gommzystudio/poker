package de.htwg.poker
package controller
import model.Player
import model.Dealer
import model.GameState
import util.Observable
import util.UndoManager

class Controller(var gameState: GameState) extends Observable {

  private val undoManager = new UndoManager

  /* the following methods are structured in this particular way:
    first, check the action for errors and throw an exception if necessary.
    second, update the gameState.
    third, notify the observers.
    additionally, for some actions like bet, call and fold it first has to be checked wether community cards need to be revealed.
   */

  def createGame(
      playerNameList: List[String],
      smallBlind: String,
      bigBlind: String
  ): Boolean = {
    if (playerNameList.size < 1) {
      throw new Exception("minimum two players")
    }
    try {
      smallBlind.toInt
      bigBlind.toInt
    } catch {
      case _: NumberFormatException =>
        throw new Exception("last 2 inputs must be integers")
    }

    val smallBlindInt = smallBlind.toInt
    val bigBlindInt = bigBlind.toInt

    if (smallBlindInt > 100 || bigBlindInt > 200) {
      throw new Exception(
        "small blind must be smaller than 101 and big blind must be smaller than 201"
      )
    }
    if (bigBlindInt <= smallBlindInt) {
      throw new Exception(
        "small blind must be smaller than big blind"
      )
    }

    gameState = Dealer.createGame(playerNameList, smallBlindInt, bigBlindInt)
    this.notifyObservers
    true
  }

  def undo: Unit = {
    undoManager.undoStep(this, this.gameState)
    notifyObservers
  }

  def redo: Unit = {
    undoManager.redoStep(this)
    notifyObservers
  }

  def bet(amount: Int): Boolean = {
    if (gameState.getPlayers.isEmpty) {
      throw new Exception("start a game first")
    } else if (
      gameState.getPlayers(gameState.getPlayerAtTurn).balance < amount
    ) {
      throw new Exception("insufficient balance")
    } else if (gameState.getBigBlind >= amount) {
      throw new Exception("bet Size is too low")
    } else if (gameState.getHighestBetSize >= amount) {
      throw new Exception("bet Size is too low")
    }

    undoManager.doStep(gameState)
    gameState = gameState.bet(amount)
    this.notifyObservers
    true
  }

  def allin(): Boolean = {
    if (gameState.getPlayers.isEmpty) {
      throw new Exception("start a game first")
    }

    undoManager.doStep(gameState)
    gameState = gameState.allIn()
    this.notifyObservers
    true
  }

  def fold(): Boolean = {
    if (gameState.getPlayers.isEmpty) {
      throw new Exception("start a game first")
    }

    undoManager.doStep(gameState)
    gameState = gameState.fold()

    // check if handout is required and if so, call updateBoard to reveal board Cards
    if (handout_required_fold()) {
      gameState = gameState.updateBoard.strategy
    }
    this.notifyObservers
    true
  }

  def call(): Boolean = {
    if (gameState.getPlayers.isEmpty) {
      throw new Exception("start a game first")
    } else if (gameState.getHighestBetSize == 0) {
      throw new Exception("invalid call before bet")
    } else if (
      gameState
        .getPlayers(gameState.getPlayerAtTurn)
        .currentAmountBetted == gameState.getHighestBetSize
    ) {
      throw new Exception("cannot call")
    }
    undoManager.doStep(gameState)
    gameState = gameState.call()

    // check if handout is required and if so, call updateBoard to reveal board Cards
    if (handout_required()) {
      gameState = gameState.updateBoard.strategy
    }
    this.notifyObservers
    true
  }

  def check(): Boolean = {
    if (gameState.getPlayers.isEmpty) {
      throw new Exception("start a game first")
    } else if (
      gameState
        .getPlayers(gameState.getPlayerAtTurn)
        .currentAmountBetted != gameState.getHighestBetSize
    ) {
      throw new Exception("cannot check")
    }

    undoManager.doStep(gameState)
    gameState = gameState.check()

    // check if handout is required and if so, call updateBoard to reveal board Cards
    if (handout_required()) {
      gameState = gameState.updateBoard.strategy
    }
    this.notifyObservers
    true
  }

  // helper methods

  // check if handout is required
  def handout_required(): Boolean = {
    gameState.getPlayers.forall(player =>
      gameState.getBoard.size == 0 && player.currentAmountBetted == gameState.getPlayers.head.currentAmountBetted
        && gameState.getPlayers.head.currentAmountBetted != 0
        && (gameState.getPlayers.size > 2 && gameState.getPlayerAtTurn == 2
          || gameState.getPlayers.size < 3 && gameState.getPlayerAtTurn == 0)
    ) ||
    gameState.getPlayers.forall(player =>
      gameState.getBoard.size != 0 &&
        player.currentAmountBetted == gameState.getPlayers.head.currentAmountBetted
    ) && gameState.playerAtTurn == 0
  }

  // check if handout is required after a fold
  def handout_required_fold(): Boolean = {
    gameState.getPlayerAtTurn == gameState.getPlayers.size - 1 && handout_required()
  }

  override def toString(): String = gameState.toString()
}
