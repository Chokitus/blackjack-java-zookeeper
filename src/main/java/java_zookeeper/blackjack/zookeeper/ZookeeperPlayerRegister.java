package java_zookeeper.blackjack.zookeeper;

import java.io.IOException;

import org.apache.zookeeper.KeeperException;

import java_zookeeper.blackjack.game.player.Dealer;
import java_zookeeper.blackjack.game.player.DealerImpl;
import java_zookeeper.blackjack.game.player.Player;

public class ZookeeperPlayerRegister {

	public static Player registerPlayer(final String mesa, final String name, final int key)
			throws KeeperException, InterruptedException, IOException {
		Player player = new Player(mesa, name, key);
		ZookeeperService.getInstance().createNewPlayerNode(player, key);
		return null;
	}

	public static Player registerDealer(final String mesa, final String name, final int key)
			throws KeeperException, InterruptedException, IOException {

		Dealer dealer = new DealerImpl(name, key);
		String mesaName = ZookeeperService.getInstance(null).createNewMesa(mesa);

		ZookeeperService.getInstance().registerPlayers(mesaName);

		System.out.println("SOU O DEALER E ACORDEI NESTA PORRA");

		return dealer;
	}

}
