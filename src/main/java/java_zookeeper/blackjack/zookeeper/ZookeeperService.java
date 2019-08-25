package java_zookeeper.blackjack.zookeeper;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZKUtil;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java_zookeeper.blackjack.game.deck.card.Card;
import java_zookeeper.blackjack.game.player.Dealer;
import java_zookeeper.blackjack.game.player.Player;
import lombok.extern.log4j.Log4j;

@Log4j
public class ZookeeperService implements Watcher, Closeable {

	private static final String BLACKJACK = "/blackjack";
	private static final byte[] INITIAL_MONEY = SerializationUtils.serialize(Integer.valueOf(2000));
	private static final String PURSE = "/purse";

	private ZooKeeper zk = null;

	public static final Object mutex = new Object();
	public static final Object mutex2 = new Object();

	private static ZookeeperService instance = null;

	private ZookeeperService(final String host) {
		try {
			this.zk = new ZooKeeper(host, 3000, this);
		} catch (IOException e) {
			ZookeeperService.log.error(e);
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
				? new StringBuilder(ZookeeperService.BLACKJACK).append("/").append(player.getMesa()).append("/").append(player.getName())
						.toString()
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

	@Override
	public void process(final WatchedEvent event) {
		if (Watcher.Event.EventType.NodeDeleted.equals(event.getType()) && event.getPath().contains("_election")) {
			synchronized (ZookeeperService.mutex2) {
				ZookeeperService.mutex2.notifyAll();
			}
		}
		synchronized (ZookeeperService.mutex) {
			ZookeeperService.mutex.notifyAll();
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
		try {
			if (mesa.isBlank()) {
				return this.zk.create(ZookeeperService.BLACKJACK, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}
			return this.zk.create(ZookeeperService.BLACKJACK + "/" + mesa, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

		} catch (KeeperException.NodeExistsException e) {
			/*
			 * Reaproveitamos a mesa
			 */
			return ZookeeperService.BLACKJACK + "/" + mesa;
		}

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
	 * @param firstTime
	 * @param key
	 * @return
	 * @throws KeeperException
	 * @throws Interrupted
	 *             Exception
	 */
	public String createNewPlayerNode(final Player player, final boolean firstTime) throws KeeperException, InterruptedException {
		synchronized (ZookeeperService.mutex) {
			/*
			 * Se a mesa não existe, espera um dealer para criá-la
			 */
			while (this.zk.exists(ZookeeperService.BLACKJACK + "/" + player.getMesa(), true) == null) {
				ZookeeperService.mutex.wait();
			}

			byte[] mensagemDeRegistro = "Gostaria de entrar na mesa!".getBytes();
			String nodeName = this.createPlayerNode(player, mensagemDeRegistro);
			player.setFullName(nodeName);
			player.setInitialMoney(this.getMoneyFromPlayer(player));

		}

		return null;
	}

	private Integer getMoneyFromPlayer(final Player player) throws KeeperException, InterruptedException {
		byte[] data = this.zk.getData(player.getFullName() + ZookeeperService.PURSE, false, null);
		return SerializationUtils.deserialize(data);
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
	public String createPlayerNode(final Player player, final byte[] mensagemDeRegistro) throws InterruptedException, KeeperException {
		try {
			String playerNode = this.zk.create(ZookeeperService.getNodePathToPlayer(player), mensagemDeRegistro, Ids.OPEN_ACL_UNSAFE,
					CreateMode.PERSISTENT);
			/*
			 * Cria também seu "bolso", onde teremos a informação de quanto
			 * dinheiro tem o Player.
			 */
			this.zk.create(playerNode + ZookeeperService.PURSE, ZookeeperService.INITIAL_MONEY, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			return playerNode;
		} catch (KeeperException.NodeExistsException e) {
			/*
			 * Player já existe, então reutilizaremos o nó.
			 */
			return ZookeeperService.getNodePathToPlayer(player);
		}
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
			do {
				children = this.getChildren(mesa, this);
				numOfPlayers = children.size();
				ZookeeperService.log
						.info(new StringBuilder("Foram encontrados ").append(numOfPlayers).append(" jogadores na fila.").toString());
				if (numOfPlayers < expectedPlayers) {
					ZookeeperService.log.info("Como o número é menor que o esperado, irei esperar por mais jogadores");
					ZookeeperService.mutex.wait();
					ZookeeperService.log.info("Novo jogador entrou na fila!");
				}
			} while (numOfPlayers < expectedPlayers);
		}
		ZookeeperService.log.info("Seguindo com o jogo!");
		return children;
	}

	private List<String> getChildren(final String mesa, final Watcher watch) throws KeeperException, InterruptedException {
		String mesaTemp = mesa;
		if (!mesa.startsWith(ZookeeperService.BLACKJACK)) {
			mesaTemp = ZookeeperService.BLACKJACK + "/" + mesaTemp;
		}
		return this.zk.getChildren(mesaTemp, watch);
	}

	public void setDataToPlayerNode(final Player player, final byte[] bytes, final int version)
			throws KeeperException, InterruptedException {
		ZookeeperService.log.info(new StringBuilder("Informando no nó ").append(player.getName()).append("..."));
		ZookeeperService.getInstance().zk.setData(ZookeeperService.getNodePathToPlayer(player), bytes, version);
	}

	public void waitForCards(final Player player, final int expectedCards) throws KeeperException, InterruptedException {
		synchronized (ZookeeperService.mutex) {
			List<String> children = this.zk.getChildren(ZookeeperService.getNodePathToPlayer(player), true);

			while (children.size() < expectedCards + 1) {
				ZookeeperService.mutex.wait();
				children = this.zk.getChildren(ZookeeperService.getNodePathToPlayer(player), true);
			}
			for (String child : children) {
				if (!ZookeeperService.PURSE.equals("/" + child)) {
					player.addToHand(this.getCardFromPath(player.getFullName() + "/" + child));
				}
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
		List<String> players = this.getChildren(player.getMesa(), null);

		for (String otherPlayer : players) {
			System.out.println("O player " + otherPlayer + " tem:");
			String playerNode = ZookeeperService.BLACKJACK + "/" + player.getMesa() + "/" + otherPlayer;
			List<Card> cardsFromPlayer = this.getCardsFromPlayerNode(playerNode);
			cardsFromPlayer.forEach(System.out::println);
		}
	}

	private List<Card> getCardsFromPlayerNode(final String playerNode) throws KeeperException, InterruptedException {
		List<String> cards = this.zk.getChildren(playerNode, false).stream().filter(p -> !ZookeeperService.PURSE.equals("/" + p))
				.collect(Collectors.toList());
		List<Card> listOfCards = new ArrayList<>();
		for (String card : cards) {
			String cardNode = playerNode + "/" + card;
			Card cardFromNode = SerializationUtils.deserialize(this.zk.getData(cardNode, false, null));
			listOfCards.add(cardFromNode);
		}
		return listOfCards;
	}

	public List<Card> getDealerCards(final Player player) throws KeeperException, InterruptedException {
		String dealerNode = new StringBuilder("/blackjack/").append(player.getMesa()).append("/dealer").toString();
		return this.getCardsFromPlayerNode(dealerNode);
	}

	public void removeAllCardsFromPlayer(final Player player) throws InterruptedException, KeeperException {
		String pathToPlayer = ZookeeperService.getNodePathToPlayer(player);
		this.removeAllChildrenFromNode(pathToPlayer);
	}

	public void removeAllChildrenFromNode(final String node) throws KeeperException, InterruptedException {
		List<String> children = this.zk.getChildren(node, false);
		for (String child : children) {
			if (!ZookeeperService.PURSE.equals("/" + child)) {
				this.zk.delete(node + "/" + child, -1);
			}
		}
	}

	public void removePlayer(final Player player) throws InterruptedException, KeeperException {
		this.removeNode(ZookeeperService.getNodePathToPlayer(player));
	}

	public void removeNode(final String path) throws InterruptedException, KeeperException {
		ZKUtil.deleteRecursive(this.zk, path);
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
		return ZookeeperService.BLACKJACK + "/" + player.getMesa() + "_new_round/" + player.getName();
	}

	public static String getPathToNewRoundNode(final Dealer dealer) {
		return ZookeeperService.BLACKJACK + "/" + dealer.getMesa() + "_new_round";
	}

	private String getPathToElectionNode(final String mesa) {
		return ZookeeperService.BLACKJACK + "/" + mesa + "_election";
	}

	public Stat existsElection(final String mesa) throws KeeperException, InterruptedException {
		if (this.exists(ZookeeperService.BLACKJACK, false) == null) {
			this.createNewMesa("");
		}
		return this.exists(this.getPathToElectionNode(mesa), true);
	}

	public Stat exists(final String node, final boolean watch) throws KeeperException, InterruptedException {
		return this.zk.exists(node, watch);
	}

	public void createElectionNode(final String mesa) throws KeeperException, InterruptedException {
		try {
			this.zk.create(this.getPathToElectionNode(mesa), new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		} catch (KeeperException.NodeExistsException e) {
			ZookeeperService.log.info(e);
		}
	}

	public int createCandidateNode(final String mesa) throws KeeperException, InterruptedException {
		String pathToElectionNode = new StringBuilder(this.getPathToElectionNode(mesa)).append("/").toString();
		String nodeName = this.zk.create(pathToElectionNode, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
		return Integer.parseInt(nodeName.replaceAll(pathToElectionNode, ""));
	}

	public Map<String, Integer> getAllCandidatesFromElection(final String mesa) throws KeeperException, InterruptedException {
		String pathToElectionNode = this.getPathToElectionNode(mesa);
		List<String> candidates = this.zk.getChildren(pathToElectionNode, false);
		Map<String, Integer> candidateToId = new HashMap<>();
		candidates.forEach(candidate -> candidateToId.put(candidate, Integer.valueOf(candidate.replaceAll(pathToElectionNode + "/", ""))));
		return candidateToId;
	}

	public void watchForLeaderHealth(final String mesa, final String leaderNode) throws KeeperException, InterruptedException {
		synchronized (ZookeeperService.mutex2) {
			String leaderNodePath = this.getPathToElectionNode(mesa) + "/" + leaderNode;
			while (this.zk.exists(leaderNodePath, this) != null) {
				ZookeeperService.mutex2.wait();
			}
		}
	}

	public void setMoneyToPlayer(final Player player, final int currentMoney) throws KeeperException, InterruptedException {
		byte[] data = SerializationUtils.serialize(Integer.valueOf(currentMoney));
		this.zk.setData(ZookeeperService.getNodePathToPlayer(player) + ZookeeperService.PURSE, data, -1);
	}
}
