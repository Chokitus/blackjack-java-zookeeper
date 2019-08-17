package java_zookeeper.blackjack.game.deck;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import java_zookeeper.blackjack.game.deck.card.Card;

public class DeckGenerator {

	private static DeckGenerator gen;

	private List<Integer> listOfCards = new ArrayList<>();
	private Random rng = new Random();

	private DeckGenerator() {
		/* Vazio - Singleton */
	}

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

	public void reset() {
		this.listOfCards.clear();
	}

	public static DeckGenerator getGenerator() {
		if (DeckGenerator.gen == null) {
			DeckGenerator.gen = new DeckGenerator();
		}
		return DeckGenerator.gen;
	}
}
