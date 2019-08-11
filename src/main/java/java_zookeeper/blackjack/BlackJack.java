package java_zookeeper.blackjack;

import java.io.IOException;

import org.apache.zookeeper.KeeperException;

import java_zookeeper.blackjack.game.player.Dealer;
import java_zookeeper.blackjack.game.player.Player;
import java_zookeeper.blackjack.game.service.BlackjackGameService;
import java_zookeeper.blackjack.zookeeper.ZookeeperPlayerRegister;
import java_zookeeper.blackjack.zookeeper.ZookeeperService;

public class BlackJack {

	public static void main(final String[] args) throws IOException, InterruptedException, KeeperException {
		ZookeeperService.createInstance("localhost:2181");
		if (args[0].equals("dealer")) {
			Dealer dealer = ZookeeperPlayerRegister.registerDealer("001", "NomeDoDealer", 5);
			new BlackJack().playDealerGame(dealer);
		} else {
			Player player = ZookeeperPlayerRegister.registerPlayer("001", "NomeDoPlayer");
			new BlackJack().playPlayerGame(player);
		}
	}

	private void playPlayerGame(final Player player) throws KeeperException, InterruptedException {
		BlackjackGameService.bet(player);
	}

	private void playDealerGame(final Dealer dealer) throws KeeperException, InterruptedException {
		/**
		 * Primeiro passo: Pedir apostas
		 */
		for (Player player : dealer.getListOfPlayers()) {
			BlackjackGameService.askForBet(player);
		}
	}
}
