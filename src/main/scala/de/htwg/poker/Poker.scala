package de.htwg.poker;
import controller.Controller
import aview.TUI
import scala.io.StdIn.readLine
import model.GameState

@main
def run: Unit = {
  val controller = new Controller(
    new GameState(Nil, None, None, 0, 0, Nil, 0, 0, 0)
  )
  val tui = new TUI(controller)
  tui.gameLoop()
}
