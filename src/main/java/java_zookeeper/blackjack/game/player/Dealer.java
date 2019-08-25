package java_zookeeper.blackjack.game.player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import java_zookeeper.blackjack.game.deck.DeckGenerator;
import java_zookeeper.blackjack.game.deck.card.Card;
import lombok.Getter;

public abstract class Dealer extends Player {

	@Getter
	private List<Player> listOfPlayers = new ArrayList<>();

	private static Player tempDealer = new Player("temp", "temp");

	private DeckGenerator deckGen = DeckGenerator.getGenerator();

	public Dealer(final String nome, final String mesa) {
		super(nome, mesa);
		this.currentMoney = 20000;
	}

	public Card getNewCard() {
		return this.deckGen.nextCard();
	}

	public void registerPlayers(final List<String> listOfPlayers) {
		listOfPlayers.stream().filter(player -> !"dealer".equals(player)).forEach(playerName -> {
			Player player = new Player(playerName, this.getMesa());
			player.setFullName("/blackjack/" + this.getMesa() + "/" + playerName);
			this.listOfPlayers.add(player);
		});
	}

	public static int calculateScore(final List<Card> dealerCards) {
		for (Card card : dealerCards) {
			Dealer.tempDealer.addToHand(card);
		}
		int dealerScore = Dealer.tempDealer.getScore();
		Dealer.tempDealer.newRound();
		return dealerScore;
	}

	@Override
	public void newRound() {
		super.newRound();
		this.deckGen.newRound();
	}

	public List<Player> getAndRemovePlayerWithoutMoney() {
		List<Player> moneylessPlayers = this.listOfPlayers.stream().filter(p -> p.getCurrentMoney() <= 0).collect(Collectors.toList());
		this.listOfPlayers = this.listOfPlayers.stream().filter(p -> p.getCurrentMoney() > 0).collect(Collectors.toList());
		return moneylessPlayers;
	}

}
