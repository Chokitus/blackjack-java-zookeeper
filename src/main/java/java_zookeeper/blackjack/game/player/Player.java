package java_zookeeper.blackjack.game.player;

import java.util.ArrayList;
import java.util.List;

import java_zookeeper.blackjack.game.deck.card.Card;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Player {

	private int score = 0;

	@NonNull
	@Getter
	private String mesa;

	@NonNull
	@Getter
	private String name;

	@NonNull
	@Getter
	private Integer key;

	@Getter
	List<Card> hand = new ArrayList<>();

	public void addToHand(final Card card) {
		this.hand.add(card);
		this.score += card.getNumericValue(this.score);
	}

}
