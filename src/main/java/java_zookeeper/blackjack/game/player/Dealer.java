package java_zookeeper.blackjack.game.player;

import java.util.ArrayList;
import java.util.List;

import java_zookeeper.blackjack.game.deck.DeckGenerator;
import java_zookeeper.blackjack.game.deck.card.Card;
import lombok.Getter;

public abstract class Dealer extends Player {

	@Getter
	private List<Player> listOfPlayers;

	private DeckGenerator deckGen = DeckGenerator.getGenerator();

	public Dealer(final String nome, final String mesa) {
		super(nome, mesa);
	}

	public Card getNewCard() {
		return this.deckGen.nextCard();
	}

	public void registerPlayers(final List<String> listOfPlayers) {
		this.listOfPlayers = new ArrayList<>();
		for (String playerName : listOfPlayers) {
			Player player = new Player(playerName, this.getMesa());
			player.setFullName("/" + this.getMesa() + "/" + playerName);
			this.listOfPlayers.add(player);
		}
	}

}
