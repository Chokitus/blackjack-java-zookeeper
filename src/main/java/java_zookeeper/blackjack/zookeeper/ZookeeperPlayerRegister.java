package java_zookeeper.blackjack.zookeeper;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.zookeeper.KeeperException;

import java_zookeeper.blackjack.BlackJack;
import java_zookeeper.blackjack.game.player.Dealer;
import java_zookeeper.blackjack.game.player.DealerImpl;
import java_zookeeper.blackjack.game.player.Player;
import java_zookeeper.blackjack.game.service.BlackjackGameService;

public class ZookeeperPlayerRegister {

	private static boolean firstTime = true;

	public static Player registerPlayer(final String mesa, final String name, final boolean firstTime)
			throws KeeperException, InterruptedException {
		Player player = new Player(name, mesa);
		ZookeeperService.getInstance().createNewPlayerNode(player, firstTime);
		BlackjackGameService.waitUntilNextRound(player);

		return player;
	}

	public static Dealer registerDealer(final String mesa, final String name, final int expectedPlayers)
			throws KeeperException, InterruptedException {

		Dealer dealer = new DealerImpl(name, mesa);
		String mesaName = ZookeeperService.getInstance().createNewMesa(mesa);
		ZookeeperService.getInstance().createNewMesa(mesa + "_new_round");
		dealer.setFullName(mesaName + "/dealer");
		ZookeeperService.getInstance().createPlayerNode(dealer, new byte[0]);

		/*
		 * Aguarda até pelo menos @expectedPlayers players entrarem na mesa.
		 */
		ZookeeperService.getInstance().waitUntilTableIsFull(mesaName, expectedPlayers);
		// dealer.registerPlayers(players);

		return dealer;
	}

	public static void electLeader(final String mesa, final String nomeDoPlayer) throws KeeperException, InterruptedException {
		/*
		 * 1. Cria nó efêmero e sequencial
		 */
		ZookeeperPlayerRegister.createOrFindElectionNode(mesa);
		int id = ZookeeperService.getInstance().createCandidateNode(mesa);
		/*
		 * 2. Procura pelo nó de menor índice
		 */

		while (true) {
			int leaderId = id;
			String leaderNode = "";
			Thread.sleep(5000l);
			Map<String, Integer> allCandidatesFromElection = ZookeeperService.getInstance().getAllCandidatesFromElection(mesa);
			for (Map.Entry<String, Integer> candidate : allCandidatesFromElection.entrySet()) {
				if (leaderId > candidate.getValue()) {
					leaderId = candidate.getValue();
					leaderNode = candidate.getKey();
				}
			}
			/*
			 * 3.a: Eu sou o leader, então vou seguir com o jogo.
			 */
			if (leaderNode.isBlank()) {
				if (!ZookeeperPlayerRegister.firstTime) {
					ZookeeperService.getInstance().removeNode("/blackjack/" + mesa + "/" + nomeDoPlayer);
				}
				new BlackJack().play(allCandidatesFromElection.size(), mesa, nomeDoPlayer, false);
				return;
			}
			/*
			 * 3.b: Não sou o leader, vou rodar o jogo em uma thread e manter
			 * outra de olho. Caso o leader caia, faço a eleição novamente.
			 */
			ExecutorService executor = Executors.newSingleThreadExecutor();
			Runnable playGame = () -> {
				try {
					new BlackJack().play(0, mesa, nomeDoPlayer, ZookeeperPlayerRegister.firstTime);
				} catch (KeeperException | InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			};
			executor.submit(playGame);
			/*
			 * Espera pela queda do leader
			 */
			ZookeeperService.getInstance().watchForLeaderHealth("/blackjack/" + mesa, leaderNode);
			executor.shutdownNow();
			ZookeeperPlayerRegister.firstTime = false;
		}
	}

	private static void createOrFindElectionNode(final String mesa) throws KeeperException, InterruptedException {
		if (ZookeeperService.getInstance().existsElection(mesa) == null) {
			ZookeeperService.getInstance().createElectionNode(mesa);
		}
	}

}
