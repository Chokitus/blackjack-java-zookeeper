package java_zookeeper.blackjack.game.player;

import java.util.ArrayList;
import java.util.List;

import java_zookeeper.blackjack.game.deck.card.Card;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j;

@RequiredArgsConstructor
@Log4j
public class Player {

	private int score = 0;

	@NonNull
	@Getter
	@Setter
	private String name;

	@Setter
	@Getter
	private String fullName = "";

	@NonNull
	@Getter
	private String mesa;

	@Getter
	@Setter
	private Integer aposta;

	@Getter
	@Setter
	private List<Card> drawnCards;

	@Getter
	List<Card> hand = new ArrayList<>();

	public void addToHand(final Card card) {
		this.hand.add(card);
		this.score += card.getNumericValue(this.score);
	}

	public void printHand() {
		for (Card card : this.hand) {
			Player.log.info(card);
			if (card.toString().contains("  de")) {
				System.out.println("!");
			}
		}
	}

}
