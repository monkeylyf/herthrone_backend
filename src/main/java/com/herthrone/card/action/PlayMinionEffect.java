package com.herthrone.card.action;

import com.herthrone.base.Minion;
import com.herthrone.card.factory.Action;
import com.herthrone.game.Container;

/**
 * Created by yifengliu on 5/25/16.
 */
public class PlayMinionEffect implements Action {

  private final Minion minion;
  private final Container<Minion> board;

  public PlayMinionEffect(final Minion minion, final Container<Minion> board) {
    this.minion = minion;
    this.board = board;
  }

  @Override
  public void act() {
    board.add(minion);
  }
}