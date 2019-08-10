package java_zookeeper.blackjack.game.player;

import java.util.ArrayList;
import java.util.List;

public abstract class Dealer extends Player {

	private List<Player> listOfPlayers = new ArrayList<>();

	public Dealer(final String mesa, final Integer key) {
		super(mesa, mesa, key);
	}

	public abstract void checkQueue();

	public abstract void distributeCards();
}
