package com.herthrone.base;

import com.herthrone.constant.ConstMinion;
import com.herthrone.game.Container;
import com.herthrone.object.EffectMechanics;

/**
 * Created by yifeng on 4/2/16.
 */


public interface Minion extends Creature {

  EffectMechanics getEffectMechanics();

  int getSequenceId();

  void setSequenceId(final int id);

  void silence();

  void destroy();

  /**
   * A minion actively to be played by a player onto the board.
   * Battlecry effects will be triggered by the action of play.
   *
   * @param board
   */
  void playOnBoard(final Container<Minion> board);

  /**
   * A minion actively to be played by a player onto the board.
   * Battlecry effects will be triggered, with a specific target, by the action of play.
   *
   * @param board
   * @param target
   */
  void playOnBoard(final Container<Minion> board, final Creature target);

  /**
   * A minion passively to be put onto a board.
   *
   * @param board
   */
  void summonOnBoard(final Container<Minion> board);

  ConstMinion minionConstName();
}
