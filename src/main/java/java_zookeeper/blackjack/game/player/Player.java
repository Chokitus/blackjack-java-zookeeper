package java_zookeeper.blackjack.game.player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.zookeeper.KeeperException;

import java_zookeeper.blackjack.game.deck.card.Card;
import java_zookeeper.blackjack.zookeeper.ZookeeperService;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j;

@RequiredArgsConstructor
@Log4j
public class Player {

	@Getter
	private int score = 0;

	@Getter
	private boolean desistir;

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
	protected int currentMoney = 300;

	@Getter
	@Setter
	private List<Card> drawnCards;

	@Getter
	List<Card> hand = new ArrayList<>();

	public void addToHand(final Card card) {
		List<Card> aces = this.hand.stream().filter(c -> "A".equals(c.getValor())).collect(Collectors.toList());
		List<Card> notAces = this.hand.stream().filter(c -> !"A".equals(c.getValor())).collect(Collectors.toList());
		if (!this.hand.contains(card)) {
			this.hand.add(card);
			if ("A".equals(card.getValor())) {
				aces.add(card);
			} else {
				notAces.add(card);
			}
			this.score = this.calculateScore(notAces, aces);
		}
	}

	private int calculateScore(final List<Card> notAces, final List<Card> aces) {
		this.score = 0;
		notAces.forEach(card -> this.score += card.getNumericValue(this.score));
		aces.forEach(card -> this.score += card.getNumericValue(this.score));
		return this.score;
	}

	public void printHand(final boolean debug) {
		if (debug) {
			Player.log.info("Meu nome é " + this.getName());
		} else {
			Player.log.info("Suas cartas são:");
		}
		for (Card card : this.hand) {
			Player.log.info(card);
		}
	}

	public void dobrarAposta() {
		this.aposta *= 2;
	}

	public void setToCurrentMoney(final int valor) throws KeeperException, InterruptedException {
		this.currentMoney += valor;
		ZookeeperService.getInstance().setMoneyToPlayer(this, this.currentMoney);
	}

	public void newRound() {
		this.aposta = 0;
		this.score = 0;
		this.desistir = false;
		this.hand.clear();
	}

	public void setInitialMoney(final Integer moneyFromPlayer) {
		this.currentMoney = moneyFromPlayer.intValue();
	}

	public void desistir() {
		this.desistir = true;
	}

}
