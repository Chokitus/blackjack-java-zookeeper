package java_zookeeper.blackjack.game.player;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public abstract class Dealer extends Player {

	@Getter
	private List<Player> listOfPlayers;

	public Dealer(final String nome, final String mesa) {
		super(nome, mesa);
	}

	public void registerPlayers(final List<String> listOfPlayers) {
		this.listOfPlayers = new ArrayList<>();
		for (String playerName : listOfPlayers) {
			Player player = new Player(playerName, this.getMesa());
			player.setFullName("/" + this.getMesa() + "/" + playerName);
			this.listOfPlayers.add(player);
		}
	}

	public abstract void checkQueue();

	public abstract void distributeCards();
}
