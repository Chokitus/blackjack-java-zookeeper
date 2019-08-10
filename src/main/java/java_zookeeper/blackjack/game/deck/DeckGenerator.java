package java_zookeeper.blackjack.game.deck;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import java_zookeeper.blackjack.game.deck.card.Card;

public class DeckGenerator {

	private List<Integer> listOfCards = new ArrayList<>();
	private Random rng = new Random();

	public Card nextCard() {
		int rnged = this.getNewCard();
		Card card = Card.getCard(rnged);
		this.listOfCards.add(rnged);
		return card;
	}

	private int getNewCard() {
		int rnged = this.rng.nextInt(52) + 1;
		while (this.listOfCards.contains(rnged)) {
			rnged = this.rng.nextInt(52) + 1;
		}
		return rnged;
	}
}
