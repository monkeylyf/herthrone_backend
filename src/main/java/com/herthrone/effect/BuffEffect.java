package com.herthrone.effect;

import com.herthrone.base.Effect;
import com.herthrone.constant.ConstEffectType;
import com.herthrone.object.IntAttribute;
import com.herthrone.object.Value;

/**
 * Created by yifeng on 4/28/16.
 */
public class BuffEffect implements Effect {

  private final IntAttribute attr;
  private final int buffDelta;
  private final boolean permanent;

  public BuffEffect(final IntAttribute attr, final int buffDelta, final boolean permanent) {
    this.attr = attr;
    this.buffDelta = buffDelta;
    this.permanent = permanent;
  }

  public BuffEffect(final IntAttribute attr, final int setToValue) {
    this.attr = attr;
    this.buffDelta = setToValue - attr.value();
    this.permanent = true;
  }

  @Override
  public ConstEffectType effectType() {
    return ConstEffectType.BUFF;
  }

  @Override
  public void act() {
    final Value value = permanent ? attr.buff.permanentBuff : attr.buff.temporaryBuff;
    value.increase(buffDelta);
  }
}