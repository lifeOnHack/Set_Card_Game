package bguspl.set.ex;

import bguspl.set.Env;
import bguspl.set.UserInterfaceImpl;

import java.util.Arrays;
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
    private Integer[][] playersSets;

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

        this.playersSets = new Integer[env.config.players][3];
        if (playersSets[0] == null) {
            System.err.println("null playersSets");
        }
    }

    public Integer[] getSTC(){
        return slotToCard;
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
        // check null
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
        cardToSlot[slotToCard[slot]] = null;
        slotToCard[slot] = null;
        env.ui.removeCard(slot);
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
    public void placeToken(int pId, int slot) {
        Integer[] pTokens = playersSets[pId];
        for (int i = 0; i < MAX_TOKENS; i++) {
            if (NOT_PLACED == pTokens[i]) {
                synchronized(pTokens){
                    pTokens[i] = slot;
                }
                env.ui.placeToken(pId, slot);
                break;
            }
        }
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
            if (slot == pTokens[i]) {
                synchronized(pTokens){
                    pTokens[i] = NOT_PLACED;
                }
                env.ui.removeToken(pId, slot);
                return true;
            }
        }
        return false;
    }

    /*
     * deciding if to place or remove a token
     */
    public int setTokIfNeed(int pId, int slot) { 
        if(removeToken(pId, slot)){
            return -1;
        }
        placeToken(pId, slot);
        return 0;
    }

    /*
     * clear players tokens
     */
    public void reset() {
        for(int i = 0; i < playersSets.length; i++){
            resetPlayer(playersSets[i]);
        }
    }

    public void resetPlayer(Integer[] pTokens){
        synchronized(pTokens){
            for(int j = 0; j < MAX_TOKENS; j++){
                pTokens[j] = NOT_PLACED;
            }
        }
    }
}