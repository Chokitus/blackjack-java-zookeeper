package java_zookeeper.blackjack.zookeeper;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;

import java_zookeeper.blackjack.game.deck.card.Card;
import java_zookeeper.blackjack.game.player.Dealer;
import java_zookeeper.blackjack.game.player.Player;
import lombok.extern.log4j.Log4j;

@Log4j
public class ZookeeperService implements Watcher, Closeable {

	private ZooKeeper zk = null;

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
	public static String getNodePathToPlayer(final Player player) {
		return "".equals(player.getFullName())
				? new StringBuilder("/").append(player.getMesa()).append("/").append(player.getName()).toString()
				: player.getFullName();
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
		ZookeeperService.log.info("Criando nova mesa de nome" + mesa);
		return this.zk.create("/" + mesa, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
	}

	/**
	 * <p>
	 * Cria um novo Player na mesa, passando o nome da mesa e o do Player. A
	 * ideia é que um Player espere o Dealer criar a mesa, e se registre nesta
	 * mesa. Ao criar o nó, o Player deixará a sua chave no nó. O Dealer deverá
	 * ter um Watch na mesa, pois assim que um novo Player criar um nó, o Dealer
	 * deverá olhar para a chave da mesma, memorizá-la (num HashMap), e
	 * sobrescrever a chave. Assim, fica acordado uma chave (presumidamente
	 * única) para cada jogador.
	 * </p>
	 * <p>
	 * Assim que pelo menos <b> n jogadores </b> entrarem, o jogo começa.
	 * </p>
	 *
	 * @param mesa
	 * @param player
	 * @param key
	 * @return
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public String createNewPlayerNodeAndWaitTillDealerCall(final Player player) throws KeeperException, InterruptedException {
		synchronized (ZookeeperService.mutex) {
			if (this.zk.exists("/" + player.getMesa(), false) == null) {
				throw new IllegalStateException("Um dealer deve ser decidido antes que os players se registrem.");
			}

			byte[] mensagemDeRegistro = "Gostaria de entrar na mesa!".getBytes();
			String nodeName = this.createPlayerNode(player, mensagemDeRegistro, false);
			player.setFullName(nodeName);

			while (Arrays.equals(mensagemDeRegistro, this.zk.getData(nodeName, ZookeeperService.getInstance(), null))) {
				System.out.println("Esperando aviso do Dealer.");
				ZookeeperService.mutex.wait();
			}
		}

		return null;
	}

	/**
	 * Cria um nó de Player. Deve ser usado com
	 * {@link #createNewPlayerNodeAndWaitTillDealerCall(Player)}, ou chamado
	 * pelo Dealer no
	 * {@link ZookeeperPlayerRegister#registerDealer(String, String, int)}
	 *
	 * @param player
	 * @param mensagemDeRegistro
	 * @param isDealer
	 *            Se verdadeiro, cria o nó com o nome exato, senão cria como
	 *            sequential (ambos persistentes)
	 * @return
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public String createPlayerNode(final Player player, final byte[] mensagemDeRegistro, final boolean isDealer)
			throws KeeperException, InterruptedException {
		return this.zk.create(ZookeeperService.getNodePathToPlayer(player), mensagemDeRegistro, Ids.OPEN_ACL_UNSAFE,
				isDealer ? CreateMode.PERSISTENT : CreateMode.PERSISTENT_SEQUENTIAL);
	}

	/**
	 * Cria um nó que representa a carta abaixo do Player.
	 *
	 * @param player
	 * @param card
	 * @return
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public String enviarCardParaPlayer(final Player player, final Card card) throws KeeperException, InterruptedException {
		return this.createZNode(player, card.getBytes());
	}

	public String createZNode(final Player player, final byte[] data) throws KeeperException, InterruptedException {
		return this.zk.create(player.getFullName() + "/", data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
	}

	@Override
	public void process(final WatchedEvent event) {
		synchronized (ZookeeperService.mutex) {
			ZookeeperService.mutex.notifyAll();
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
	 * @return
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public List<String> waitUntilTableIsFull(final String mesa, final int expectedPlayers) throws KeeperException, InterruptedException {
		List<String> children = null;
		synchronized (ZookeeperService.mutex) {
			int numOfPlayers = 0;
			while (numOfPlayers < expectedPlayers) {
				children = this.zk.getChildren(mesa, this);
				numOfPlayers = children.size();
				if (numOfPlayers < expectedPlayers) {
					ZookeeperService.mutex.wait();
				}
				ZookeeperService.log.info(new StringBuilder("Player novo entrou, a mesa está com ").append(numOfPlayers).append(" de ")
						.append(expectedPlayers).append(" mesas ocupadas"));
			}
		}

		ZookeeperService.log.info("Seguindo com o jogo!");
		return children;
	}

	public void setDataToPlayerNode(final Player player, final byte[] bytes, final int version)
			throws KeeperException, InterruptedException {
		ZookeeperService.getInstance().zk.setData(ZookeeperService.getNodePathToPlayer(player), bytes, version);
	}

	public void waitForCards(final Player player, final int expectedCards) throws KeeperException, InterruptedException {
		synchronized (ZookeeperService.mutex) {
			List<String> children = this.zk.getChildren(ZookeeperService.getNodePathToPlayer(player), true);

			while (children.size() < expectedCards) {
				ZookeeperService.mutex.wait();
				children = this.zk.getChildren(ZookeeperService.getNodePathToPlayer(player), true);
			}
			for (String child : children) {
				player.addToHand(this.getCardFromPath(player.getFullName() + "/" + child));
			}
		}
	}

	private Card getCardFromPath(final String child) throws KeeperException, InterruptedException {
		return Card.cardFromBytes(this.zk.getData(child, false, null));
	}

	public byte[] getDataFromPlayerNode(final Player player, final boolean watch) throws KeeperException, InterruptedException {
		return ZookeeperService.getInstance().zk.getData(ZookeeperService.getNodePathToPlayer(player),
				watch ? ZookeeperService.getInstance() : null, null);
	}

	public void getDrawnCards(final Player player) throws KeeperException, InterruptedException {
		String mesa = "/" + player.getMesa();
		List<String> players = this.zk.getChildren(mesa, false);

		for (String otherPlayer : players) {
			String playerNode = mesa + "/" + otherPlayer;
			List<Card> cardsFromPlayer = this.getCardsFromPlayerNode(playerNode);
			cardsFromPlayer.forEach(card -> System.out.println(card));
		}
	}

	private List<Card> getCardsFromPlayerNode(final String playerNode) throws KeeperException, InterruptedException {
		List<String> cards = this.zk.getChildren(playerNode, false);
		List<Card> listOfCards = new ArrayList<>();
		for (String card : cards) {
			String cardNode = playerNode + "/" + card;
			Card cardFromNode = SerializationUtils.deserialize(this.zk.getData(cardNode, false, null));
			listOfCards.add(cardFromNode);
		}
		return listOfCards;
	}

	public List<Card> getDealerCards(final Player player) throws KeeperException, InterruptedException {
		String dealerNode = new StringBuilder("/").append(player.getMesa()).append("/dealer").toString();
		return this.getCardsFromPlayerNode(dealerNode);
	}

	public void removeAllCardsFromPlayer(final Player player) throws InterruptedException, KeeperException {
		String pathToPlayer = ZookeeperService.getNodePathToPlayer(player);
		this.removeAllChildrenFromNode(pathToPlayer);
	}

	public void removeAllChildrenFromNode(final String node) throws KeeperException, InterruptedException {
		List<String> children = this.zk.getChildren(node, false);
		for (String child : children) {
			this.zk.delete(node + "/" + child, -1);
		}
	}

	public void removePlayer(final Player player) throws InterruptedException, KeeperException {
		this.zk.delete(ZookeeperService.getNodePathToPlayer(player), -1);
	}

	public void registerPlayerForNextRound(final Player player) throws KeeperException, InterruptedException {
		synchronized (ZookeeperService.mutex) {
			String newRoundNode = ZookeeperService.getPathToNewRoundNode(player);
			newRoundNode = this.zk.create(newRoundNode, "Gostaria de jogar mais um Round!".getBytes(), Ids.OPEN_ACL_UNSAFE,
					CreateMode.PERSISTENT_SEQUENTIAL);
			while (this.zk.exists(newRoundNode, this) != null) {
				ZookeeperService.mutex.wait();
			}
		}

	}

	public static String getPathToNewRoundNode(final Player player) {
		return "/" + player.getMesa() + "_new_round/" + player.getName();
	}

	public static String getPathToNewRoundNode(final Dealer dealer) {
		return "/" + dealer.getMesa() + "_new_round";
	}

}
