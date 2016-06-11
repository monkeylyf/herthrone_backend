package com.herthrone.constant;


import java.util.Map;

/**
 * Created by yifengliu on 5/13/16.
 */
public class Constant {

  // TODO: There are used in switch/case so cannot be enum type.
  // However, due to the silly modeling on the effect config yaml, cannot
  // use Enum to replace the String(yaml field might be mapped to different Enum
  // type, causing type conversion impossible during load config. Better to
  // think about it later(better config file structure?)
  public static final String ARMOR = "armor";
  public static final String ATTACK = "attack";
  public static final String CRYSTAL = "crystal";
  public static final String BOARD = "board";
  public static final String HEROPOWER = "heropower";
  public static final String DECK_SIZE = "deck_size";
  public static final String HAND_SIZE = "hand_size";
  public static final String HAND = "hand";
  public static final String SECRET_SIZE = "secret_size";
  public static final String SECRET = "secret";
  public static final String HEALTH = "health";
  public static final String CARD_NAME = "card_name";
  public static final String TYPE = "type";
  public static final String HERO = "hero";
  public static final String WEAPON = "weapon";
  public static final String DESCRIPTION = "description";
  public static final String MOVE_POINTS = "move_points";
  public static final String HEALTH_UPPER_BOUND = "health_upper_bound";

  public static String upperCaseValue(final Map map, final String key) {
    final String value = (String) map.get(key);
    return value.toUpperCase();
  }

}
