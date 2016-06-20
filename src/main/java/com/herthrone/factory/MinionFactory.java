package com.herthrone.factory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.herthrone.base.Creature;
import com.herthrone.base.Minion;
import com.herthrone.configuration.ConfigLoader;
import com.herthrone.configuration.MechanicConfig;
import com.herthrone.configuration.MinionConfig;
import com.herthrone.constant.ConstClass;
import com.herthrone.constant.ConstMechanic;
import com.herthrone.constant.ConstMinion;
import com.herthrone.constant.ConstType;
import com.herthrone.constant.Constant;
import com.herthrone.game.Battlefield;
import com.herthrone.stats.BooleanAttribute;
import com.herthrone.stats.BooleanMechanics;
import com.herthrone.stats.EffectMechanics;
import com.herthrone.stats.IntAttribute;
import org.apache.log4j.Logger;

import java.util.Map;


/**
 * Created by yifeng on 4/13/16.
 */
public class MinionFactory {

  private static final int MINION_INIT_MOVE_POINTS = 1;
  private static final int WINDFURY_INIT_MOVE_POINTS = 2;
  static Logger logger = Logger.getLogger(MinionFactory.class.getName());
  private final Battlefield battlefield;


  public MinionFactory(final Battlefield battlefield) {
    this.battlefield = battlefield;
  }

  public Minion createMinionByName(final ConstMinion minion) {
    MinionConfig config = ConfigLoader.getMinionConfigByName(minion);
    return createMinion(config.getHealth(), config.getAttack(), config.getCrystal(), config
        .getClassName(), config.getName(), config.getMechanics(), config.isCollectible());
  }

  Minion createMinion(final int health, final int attack, final int crystalManaCost,
                      final ConstClass className, final ConstMinion name,
                      final Map<ConstMechanic, MechanicConfig> mechanics, final boolean isCollectible) {
    return createMinion(health, attack, crystalManaCost,
        className, name, mechanics, isCollectible, battlefield);
  }

  public Minion createMinion(final int health, final int attack, final int crystalManaCost,
                             final ConstClass className, final ConstMinion name,
                             final Map<ConstMechanic, MechanicConfig> mechanics,
                             final boolean isCollectible, final Battlefield field) {
    final Minion minion = new Minion() {

      private final IntAttribute healthAttr = new IntAttribute(health);
      private final IntAttribute healthUpperAttr = new IntAttribute(health);
      private final IntAttribute attackAttr = new IntAttribute(attack);
      private final IntAttribute crystalManaCostAttr = new IntAttribute(crystalManaCost);
      private final IntAttribute movePoints = new IntAttribute(
          mechanics.containsKey(ConstMechanic.WINDFURY) ?
              WINDFURY_INIT_MOVE_POINTS : MINION_INIT_MOVE_POINTS);
      private final BooleanMechanics booleanMechanics = new BooleanMechanics(mechanics);
      private final EffectMechanics effectMechanics = new EffectMechanics(mechanics);
      private final Battlefield battlefield = field;

      private Optional<Integer> seqId = Optional.absent();

      @Override
      public EffectMechanics getEffectMechanics() {
        return effectMechanics;
      }

      @Override
      public int getSequenceId() {
        Preconditions.checkArgument(seqId.isPresent(), "Minion sequence Id not set yet");
        return seqId.get().intValue();
      }

      @Override
      public void setSequenceId(final int sequenceId) {
        Preconditions.checkArgument(!seqId.isPresent(), "Minion sequence Id already set");
        seqId = Optional.of(sequenceId);
        logger.debug(String.format("%s ID set to %d", getCardName(), sequenceId));
      }

      @Override
      public void silence() {

      }

      @Override
      public void destroy() {
        final int health = healthAttr.getVal();
        healthAttr.decrease(health);
      }

      @Override
      public String getCardName() {
        return name.toString();
      }

      @Override
      public ConstType getType() {
        return ConstType.MINION;
      }

      @Override
      public ConstClass getClassName() {
        return className;
      }

      @Override
      public IntAttribute getCrystalManaCost() {
        return crystalManaCostAttr;
      }

      @Override
      public boolean isCollectible() {
        return isCollectible;
      }

      @Override
      public Map<String, String> view() {
        return ImmutableMap.<String, String>builder()
            .put(Constant.CARD_NAME, getCardName())
            .put(Constant.HEALTH, getHealthAttr().toString() + "/" + getHealthUpperAttr().toString())
            .put(Constant.ATTACK, getAttackAttr().toString())
            .put(Constant.CRYSTAL, getCrystalManaCost().toString())
            //.put(Constant.DESCRIPTION, "TODO")
            .put(Constant.TYPE, getClassName().toString())
            .put(Constant.MOVE_POINTS, getAttackMovePoints().toString())
            .build();
      }

      @Override
      public IntAttribute getHealthAttr() {
        return healthAttr;
      }

      @Override
      public IntAttribute getHealthUpperAttr() {
        return healthUpperAttr;
      }

      @Override
      public IntAttribute getAttackAttr() {
        return attackAttr;
      }

      @Override
      public IntAttribute getAttackMovePoints() {
        return this.movePoints;
      }

      @Override
      public BooleanMechanics getBooleanMechanics() {
        return booleanMechanics;
      }

      @Override
      public void causeDamage(final Creature creature) {
        // TODO: but this is not the only way to reveal a minion in stealth.
        // http://hearthstone.gamepedia.com/Stealth
        booleanMechanics.resetIfPresent(ConstMechanic.STEALTH);
        boolean isDamaged = creature.takeDamage(attackAttr.getVal());

        if (isDamaged && BooleanAttribute.isPresentAndOn(booleanMechanics.get(ConstMechanic.FREEZE))) {
          creature.getBooleanMechanics().initialize(ConstMechanic.FROZEN, 1);
        }

        if (isDamaged &&
            BooleanAttribute.isPresentAndOn(booleanMechanics.get(ConstMechanic.POISON)) &&
            creature instanceof Minion) {
          ((Minion) creature).destroy();
        }
      }

      @Override
      public boolean takeDamage(final int damage) {
        final Optional<BooleanAttribute> divineShield = booleanMechanics.get(
            ConstMechanic.DIVINE_SHIELD);
        if (BooleanAttribute.isPresentAndOn(divineShield)) {
          logger.debug(ConstMechanic.DIVINE_SHIELD + " absorbed the damage");
          booleanMechanics.resetIfPresent(ConstMechanic.DIVINE_SHIELD);
          return false;
        } else {
          healthAttr.decrease(damage);
          return true;
        }
      }

      @Override
      public boolean canDamage() {
        return attackAttr.getVal() > 0;
      }

      @Override
      public boolean isDead() {
        return healthAttr.getVal() <= 0;
      }

      @Override
      public boolean canMove() {
        return movePoints.getVal() > 0 &&
            BooleanAttribute.isAbsentOrOff(booleanMechanics.get(ConstMechanic.CHARGE));
      }

      @Override
      public int getHealthLoss() {
        return getHealthUpperAttr().getVal() - getHealthAttr().getVal();
      }

      @Override
      public void endTurn() {
        this.movePoints.endTurn();
      }

      @Override
      public void startTurn() {

      }
    };

    if (!mechanics.containsKey(ConstMechanic.CHARGE)) {
      // Minion with no charge ability waits until next turn to move.
      // TODO: also when a minion switch side due to TAKE_CONTROLL effect, the CHARGE also apply
      // the right after it switches side.
      minion.getAttackMovePoints().buff.temp.decrease(minion.getAttackMovePoints().getVal());
    }

    return minion;
  }

}