package java_zookeeper.blackjack.zookeeper;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;

import java_zookeeper.blackjack.game.deck.card.Card;
import java_zookeeper.blackjack.game.player.Player;
import lombok.Getter;

public class ZookeeperService implements Watcher, Closeable {

	@Getter
	public ZooKeeper zk = null;

	public static Object mutex = new Object();

	private static ZookeeperService instance = null;

	private ZookeeperService(final String host) {
		try {
			this.zk = new ZooKeeper(host, 3000, this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void createInstance(final String host) {
		if (ZookeeperService.instance == null) {
			ZookeeperService.instance = new ZookeeperService(host);
		}
	}

	public static ZookeeperService getInstance(final String host) {
		ZookeeperService.createInstance(host);
		return ZookeeperService.instance;
	}

	public static ZookeeperService getInstance() {
		return ZookeeperService.getInstance(null);
	}

	@Override
	public void close() {
		try {
			this.zk.close();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * <p>
	 * Cria uma nova mesa no Jogo, sendo seu nome conhecido entre todos os
	 * jogadores. Este método também adiciona um Watch no nó criado, a ser usado
	 * pelo Dealer, conforme necessidade descrita em
	 * {@link #createNewPlayerNode(String, Player, int)}
	 * </p>
	 *
	 * @param mesa
	 * @return
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public synchronized String createNewMesa(final String mesa) throws KeeperException, InterruptedException {
		return this.zk.create("/" + mesa, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
	}

	/**
	 * <p>
	 * Cria um novo Player na mesa, passando o nome da mesa, o player e a sua
	 * chave. A ideia é que um Player espere o Dealer criar a mesa, e se
	 * registre nesta mesa. Ao criar o nó, o Player deixará a sua chave no nó. O
	 * Dealer deverá ter um Watch na mesa, pois assim que um novo Player criar
	 * um nó, o Dealer deverá olhar para a chave da mesma, memorizá-la (num
	 * HashMap), e sobrescrever a chave. Assim, fica acordado uma chave
	 * (presumidamente única) para cada jogador.
	 * </p>
	 * <p>
	 * Assim que pelo menos <b> n jogadores </> entrarem, o jogo começa.
	 * </p>
	 *
	 * @param mesa
	 * @param player
	 * @param key
	 * @return
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public String createNewPlayerNode(final Player player, final int key) throws KeeperException, InterruptedException {
		if (this.zk.exists("/" + player.getMesa(), false) == null) {
			throw new IllegalStateException("Um dealer deve ser decidido antes que os players se registrem.");
		}
		return this.zk.create("/" + player.getMesa() + "/" + player.getName(), String.valueOf(key).getBytes(), Ids.OPEN_ACL_UNSAFE,
				CreateMode.PERSISTENT_SEQUENTIAL);
	}

	/**
	 * Encontra o nó contendo o nome do player (que estará em "/" +
	 * player.getMesa() + "/" + player.getNome()) e seta os dados como a carda
	 * desejada. A verdade é que não é um create, mas sim um set, mas como esta
	 * parte ainda não está feita, fica como TODO
	 *
	 * @param player
	 * @param card
	 * @return
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public String enviarCardParaPlayer(final Player player, final Card card) throws KeeperException, InterruptedException {
		return this.createZNode(player, card.serialize(player));
	}

	public String createZNode(final Player player, final byte[] data) throws KeeperException, InterruptedException {
		return this.zk.create("/" + player.getMesa() + "/" + player.getName(), data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
	}

	@Override
	public void process(final WatchedEvent event) {
		synchronized (ZookeeperService.mutex) {
			// System.out.println("Process: " + event.getType());
			ZookeeperService.mutex.notify();
		}
	}

	/**
	 * Espera algum player se registrar para começar o processo de registro de
	 * seu lado. O processo de Registro do Dealer é como já foi dito: Pegue o
	 * nome e chave do Player (que estará nos dados do nó), alimente um
	 * HashMap<String, Integer> com Nome -> Chave, e limpe o nó (sete os dados
	 * como vazio).
	 *
	 * @param mesa
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public void registerPlayers(final String mesa) throws KeeperException, InterruptedException {
		synchronized (ZookeeperService.mutex) {
			boolean empty = true;
			while (empty) {
				List<String> children = this.zk.getChildren(mesa, this);
				empty = children.isEmpty();
				if (empty) {
					ZookeeperService.mutex.wait();
				}
			}
		}

		// TODO: Após este ponto, há um player para registrar, ou seja,
		// precisamos registrá-lo.

	}

}
