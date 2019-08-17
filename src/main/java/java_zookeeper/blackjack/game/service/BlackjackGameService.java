package java_zookeeper.blackjack.game.service;

import java.util.Arrays;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.zookeeper.KeeperException;

import java_zookeeper.blackjack.game.deck.card.Card;
import java_zookeeper.blackjack.game.player.Dealer;
import java_zookeeper.blackjack.game.player.Player;
import java_zookeeper.blackjack.zookeeper.ZookeeperService;

public class BlackjackGameService {

	public static final String APOSTE_STRING = "Um novo round começou! Mande-me sua aposta!";
	public static final byte[] APOSTE = BlackjackGameService.APOSTE_STRING.getBytes();

	public static void askForBet(final Dealer dealer) throws KeeperException, InterruptedException {
		for (Player player : dealer.getListOfPlayers()) {
			BlackjackGameService.askForBet(player);
		}
	}

	public static void askForBet(final Player player) throws KeeperException, InterruptedException {
		/*
		 * Pega o nó do Player
		 */
		String name = ZookeeperService.getNodePathToPlayer(player);
		synchronized (ZookeeperService.mutex) {
			/*
			 * "Envia" uma mensagem ao Player, acordando-o.
			 */
			ZookeeperService.getInstance().setDataToPlayerNode(player, BlackjackGameService.APOSTE, 0);

			/*
			 * Pega aposta do Player, verificando se a mensagem ainda é a que
			 * ele mesmo enviou, se for, aguarde uma aposta.
			 *
			 * TODO: Se a aposta não for numérica, pede novamente (talvez, não
			 * sei se vale a pena)
			 */
			byte[] aposta = ZookeeperService.getInstance().zk.getData(name, ZookeeperService.getInstance(), null);
			while (Arrays.equals(aposta, BlackjackGameService.APOSTE)) {
				/*
				 * Aguarda ser acordado
				 */
				ZookeeperService.mutex.wait();
				aposta = ZookeeperService.getInstance().zk.getData(name, ZookeeperService.getInstance(), null);
			}

			/*
			 * Pega a aposta e desserializa.
			 */
			Integer apostaNumerica = SerializationUtils.deserialize(aposta);
			player.setAposta(apostaNumerica);
			System.out.println("Chegou a aposta: " + apostaNumerica);
		}
	}

	public static void bet(final Player player) throws KeeperException, InterruptedException {
		/**
		 * Pega os dados presente no próprio nó.
		 */
		byte[] data = ZookeeperService.getInstance().getDataFromPlayerNode(player);

		/**
		 * Se a mensagem for diferente da pergunta de aposta, desserialize e
		 * verifique, senão simplesmente printe a String
		 */
		if (!Arrays.equals(BlackjackGameService.APOSTE, data)) {
			Object deserialize = SerializationUtils.deserialize(data);
			System.out.println("Mensagem estranha do Dealer: " + deserialize);
		} else {
			System.out.println("Dealer diz: " + BlackjackGameService.APOSTE_STRING);
		}

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
			BlackjackGameService.sendCard(dealer.getNewCard(), player);
			BlackjackGameService.sendCard(dealer.getNewCard(), player);
		}
	}

	public static void waitForCards(final Player player, final int expectedCards) throws KeeperException, InterruptedException {
		ZookeeperService.getInstance().waitForCards(player, expectedCards);
	}

	private static void sendCard(final Card card, final Player player) throws KeeperException, InterruptedException {
		ZookeeperService.getInstance().enviarCardParaPlayer(player, card);
	}

	public static void sendCardToMyself(final Dealer dealer) throws KeeperException, InterruptedException {
		BlackjackGameService.sendCard(dealer.getNewCard(), dealer);
	}

}
