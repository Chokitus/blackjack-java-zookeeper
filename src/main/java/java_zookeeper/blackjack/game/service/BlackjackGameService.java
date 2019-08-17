package java_zookeeper.blackjack.game.service;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.zookeeper.KeeperException;

import java_zookeeper.blackjack.game.player.Dealer;
import java_zookeeper.blackjack.game.player.Player;
import java_zookeeper.blackjack.zookeeper.ZookeeperService;

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
		ZookeeperService.getInstance().setDataToPlayerNode(player, myBet, 1);
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
		BlackjackGameServiceHelper.sendCard(dealer.getNewCard(), dealer);
	}

	public static void askForActions(final Dealer dealer) throws KeeperException, InterruptedException {
		for (Player player : dealer.getListOfPlayers()) {
			BlackjackGameServiceHelper.askForAction(player);
		}
	}

	public static void seeTableAndAct(final Player player) throws KeeperException, InterruptedException {
		BlackjackGameServiceHelper.getDrawnCards(player);
		byte[] data = ZookeeperService.getInstance().getDataFromPlayerNode(player, false);
		BlackjackGameServiceHelper.printMaybeQuestion(data, BlackjackGameServiceHelper.ACAO, BlackjackGameServiceHelper.ACAO_STRING);

	}

}
