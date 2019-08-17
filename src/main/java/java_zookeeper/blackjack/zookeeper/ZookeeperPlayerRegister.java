package java_zookeeper.blackjack.zookeeper;

import java.io.IOException;
import java.util.List;

import org.apache.zookeeper.KeeperException;

import java_zookeeper.blackjack.game.player.Dealer;
import java_zookeeper.blackjack.game.player.DealerImpl;
import java_zookeeper.blackjack.game.player.Player;

public class ZookeeperPlayerRegister {

	public static Player registerPlayer(final String mesa, final String name) throws KeeperException, InterruptedException, IOException {
		Player player = new Player(name, mesa);
		ZookeeperService.getInstance().createNewPlayerNodeAndWaitTillDealerCall(player);

		// DONE: Player entra na barreira, esperando outros 4 players entrarem.

		return player;
	}

	public static Dealer registerDealer(final String mesa, final String name, final int expectedPlayers)
			throws KeeperException, InterruptedException, IOException {

		Dealer dealer = new DealerImpl(name, mesa);
		String mesaName = ZookeeperService.getInstance(null).createNewMesa(mesa);

		dealer.setFullName("/" + mesa + "/dealer");
		ZookeeperService.getInstance().createPlayerNode(dealer, new byte[0], true);

		/*
		 * Aguarda até pelo menos @expectedPlayers players entrarem na mesa.
		 */
		List<String> players = ZookeeperService.getInstance().waitUntilTableIsFull(mesaName, expectedPlayers);
		dealer.registerPlayers(players);

		return dealer;
	}

}
