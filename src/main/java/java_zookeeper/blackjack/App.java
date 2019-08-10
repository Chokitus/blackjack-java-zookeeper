package java_zookeeper.blackjack;

import java.io.IOException;

import org.apache.zookeeper.KeeperException;

import java_zookeeper.blackjack.game.player.Player;
import java_zookeeper.blackjack.zookeeper.ZookeeperPlayerRegister;
import java_zookeeper.blackjack.zookeeper.ZookeeperService;

public class App {

	public static void main(final String[] args) throws IOException, InterruptedException, KeeperException {
		// ZookeeperService instance = ZookeeperService.getInstance();
		// instance.connect("localhost:2181");
		// ZooKeeper zk = instance.getZooKeeper();
		//
		// DeckGenerator gen = new DeckGenerator();
		// Card card = gen.nextCard();
		//
		// String create = zk.create("/newnode1", card.serialize(),
		// Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
		// byte[] data = zk.getData(create, false, null);
		//
		// Card cardFromBytes = Card.cardFromBytes(data);
		//
		// System.out.println("a");

		ZookeeperService aaa = ZookeeperService.getInstance("localhost:2181");
		if (args[0].equals("dealer")) {
			// aaa.produce();
			Player dealer = ZookeeperPlayerRegister.registerDealer("001", "Meu nome", 0);
		} else {
			// aaa.consume();
			Player me = ZookeeperPlayerRegister.registerPlayer("001", "Meu nome", 10);
		}

	}
}
