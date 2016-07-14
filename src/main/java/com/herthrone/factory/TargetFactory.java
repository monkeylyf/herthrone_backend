package com.herthrone.factory;

import com.herthrone.base.Creature;
import com.herthrone.base.Destroyable;
import com.herthrone.base.Hero;
import com.herthrone.base.Minion;
import com.herthrone.configuration.TargetConfig;
import com.herthrone.constant.ConstMechanic;
import com.herthrone.constant.ConstType;
import com.herthrone.game.Container;
import com.herthrone.game.Side;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by yifengliu on 7/10/16.
 */
public class TargetFactory {

  private static final Logger logger = Logger.getLogger(TargetFactory.class.getName());

  public static boolean isMinionTargetable(final Minion minion, final Container<Minion> board, final ConstType type) {
    if (minion.booleanMechanics().isOn(ConstMechanic.IMMUNE)) {
      return false;
    } else {
      switch (type) {
        case ATTACK:
          return isMinionTargetableByAttack(minion, board);
        case SPELL:
          return isMinionTargetableBySpell(minion, board);
        default:
          throw new RuntimeException(String.format("Unknown type %s for target", type.toString()));
      }
    }
  }

  private static boolean isMinionTargetableByAttack(final Minion minion, final Container<Minion> board) {
    // A stealth minion can not be targeted, even it is a taunt minion.
    if (minion.booleanMechanics().isOn(ConstMechanic.STEALTH)) {
      return false;
    } else if (minion.booleanMechanics().isOn(ConstMechanic.TAUNT)) {
      // A taunt minion is targetable.
      return true;
    } else {
      // If there is any other minions on the board with taunt but not stealth ability, this minion
      // cannot be targeted.
      return !board.stream()
          .anyMatch(m ->
              m.booleanMechanics().isOn(ConstMechanic.TAUNT) &&
              m.booleanMechanics().isOff(ConstMechanic.STEALTH));
    }
  }

  private static boolean isMinionTargetableBySpell(final Minion minion, final Container<Minion> board) {
    return !minion.booleanMechanics().isOn(ConstMechanic.ELUSIVE);
  }

  public static boolean isHeroTargetable(final Hero hero, final Container<Minion> board, final ConstType type) {
    if (hero.booleanMechanics().isOn(ConstMechanic.IMMUNE)) {
      return false;
    } else {
      switch (type) {
        case ATTACK:
          return isHeroTargetableByAttack(hero, board);
        case SPELL:
          return isHeroTargetableBySpell(hero, board);
        default:
          throw new RuntimeException(String.format("Unknown type %s for target", type.toString()));
      }
    }
  }

  private static boolean isHeroTargetableByAttack(final Hero hero, final Container<Minion> board) {
    return hero.booleanMechanics().isOn(ConstMechanic.TAUNT);
  }

  private static boolean isHeroTargetableBySpell(final Hero hero, final Container<Minion> board) {
    return true;
  }

  public static class NoTargetFoundException extends RuntimeException {

    private NoTargetFoundException(final String message) {
      super(message);
    }
  }

  static List<Creature> getProperTargets(final TargetConfig targetConfig, final Side side) {
    switch (targetConfig.scope) {
      case OWN:
        return getProperTargetsBySide(targetConfig, side);
      case OPPONENT:
        return getProperTargetsBySide(targetConfig, side.getOpponentSide());
      case ALL:
        final List<Creature> targets = getProperTargetsBySide(targetConfig, side);
        targets.addAll(getProperTargetsBySide(targetConfig, side.getOpponentSide()));
        return targets;
      default:
        throw new RuntimeException("Unknown scope: " + targetConfig.scope);
    }
  }

  private static List<Creature> getProperTargetsBySide(final TargetConfig targetConfig,
                                                       final Side side) {
    switch (targetConfig.type) {
      case HERO:
        return Collections.singletonList(side.hero);
      case MINION:
        return side.board.stream().sorted(
            EffectFactory.compareBySequenceId).collect(Collectors.toList());
      case ALL:
        final List<Creature> targets = side.board.stream().sorted(
            EffectFactory.compareBySequenceId).collect(Collectors.toList());
        targets.add(side.hero);
        return targets;
      default:
        throw new NoTargetFoundException("No target found");
    }
  }

  static List<Destroyable> getDestroyablesBySide(final TargetConfig target, final Side side) {
    switch (target.type) {
      case MINION:
        return side.board.stream().collect(Collectors.toList());
      case WEAPON:
        return (side.hero.getWeapon().isPresent()) ?
          Collections.singletonList(side.hero.getWeapon().get()) : Collections.emptyList();
      case ALL:
        final List<Destroyable> destroyables = side.board.stream().collect(Collectors.toList());
        if (side.hero.getWeapon().isPresent()) {
          destroyables.add(side.hero.getWeapon().get());
        }
        return destroyables;
      default:
        throw new RuntimeException("Unknown type: " + target.type);
    }
  }

  static List<Destroyable> getDestroyables(final TargetConfig target, final Side side) {
    switch (target.scope) {
      case OWN:
        return getDestroyablesBySide(target, side);
      case OPPONENT:
        return getDestroyablesBySide(target, side.getOpponentSide());
      case ALL:
        final List<Destroyable> targets = getDestroyablesBySide(target, side);
        targets.addAll(getDestroyablesBySide(target, side.getOpponentSide()));
        return targets;
      default:
        throw new RuntimeException("Unknown scope: " + target.scope);
    }
  }

  public static List<Side> getSide(final TargetConfig target, final Side side) {
    switch (target.scope) {
      case OWN:
        return Collections.singletonList(side);
      case OPPONENT:
        return Collections.singletonList(side.getOpponentSide());
      case ALL:
        return Arrays.asList(side, side.getOpponentSide());
      default:
        throw new RuntimeException("Unknown target scope: " + target.scope);
    }
  }
}