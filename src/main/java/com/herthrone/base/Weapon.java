package com.herthrone.base;

import com.herthrone.objects.IntAttribute;

/**
 * Created by yifeng on 4/2/16.
 */
public interface Weapon extends Card {

  int use();

  IntAttribute getDurabilityAttr();

  IntAttribute getAttackAttr();
}
