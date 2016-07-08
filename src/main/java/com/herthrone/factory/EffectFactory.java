package com.herthrone.factory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.herthrone.base.Creature;
import com.herthrone.base.Destroyable;
import com.herthrone.base.Effect;
import com.herthrone.base.Hero;
import com.herthrone.base.Minion;
import com.herthrone.base.Spell;
import com.herthrone.base.Weapon;
import com.herthrone.configuration.ConditionConfig;
import com.herthrone.configuration.EffectConfig;
import com.herthrone.configuration.MechanicConfig;
import com.herthrone.configuration.SpellConfig;
import com.herthrone.configuration.TargetConfig;
import com.herthrone.constant.ConstEffectType;
import com.herthrone.constant.ConstMechanic;
import com.herthrone.constant.ConstMinion;
import com.herthrone.constant.ConstTrigger;
import com.herthrone.constant.ConstWeapon;
import com.herthrone.constant.Constant;
import com.herthrone.effect.AttributeEffect;
import com.herthrone.effect.DestroyEffect;
import com.herthrone.effect.EquipWeaponEffect;
import com.herthrone.effect.GenerateEffect;
import com.herthrone.effect.MoveCardEffect;
import com.herthrone.effect.OverloadEffect;
import com.herthrone.effect.PhysicalDamageEffect;
import com.herthrone.effect.ReturnToHandEffect;
import com.herthrone.effect.SummonEffect;
import com.herthrone.effect.TakeControlEffect;
import com.herthrone.game.Side;
import com.herthrone.helper.RandomMinionGenerator;
import com.herthrone.object.ManaCrystal;
import com.herthrone.object.ValueAttribute;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by yifeng on 4/14/16.
 */
public class EffectFactory {

  private static Logger logger = Logger.getLogger(EffectFactory.class.getName());
  static final Comparator<Minion> compareBySequenceId = (m1, m2) -> Integer.compare(
      m1.getSequenceId(), m2.getSequenceId());

  public static void addAuraEffect(final EffectConfig effectConfig, final Minion minion,
                                   final Minion target) {
    switch (effectConfig.type) {
      case Constant.ATTACK:
        target.attack().addAuraBuff(minion, effectConfig.value);
        break;
      case Constant.HEALTH:
        target.health().addAuraBuff(minion, effectConfig.value);
        break;
      case Constant.MAX_HEALTH:
        target.maxHealth().addAuraBuff(minion, effectConfig.value);
        break;
      default:
        throw new RuntimeException(effectConfig.type + " not supported for aura");
    }
  }

  public static void removeAuraEffect(final EffectConfig effectConfig, final Minion minion,
                                      final Minion target) {
    switch (effectConfig.type) {
      case Constant.ATTACK:
        target.attack().removeAuraBuff(minion);
        break;
      case Constant.HEALTH:
        target.health().removeAuraBuff(minion);
        break;
      case Constant.MAX_HEALTH:
        target.maxHealth().removeAuraBuff(minion);
        break;
      default:
        throw new RuntimeException(effectConfig.type + " not supported for aura");
    }
  }

  public static boolean isTriggerConditionMet(final Optional<MechanicConfig> mechanicConfigOptional,
                                              final Side side, final Creature target) {
    if (!mechanicConfigOptional.isPresent()) {
      logger.debug("Mechanic configuration is absent");
      return false;
    } else if (!isConditionTriggered(mechanicConfigOptional.get().effect, target)) {
      logger.debug("Condition not met and mechanic effect not triggered");
      return false;
    } else {
      return true;
    }
  }

  public static void pipeMechanicEffectIfPresentAndMeetCondition(
      final Optional<MechanicConfig> mechanicConfigOptional, final Side side,
      final Creature caster, final Creature target) {
    if (isTriggerConditionMet(mechanicConfigOptional, side, target)) {
      final MechanicConfig mechanicConfig = mechanicConfigOptional.get();
      logger.debug("Triggering " + mechanicConfig.mechanic.toString());
      Effect effect = pipeMechanicEffect(mechanicConfig, target);
      target.binder().getSide().getEffectQueue().enqueue(effect);
    }
  }

  public static void triggerEndTurnMechanics(final Side side) {
    List<Minion> minions = side.board.stream()
        .sorted(compareBySequenceId)
        .filter(minion -> minion.getEffectMechanics().has(ConstTrigger.ON_END_TURN))
        .collect(Collectors.toList());

    for (final Minion minion : minions) {
      for (MechanicConfig mechanic : minion.getEffectMechanics().get(ConstTrigger.ON_END_TURN)) {
        final List<Creature> targets = getProperTargets(mechanic.effect.get().target, side, minion);
        final List<Effect> effects = targets.stream().map(target -> pipeMechanicEffect(
            mechanic, target)).collect(Collectors.toList());
        side.getEffectQueue().enqueue(effects);
      }
    }
  }

  static List<Creature> getProperTargets(final TargetConfig targetConfig, final Side side,
                                         final Creature caster) {
    switch (targetConfig.scope) {
      case OWN:
        return getProperTargetsBySide(targetConfig, side, caster);
      case OPPONENT:
        return getProperTargetsBySide(targetConfig, side.getOpponentSide(), caster);
      case ALL:
        final List<Creature> targets = getProperTargetsBySide(targetConfig, side, caster);
        targets.addAll(getProperTargetsBySide(targetConfig, side.getOpponentSide(), caster));
        return targets;
      default:
        throw new RuntimeException("Unknown scope: " + targetConfig.scope);
    }
  }

  private static List<Creature> getProperTargetsBySide(final TargetConfig targetConfig,
                                                       final Side side, final Creature caster) {
    final List<Creature> targets = new ArrayList<>();
    switch (targetConfig.type) {
      case HERO:
        targets.add(side.hero);
        break;
      case MINION:
        side.board.stream().sorted(EffectFactory.compareBySequenceId).forEach(targets::add);
        break;
      case ALL:
        side.board.stream().sorted(EffectFactory.compareBySequenceId).forEach(targets::add);
        targets.add(side.hero);
        break;
      default:
        targets.add(caster);
    }
    return targets;
  }

  private static boolean isConditionTriggered(final Optional<EffectConfig> effectConfigOptional,
                                              final Creature target) {
    if (effectConfigOptional.isPresent() &&
        effectConfigOptional.get().conditionConfigOptional.isPresent()) {
      // Check if there is condition config. If there is, return whether condition is met.
      final ConditionConfig conditionConfig = effectConfigOptional.get()
          .conditionConfigOptional.get();
      switch (conditionConfig.conditionType) {
        case BOARD_SIZE:
          return conditionConfig.inRange(target.binder().getSide().board.size());
        case COMBO:
          return target.binder().getSide().replay.size() > 1;
        case WEAPON_EQUIPED:
          final List<Destroyable> destroyables = getDestroyables(
              effectConfigOptional.get().target, target.binder().getSide());
          if (destroyables.size() == 0) {
            return false;
          } else {
            Preconditions.checkArgument(destroyables.size() == 1, "More than one destroyable object");
            Preconditions.checkArgument(destroyables.get(0) instanceof Weapon, "Only support weapon");
            return true;
          }
        default:
          throw new RuntimeException("Unknown condition: " + conditionConfig.conditionType);
      }
    } else {
      // If no condition configured, return true and the effect should be triggered any way.
      return true;
    }
  }

  public static Effect pipeMechanicEffect(final MechanicConfig mechanic, final Creature target) {
    final Optional<EffectConfig> config = mechanic.effect;
    Preconditions.checkArgument(config.isPresent(), "Mechanic " + mechanic + " has no effect");
    final EffectConfig effectConfig = config.get();
    final Creature realTarget = effectConfig.isRandom ?
        RandomMinionGenerator.randomCreature(effectConfig.target, target.binder().getSide()) :
        target;

    return pipeEffectsByConfig(effectConfig,  realTarget);
  }

  public static Effect pipeEffectsByConfig(final EffectConfig config, final Creature creature) {
    ConstEffectType effect = config.name;
    switch (effect) {
      case ATTRIBUTE:
        return getAttributeAction(config, creature);
      case CRYSTAL:
        return getCrystalEffect(config, creature);
      case DRAW:
        return getDrawCardAction(config, creature.binder().getSide());
      case GENERATE:
        return getGenerateAction(config, creature);
      case DESTROY:
        return getDestroyEffect(config, creature);
      case RETURN_TO_HAND:
        Preconditions.checkArgument(
            creature instanceof Minion, creature.type() + " can not be returned to player's hand");
        return getReturnToHandAction((Minion) creature);
      case SUMMON:
        return getSummonAction(config, creature.binder().getSide());
      case TAKE_CONTROL:
        return getTakeControlAction(config, creature);
      case WEAPON:
        Preconditions.checkArgument(
            creature instanceof Hero, creature.type() + " can not equip weapon");
        return getEquipWeaponAction((Hero) creature, config);
      default:
        throw new IllegalArgumentException("Unknown effect: " + effect);
    }
  }

  private static Effect getDestroyEffect(final EffectConfig config, final Creature creature) {
    List<Destroyable> destroyables = getDestroyables(config.target, creature.binder().getSide());
    Preconditions.checkArgument(destroyables.size() == 1, "TODO");
    return new DestroyEffect(destroyables.get(0));
  }

  private static List<Destroyable> getDestroyables(final TargetConfig target, final Side side) {
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

  private static List<Destroyable> getDestroyablesBySide(final TargetConfig target,
                                                         final Side side) {
    final List<Destroyable> destroyables = new ArrayList<>();
    switch (target.type) {
      case MINION:
        side.board.stream().forEach(destroyables::add);
        break;
      case WEAPON:
        if (side.hero.getWeapon().isPresent()) {
          destroyables.add(side.hero.getWeapon().get());
        }
        break;
      case ALL:
        side.board.stream().forEach(destroyables::add);
        if (side.hero.getWeapon().isPresent()) {
          destroyables.add(side.hero.getWeapon().get());
        }
        break;
      default:
        throw new RuntimeException("Unknown type: " + target.type);
    }
    return destroyables;
  }

  private static Effect getReturnToHandAction(final Minion target) {
    return new ReturnToHandEffect(target);
  }

  private static Effect getGenerateAction(EffectConfig config, Creature creature) {
    return new GenerateEffect(
        config.choices, config.type, config.target, creature.binder().getSide());
  }

  private static Effect getTakeControlAction(final EffectConfig effect, final Creature creature) {
    final Creature traitorMinion = RandomMinionGenerator.randomCreature(
        effect.target, creature.binder().getSide());
    Preconditions.checkArgument(traitorMinion instanceof Minion);
    return new TakeControlEffect((Minion) traitorMinion);
  }

  private static Effect getAttributeAction(final EffectConfig effect, final Creature creature) {
    final String type = effect.type;
    switch (type) {
      case (Constant.HEALTH):
        return getHealthAttributeAction(creature, effect);
      case (Constant.ATTACK):
        return getGeneralAttributeAction(creature.attack(), effect);
      case (Constant.CRYSTAL):
        return getGeneralAttributeAction(creature.manaCost(), effect);
      case (Constant.MAX_HEALTH):
        return getGeneralAttributeAction(creature.maxHealth(), effect);
      case (Constant.ARMOR):
        Preconditions.checkArgument(
            creature instanceof Hero, "Armor Attribute does not applies to " + creature.type());
        return getGeneralAttributeAction(((Hero) creature).armor(), effect);
      default:
        throw new IllegalArgumentException("Unknown effect type: " + type);
    }
  }

  private static Effect getEquipWeaponAction(final Hero hero, final EffectConfig effect) {
    final String weaponName = effect.type;
    final ConstWeapon weapon = ConstWeapon.valueOf(weaponName.toUpperCase());
    final Weapon weaponInstance = WeaponFactory.create(weapon);
    return new EquipWeaponEffect(hero, weaponInstance);
  }

  private static Effect getSummonAction(final EffectConfig effect, final Side side) {
    final List<String> summonChoices = effect.choices.stream()
        .map(String::toUpperCase)
        .collect(Collectors.toList());
    // Summon candidates must be non-existing on the board to avoid dups.
    final String summonTargetName = effect.isUnique ?
        RandomMinionGenerator.randomUnique(summonChoices, new ArrayList<>(side.board.asList())) :
        RandomMinionGenerator.randomOne(summonChoices);
    final ConstMinion summonTarget = ConstMinion.valueOf(summonTargetName);
    final Minion minion = MinionFactory.create(summonTarget);
    return new SummonEffect(side.board, minion);
  }

  private static Effect getDrawCardAction(final EffectConfig effect, final Side side) {
    // TODO: draw from own deck/opponent deck/opponent hand
    final TargetConfig target = effect.target;
    switch (target.type) {

    }
    return new MoveCardEffect(side.hand, side.deck, side);
  }

  private static Effect getCrystalEffect(final EffectConfig config, final Creature creature) {
    final String type = config.type;
    final ManaCrystal manaCrystal = creature.binder().getSide().manaCrystal;
    switch (type) {
      case (Constant.CRYSTAL_LOCK):
        return new OverloadEffect(manaCrystal, config.value);
      default:
        throw new IllegalArgumentException("Unknown type: " + type);
    }
  }

  private static Effect getHealthAttributeAction(final Creature creature, final EffectConfig effect) {
    final int value = effect.value;
    Preconditions.checkArgument(value != 0, "Health change must be non-zero");
    final int adjustChange = (value > 0) ? Math.min(value, creature.healthLoss()) : value;
    return new AttributeEffect(creature.health(), adjustChange, effect.isPermanent);
  }

  private static Effect getGeneralAttributeAction(final ValueAttribute attr, final EffectConfig effect) {
    Preconditions.checkArgument(effect.value != 0, "Attribute change must be non-zero");
    return new AttributeEffect(attr, effect.value, effect.isPermanent);
  }

  public static void pipeEffectsByConfig(final Spell spell, final Creature creature) {
    final List<Effect> effects = spell.getEffects().stream()
        .map(effect -> pipeEffectsByConfig(effect, creature)).collect(Collectors.toList());
    spell.binder().getSide().getEffectQueue().enqueue(effects);
  }

  public static List<Effect> pipeEffectsByConfig(final SpellConfig config, final Creature creature) {
    return config.effects.stream()
        .map(effect -> pipeEffectsByConfig(effect, creature)).collect(Collectors.toList());
  }


  public static class AttackFactory {

    public static void getPhysicalDamageAction(final Creature attacker, final Creature attackee) {
      final Effect effect = attacker.booleanMechanics().has(ConstMechanic.FORGETFUL) ?
          getForgetfulPhysicalDamageAction(attacker, attackee) : new PhysicalDamageEffect(attacker, attackee);
      attacker.binder().getSide().getEffectQueue().enqueue(effect);
    }

    private static Effect getForgetfulPhysicalDamageAction(final Creature attacker, final Creature attackee) {
      final boolean isForgetfulToPickNewTarget = RandomMinionGenerator.getBool();
      if (isForgetfulToPickNewTarget) {
        logger.debug("Forgetful triggered");
        final Creature substituteAttackee = RandomMinionGenerator.randomExcept(attackee.binder().getSide().allCreatures(), attackee);
        logger.debug(String.format("Change attackee from %s to %s", attackee.toString(), substituteAttackee.toString()));
        Preconditions.checkArgument(substituteAttackee != attackee);
        return new PhysicalDamageEffect(attacker, substituteAttackee);
      } else {
        logger.debug("Forgetful not triggered");
        return new PhysicalDamageEffect(attacker, attackee);
      }
    }
  }
}
