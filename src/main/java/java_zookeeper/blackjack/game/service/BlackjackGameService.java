package java_zookeeper.blackjack.game.service;

import java.util.List;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.zookeeper.KeeperException;

import java_zookeeper.blackjack.game.actions.BlackjackRoundAction;
import java_zookeeper.blackjack.game.deck.card.Card;
import java_zookeeper.blackjack.game.player.Dealer;
import java_zookeeper.blackjack.game.player.Player;
import java_zookeeper.blackjack.zookeeper.ZookeeperService;
import lombok.extern.log4j.Log4j;

@Log4j
public abstract class BlackjackGameService extends BlackjackGameServiceHelper {

	public static void askForBet(final Dealer dealer) throws KeeperException, InterruptedException {
		for (Player player : dealer.getListOfPlayers()) {
			BlackjackGameServiceHelper.askForBet(player);
		}
	}

	public static void bet(final Player player) throws KeeperException, InterruptedException {
		/**
		 * Pega os dados presente no próprio nó.
		 */
		byte[] data = ZookeeperService.getInstance().getDataFromPlayerNode(player, false);

		/**
		 * Se a mensagem for diferente da pergunta de aposta, desserialize e
		 * verifique, senão simplesmente printe a String
		 */
		BlackjackGameServiceHelper.printMaybeQuestion(data, BlackjackGameServiceHelper.APOSTE, BlackjackGameServiceHelper.APOSTE_STRING);

		/**
		 * Coloca a aposta no próprio nó.
		 *
		 * TODO: Verificar se a aposta foi realmente numérica do lado do
		 * Cliente, para garantir que não havarão problemas de desserialização
		 *
		 * TODO: Fazer o input dos dados, atualmente está hardcoded como 100.
		 *
		 */
		System.out.println("Apostando: 100");
		byte[] myBet = SerializationUtils.serialize(Integer.valueOf(100));
		player.setAposta(100);
		ZookeeperService.getInstance().setDataToPlayerNode(player, myBet, -1);
	}

	public static void distributeCards(final Dealer dealer) throws KeeperException, InterruptedException {
		for (Player player : dealer.getListOfPlayers()) {
			BlackjackGameServiceHelper.sendCard(dealer.getNewCard(), player);
			BlackjackGameServiceHelper.sendCard(dealer.getNewCard(), player);
		}
	}

	public static void waitForCards(final Player player, final int expectedCards) throws KeeperException, InterruptedException {
		ZookeeperService.getInstance().waitForCards(player, expectedCards);
	}

	public static void sendCardToMyself(final Dealer dealer) throws KeeperException, InterruptedException {
		Card newCard = dealer.getNewCard();
		BlackjackGameServiceHelper.sendCard(newCard, dealer);
		dealer.addToHand(newCard);
	}

	public static void askForActions(final Dealer dealer) throws KeeperException, InterruptedException {
		for (Player player : dealer.getListOfPlayers()) {
			while (BlackjackGameServiceHelper.askForAction(dealer, player)) {
				BlackjackGameService.log.info("Player será chamado novamente");
			}
		}
	}

	public static void act(final Player player) throws KeeperException, InterruptedException {
		/*
		 * Primeiro, pega a ação desejada pelo player
		 */
		BlackjackRoundAction action = BlackjackGameServiceHelper.getPlayerLocalAction(player);

		/*
		 * Envia a ação, e espera pela resposta do Dealer. Caso o pedido não
		 * seja UMA_CARTA ou DOBRAR, o Player encerrou sua mão e deverá esperar
		 * pelo resultado
		 */
		while (BlackjackGameService.actAndWaitForAnswer(player, action)) {
			/*
			 * Se entrou aqui, significa que o Player pediu mais uma carta, ou
			 * pediu para dobrar. Devemos esperar que o Dealer peça mais uma
			 * ação do Player, para que peguemos mais uma ação do Player e então
			 * repetirmos o processo.
			 */
			BlackjackGameService.waitUntilAskedForActions(player);
			action = BlackjackGameServiceHelper.getPlayerLocalAction(player);
		}
	}

	public static boolean actAndWaitForAnswer(final Player player, final BlackjackRoundAction action)
			throws KeeperException, InterruptedException {
		byte[] bytes = SerializationUtils.serialize(action);
		BlackjackGameServiceHelper.requestAndWaitForAnswer(player, bytes);
		if (BlackjackRoundAction.UMA_CARTA.equals(action) || BlackjackRoundAction.DOBRAR.equals(action)) {
			BlackjackGameService.waitForCards(player, player.getHand().size() + 1);
			return true;
		}
		return false;
	}

	public static void waitUntilAskedForActions(final Player player) throws KeeperException, InterruptedException {
		BlackjackGameServiceHelper.waitForRequestOrAnswer(player, BlackjackGameServiceHelper.ACAO);
		System.out.println(BlackjackGameServiceHelper.ACAO_STRING);
	}

	public static void seeTable(final Player player) throws KeeperException, InterruptedException {
		ZookeeperService.getInstance().getDrawnCards(player);
	}

	public static void fillHandUntilMinimum(final Dealer dealer) throws KeeperException, InterruptedException {
		while (dealer.getScore() < 17) {
			BlackjackGameService.sendCardToMyself(dealer);
		}
	}

	public static void verifyWinnersAndDoPayouts(final Dealer dealer) throws KeeperException, InterruptedException {
		int dealerScore = dealer.getScore();
		/*
		 * Verifica se o Dealer passou de 21. Caso tenha passado, para ganhar
		 * basta não ter passado você mesmo
		 */
		for (Player player : dealer.getListOfPlayers()) {
			player.printHand();
			int playerScore = player.getScore();
			System.out.println(playerScore);
			BlackjackGameServiceHelper.verifyWinner(dealerScore, player, playerScore);
			/*
			 * Acorda o Player para que ele também verifique se ganhou
			 */
			ZookeeperService.getInstance().setDataToPlayerNode(player, BlackjackGameServiceHelper.FIM, -1);
		}
	}

	public static void verifyPlayerResults(final Player player) throws KeeperException, InterruptedException {
		List<Card> dealerCards = ZookeeperService.getInstance().getDealerCards(player);
		int dealerScore = Dealer.calculateScore(dealerCards);
		int playerScore = player.getScore();
		BlackjackGameServiceHelper.verifyWinner(dealerScore, player, playerScore);
	}

	public static void cleanTableForNextRound(final Dealer dealer) throws InterruptedException, KeeperException {
		for (Player player : dealer.getListOfPlayers()) {
			player.newRound();
			ZookeeperService.getInstance().removeAllCardsFromPlayer(player);
			System.out.println(player.getCurrentMoney());
		}
		List<Player> listOfMoneylessPlayer = dealer.getAndRemovePlayerWithoutMoney();
		for (Player player : listOfMoneylessPlayer) {
			ZookeeperService.getInstance().removePlayer(player);
		}
		ZookeeperService.getInstance().removeAllCardsFromPlayer(dealer);
		dealer.newRound();

	}

	public static void waitUntilNextRound(final Player player) throws KeeperException, InterruptedException {
		BlackjackGameServiceHelper.waitForRequestOrAnswer(player, BlackjackGameServiceHelper.APOSTE);
		System.out.println(BlackjackGameServiceHelper.APOSTE_STRING);
	}

	public static void waitUntilEndOfRound(final Player player) throws KeeperException, InterruptedException {
		BlackjackGameServiceHelper.waitForRequestOrAnswer(player, BlackjackGameServiceHelper.FIM);
		System.out.println(BlackjackGameServiceHelper.FIM_STRING);
	}

	public static void waitUntilAllPlayersAreReady(final Dealer dealer) throws KeeperException, InterruptedException {
		String newRoundNode = ZookeeperService.getPathToNewRoundNode(dealer);
		ZookeeperService.getInstance().waitUntilTableIsFull(newRoundNode, dealer.getListOfPlayers().size());
		ZookeeperService.getInstance().removeAllChildrenFromNode(newRoundNode);

	}

	public static void registerForNextRound(final Player player) throws KeeperException, InterruptedException {
		ZookeeperService.getInstance().registerPlayerForNextRound(player);
	}

}
