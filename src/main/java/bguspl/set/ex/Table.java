package bguspl.set.ex;

import bguspl.set.Env;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class contains the data that is visible to the player.
 *
 * @inv slotToCard[x] == y iff cardToSlot[y] == x
 */
public class Table {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Mapping between a slot and the card placed in it (null if none).
     */
    protected final Integer[] slotToCard; // card per slot (if any)

    /**
     * Mapping between a card and the slot it is in (null if none).
     */
    protected final Integer[] cardToSlot; // slot per card (if any)
    private Integer[][] playersSets;// save players sets by slot
    private final int NOT_PLACED = -1;
    private final int MAX_TOKENS = 3;

    /**
     * Constructor for testing.
     *
     * @param env        - the game environment objects.
     * @param slotToCard - mapping between a slot and the card placed in it (null if
     *                   none).
     * @param cardToSlot - mapping between a card and the slot it is in (null if
     *                   none).
     */
    public Table(Env env, Integer[] slotToCard, Integer[] cardToSlot) {

        this.env = env;
        this.slotToCard = slotToCard;
        this.cardToSlot = cardToSlot;
        this.playersSets = new Integer[env.config.players][MAX_TOKENS];
        reset();
    }

    public Integer[] getSTC() {
        return slotToCard;
    }

    public Integer[] getCTS() {
        return cardToSlot;
    }

    public int[] getPSet(Player p) {
        int[] set = new int[MAX_TOKENS];
        int curTockens = 0;
        boolean falseSet = false;
        synchronized (playersSets[p.id]) {
            synchronized (slotToCard) {
                for (int i = 0; i < MAX_TOKENS; i++) {
                    if (playersSets[p.id][i] != -1 && slotToCard[playersSets[p.id][i]] != null) {
                        curTockens++;
                        set[i] = slotToCard[playersSets[p.id][i]];
                    } else {
                        falseSet = true;
                    }
                }
            }
        }
        p.fixTockens(curTockens);
        return falseSet ? null : set;
    }

    /**
     * Constructor for actual usage.
     *
     * @param env - the game environment objects.
     */
    public Table(Env env) {
        this(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
    }

    /**
     * This method prints all possible legal sets of cards that are currently on the
     * table.
     */
    public void hints() {
        List<Integer> deck = Arrays.stream(slotToCard).filter(Objects::nonNull).collect(Collectors.toList());
        env.util.findSets(deck, Integer.MAX_VALUE).forEach(set -> {
            StringBuilder sb = new StringBuilder().append("Hint: Set found: ");
            List<Integer> slots = Arrays.stream(set).mapToObj(card -> cardToSlot[card]).sorted()
                    .collect(Collectors.toList());
            int[][] features = env.util.cardsToFeatures(set);
            System.out.println(
                    sb.append("slots: ").append(slots).append(" features: ").append(Arrays.deepToString(features)));
        });
    }

    /**
     * Count the number of cards currently on the table.
     *
     * @return - the number of cards on the table.
     */
    public int countCards() {
        int cards = 0;
        for (Integer card : slotToCard)
            if (card != null)
                ++cards;
        return cards;
    }

    /**
     * Places a card on the table in a grid slot.
     * 
     * @param card - the card id to place in the slot.
     * @param slot - the slot in which the card should be placed.
     *
     * @post - the card placed is on the table, in the assigned slot.
     */
    public void placeCard(int card, int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {
        }

        cardToSlot[card] = slot;
        slotToCard[slot] = card;
        env.ui.placeCard(card, slot);
    }

    /**
     * Removes a card from a grid slot on the table.
     * 
     * @param slot - the slot from which to remove the card.
     */
    public void removeCard(int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {
        }
        synchronized (slotToCard) {
            if (slotToCard[slot] != null) {
                cardToSlot[slotToCard[slot]] = null;
                slotToCard[slot] = null;
                env.ui.removeCard(slot);
            }
        }
    }

    public void removeByCard(int card) {
        removeCard(cardToSlot[card]);
    }

    /**
     * Places a player token on a grid slot.
     * 
     * @param player - the player the token belongs to.
     * @param slot   - the slot on which to place the token.
     */
    public int placeToken(int pId, int slot) {
        Integer[] pTokens = playersSets[pId];
        synchronized (slotToCard) {
            if (slotToCard[slot] != null) {
                for (int i = 0; i < MAX_TOKENS; i++) {
                    synchronized (pTokens) {
                        if (NOT_PLACED == pTokens[i]) {
                            pTokens[i] = slot;
                            env.ui.placeToken(pId, slot);
                            return 1;
                        }
                    }
                }
            }
        }
        return 0;
    }

    /**
     * Removes a token of a player from a grid slot.
     * 
     * @param player - the player the token belongs to.
     * @param slot   - the slot from which to remove the token.
     * @return - true iff a token was successfully removed.
     */
    public boolean removeToken(int pId, int slot) {
        Integer[] pTokens = playersSets[pId];
        for (int i = 0; i < MAX_TOKENS; i++) {
            synchronized (pTokens) {
                if (slot == pTokens[i]) {
                    pTokens[i] = NOT_PLACED;
                    env.ui.removeToken(pId, slot);
                    return true;
                }
            }
        }
        return false;
    }

    /*
     * deciding if to place or remove a token
     */
    public int setTokIfNeed(int pId, int slot) {
        if (removeToken(pId, slot)) {
            return -1;
        }
        return placeToken(pId, slot);
    }

    /*
     * clear players tokens
     */
    public void reset() {
        for (int i = 0; i < playersSets.length; i++) {
            resetPlayer(playersSets[i], i);
        }
    }

    private void resetPlayer(Integer[] pTokens, int id) {
        synchronized (pTokens) {
            for (int j = 0; j < MAX_TOKENS; j++) {
                if (pTokens[j] != null && pTokens[j] != NOT_PLACED) {
                    env.ui.removeToken(id, pTokens[j]);
                }
                pTokens[j] = NOT_PLACED;
            }
        }
        // System.out.println("player" + id + " reset");
    }

    public void resetPlayer(int pId) {
        resetPlayer(playersSets[pId], pId);
    }

    public Integer[] getPlyrTok(int pId) {
        return playersSets[pId];
    }

    public void removeAtPoint(int c1, int c2, int c3, Player[] players, LinkedList<Integer> requests) {
        for (int i = 0; i < playersSets.length; i++) {
            Boolean isNeedRemove = false;
            synchronized (playersSets[i]) {// so the player cant add\remove tokens
                for (int j = 0; j < playersSets[i].length; j++) {
                    if (playersSets[i][j] == cardToSlot[c1]) {
                        if (removeToken(i, cardToSlot[c1])) {
                            players[i].tokenGotRemoved();
                            isNeedRemove = true;
                        }
                    } else if (playersSets[i][j] == cardToSlot[c2]) {
                        if (removeToken(i, cardToSlot[c2])) {
                            players[i].tokenGotRemoved();
                            isNeedRemove = true;
                        }
                    } else if (playersSets[i][j] == cardToSlot[c3]) {
                        if (removeToken(i, cardToSlot[c3])) {
                            players[i].tokenGotRemoved();
                            isNeedRemove = true;
                        }
                    }
                }
            }
            if (isNeedRemove) {
                rmvReq(players[i], requests);
            }
        }
        System.out.println("remove at point done");
    }

    private void rmvReq(Player p, LinkedList<Integer> requests) {
        Integer id = p.id;
        synchronized (requests) {
            if (requests.contains(p.id)) {
                System.out.println("remove at point p" + p.id);
                requests.remove(id);
            }
            p.myState.setState(STATES.FREE_TO_GO);
            p.myState.wakeup();
        }
    }
}
