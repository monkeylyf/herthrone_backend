package com.herthrone.game;

import com.google.common.base.Enums;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.herthrone.base.Card;
import com.herthrone.base.Creature;
import com.herthrone.base.Hero;
import com.herthrone.base.Minion;
import com.herthrone.base.Round;
import com.herthrone.base.Secret;
import com.herthrone.base.View;
import com.herthrone.configuration.ConfigLoader;
import com.herthrone.constant.ConstHero;
import com.herthrone.constant.ConstMinion;
import com.herthrone.constant.ConstSecret;
import com.herthrone.constant.ConstSpell;
import com.herthrone.constant.ConstTarget;
import com.herthrone.constant.ConstTrigger;
import com.herthrone.constant.ConstWeapon;
import com.herthrone.constant.Constant;
import com.herthrone.factory.EffectFactory;
import com.herthrone.factory.HeroFactory;
import com.herthrone.factory.MinionFactory;
import com.herthrone.factory.SecretFactory;
import com.herthrone.factory.SpellFactory;
import com.herthrone.factory.TriggerFactory;
import com.herthrone.factory.WeaponFactory;
import com.herthrone.object.Replay;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

public class Side implements Round, View {

  private static Logger logger = Logger.getLogger(Side.class.getName());
  private static final int SHUFFLE_ITERATION = 5;
  public final Hero hero;
  public final Container<Card> hand;
  public final Container<Card> deck;
  public final Container<Minion> board;
  public final Container<Secret> secrets;
  public final List<Enum> playedCards;
  public final Replay replay;
  private final EffectQueue effectQueue;
  private final IntSupplier idGenerator;
  private int fatigue;
  private Side foeSide;

  private Side(final Hero hero, final List<Enum> cards, final EffectQueue effectQueue,
               final IntSupplier idGenerator) {
    // Init containers.
    final int handCapacity = Integer.parseInt(
        ConfigLoader.getResource().getString(Constant.HAND_MAX_SIZE));
    this.hand = new Container<>(handCapacity);
    final int boardCapacity = Integer.parseInt(
        ConfigLoader.getResource().getString(Constant.BOARD_MAX_CAPACITY));
    this.board = new Container<>(boardCapacity);
    final int deckCapacity = Integer.parseInt(
        ConfigLoader.getResource().getString(Constant.DECK_MAX_CAPACITY));
    this.secrets = new Container<>();
    this.deck = new Container<>(deckCapacity);
    populateDeck(cards);

    this.hero = hero;
    bind(hero);
    bind(hero.getHeroPower());
    this.replay = new Replay();
    this.fatigue = 0;
    this.effectQueue = effectQueue;
    this.idGenerator = idGenerator;
    this.playedCards = new ArrayList<>();
  }

  public void bind(final Card card) {
    card.binder().bind(this);
  }

  static Side createSidePair(final ConstHero ownHero, final List<Enum> ownDeck,
                             final ConstHero foeHero, final List<Enum> foeDeck) {
    final IntSupplier sequenceIdGenerator = new IntSupplier() {
      private int id = 0;

      @Override
      public int getAsInt() {
        ++id;
        return id;
      }
    };
    final EffectQueue effectQueue = new EffectQueue();
    final Side ownSide = new Side(
        HeroFactory.create(ownHero), ownDeck, effectQueue, sequenceIdGenerator);
    final Side foeSide = new Side(
        HeroFactory.create(foeHero), foeDeck, effectQueue, sequenceIdGenerator);
    ownSide.foeSide = foeSide;
    foeSide.foeSide = ownSide;
    return ownSide;
  }

  private void populateDeck(final List<Enum> cards) {
    cards.forEach(cardName -> {
      final Card card = createCardInstance(cardName);
      deck.add(card);
      bind(card);
    });

    // Shuffle the deck.
    for (int i = 0; i < SHUFFLE_ITERATION; ++i) {
      deck.shuffle();
    }
  }

  private static Card createCardInstance(final Enum cardName) {
    final String name = cardName.toString();

    Optional<ConstMinion> constMinion = Enums.getIfPresent(ConstMinion.class, name);
    if (constMinion.isPresent()) {
      return MinionFactory.create(constMinion.get());
    }

    Optional<ConstWeapon> constWeapon = Enums.getIfPresent(ConstWeapon.class, name);
    if (constWeapon.isPresent()) {
      return WeaponFactory.create(constWeapon.get());
    }

    Optional<ConstSpell> constSpell = Enums.getIfPresent(ConstSpell.class, name);
    if (constSpell.isPresent()) {
      return SpellFactory.create(constSpell.get());
    }

    Optional<ConstSecret> constSecret = Enums.getIfPresent(ConstSecret.class, name);
    if (constSecret.isPresent()) {
      return SecretFactory.create(constSecret.get());
    }

    throw new RuntimeException(String.format("Unknown card %s", name));
  }

  public void takeFatigueDamage() {
    fatigue += 1;
    logger.debug(String.format("Increase fatigue to %d", fatigue));
    hero.takeDamage(fatigue);
  }

  public List<Creature> allCreatures() {
    List<Creature> allCreatures = new ArrayList<>();
    allCreatures.add(hero);
    for (int i = 0; i < board.size(); ++i) {
      allCreatures.add(board.get(i));
    }

    return allCreatures;
  }

  public EffectQueue getEffectQueue() {
    return effectQueue;
  }

  @Override
  public void endTurn() {
    replay.endTurn();
    hero.endTurn();
    board.stream().sorted(EffectFactory.compareBySequenceId).forEach(Round::endTurn);
    TriggerFactory.triggerByBoard(board.stream(), this, ConstTrigger.ON_END_TURN);
    TriggerFactory.triggerByBoard(getFoeSide().board.stream(), this, ConstTrigger.ON_FOE_END_TURN);
  }

  @Override
  public void startTurn() {
    replay.startTurn();
    hero.startTurn();
    board.stream().forEach(Round::startTurn);
    TriggerFactory.triggerByBoard(getFoeSide().board.stream(), this, ConstTrigger.ON_FOE_START_TURN);
    TriggerFactory.triggerByBoard(board.stream(), this, ConstTrigger.ON_START_TURN);
  }

  public Side getFoeSide() {
    return foeSide;
  }

  public void setSequenceId(final Minion minion) {
    final int sequenceId = idGenerator.getAsInt();
    logger.debug("Set ID " + sequenceId + " to minion " + minion);
    minion.setSequenceId(sequenceId);
  }

  @Override
  public Map<String, String> view() {
    final ImmutableMap.Builder<String, String> viewBuilder = ImmutableMap.builder();

    final String ownPrefix = ConstTarget.OWN.toString() + ":";
    final Map<String, String> ownSideView = getOwnSideView();
    for (Map.Entry<String, String> entry : ownSideView.entrySet()) {
      viewBuilder.put(ownPrefix + entry.getKey(), entry.getValue());
    }

    final String foePrefix = ConstTarget.FOE.toString() + ":";
    final Map<String, String> foeSideView = getFoeSideView();
    for (Map.Entry<String, String> entry : foeSideView.entrySet()) {
      viewBuilder.put(foePrefix + entry.getKey(), entry.getValue());
    }

    return viewBuilder.build();
  }

  private Map<String, String> getOwnSideView() {
    final ImmutableMap.Builder<String, String> ownSideBuilder = buildNoHiddenSideView(this);
    ownSideBuilder.put(Constant.HAND, hand.view().toString());
    ownSideBuilder.put(Constant.SECRET, secrets.view().toString());
    return ownSideBuilder.build();
  }

  private Map<String, String> getFoeSideView() {
    return buildNoHiddenSideView(foeSide)
        .put(Constant.HAND, Integer.toString(foeSide.hand.size()))
        .put(Constant.SECRET, Integer.toString(foeSide.secrets.size()))
        .build();
  }

  private static ImmutableMap.Builder<String, String> buildNoHiddenSideView(final Side side) {
    final ImmutableMap.Builder<String, String> sideViewBuilder = ImmutableMap.builder();
    // Add here as part of view.
    sideViewBuilder.put(Constant.HERO, side.hero.view().toString());
    // Add deck count as part of view.
    sideViewBuilder.put(Constant.DECK_SIZE, Integer.toString(side.deck.size()));
    // Add board as part of view.
    for (int i = 0; i < side.board.size(); ++i) {
      sideViewBuilder.put(Constant.BOARD + i, side.board.get(i).view().toString());
    }
    // Add crystals as part of view.
    sideViewBuilder.put(Constant.CRYSTAL, side.hero.manaCrystal().toString());
    // Add hero power as part of view.
    sideViewBuilder.put(Constant.HERO_POWER, side.hero.getHeroPower().view().toString());

    return sideViewBuilder;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add(Constant.HERO, hero)
        .add(Constant.BOARD, board)
        .add(Constant.SECRET, secrets)
        .toString();
  }

}
