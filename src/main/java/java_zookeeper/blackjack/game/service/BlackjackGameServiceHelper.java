package java_zookeeper.blackjack.game.service;

import java.util.Arrays;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.zookeeper.KeeperException;

import java_zookeeper.blackjack.game.actions.BlackjackRoundAction;
import java_zookeeper.blackjack.game.deck.card.Card;
import java_zookeeper.blackjack.game.player.Dealer;
import java_zookeeper.blackjack.game.player.Player;
import java_zookeeper.blackjack.zookeeper.ZookeeperService;

public abstract class BlackjackGameServiceHelper {
	protected static final String APOSTE_STRING = "Um novo round começou! Mande-me sua aposta!";
	protected static final byte[] APOSTE = BlackjackGameServiceHelper.APOSTE_STRING.getBytes();

	protected static final String ACAO_STRING = "É hora de jogar! Escolha sua ação.";
	protected static final byte[] ACAO = BlackjackGameServiceHelper.ACAO_STRING.getBytes();

	protected static final String OK_STRING = "Entendido!";
	protected static final byte[] OK = BlackjackGameServiceHelper.OK_STRING.getBytes();

	protected BlackjackGameServiceHelper() {

	}

	protected static byte[] requestAndWaitForAnswer(final Player player, final byte[] bytesDaRequisicao)
			throws KeeperException, InterruptedException {
		synchronized (ZookeeperService.mutex) {
			/*
			 * "Envia" uma mensagem ao Player, acordando-o.
			 */
			ZookeeperService.getInstance().setDataToPlayerNode(player, bytesDaRequisicao, -1);

			/*
			 * Pega a resposta do player, verifica se o player mudou os dados
			 * (se a pergunta ainda estiver lá, verifica novamente quando
			 * acordado
			 */
			byte[] answer = ZookeeperService.getInstance().getDataFromPlayerNode(player, true);
			while (Arrays.equals(answer, bytesDaRequisicao)) {
				/*
				 * Aguarda ser acordado
				 */
				ZookeeperService.mutex.wait();
				answer = ZookeeperService.getInstance().getDataFromPlayerNode(player, true);
			}

			/*
			 * Retorna a resposta
			 */
			return answer;
		}
	}

	protected static void printMaybeQuestion(final byte[] data, final byte[] expectedQuestionBytes, final String expectedQuestion) {
		if (!Arrays.equals(expectedQuestionBytes, data)) {
			Object deserialize = SerializationUtils.deserialize(data);
			System.out.println(new StringBuilder("Mensagem estranha do Dealer: ").append(deserialize));
		} else {
			System.out.println(new StringBuilder("Dealer diz: ").append(expectedQuestion));
		}
	}

	protected static void askForBet(final Player player) throws KeeperException, InterruptedException {
		Integer aposta = SerializationUtils
				.deserialize(BlackjackGameServiceHelper.requestAndWaitForAnswer(player, BlackjackGameServiceHelper.APOSTE));
		player.setAposta(aposta);
		System.out.println("Chegou a aposta: " + aposta);
	}

	protected static void sendCard(final Card card, final Player player) throws KeeperException, InterruptedException {
		ZookeeperService.getInstance().enviarCardParaPlayer(player, card);
	}

	protected static boolean askForAction(final Dealer dealer, final Player player) throws KeeperException, InterruptedException {
		byte[] answer = BlackjackGameServiceHelper.requestAndWaitForAnswer(player, BlackjackGameServiceHelper.ACAO);
		BlackjackRoundAction acao = SerializationUtils.deserialize(answer);
		return BlackjackGameServiceHelper.answerToAction(dealer, player, acao);
	}

	private static boolean answerToAction(final Dealer dealer, final Player player, final BlackjackRoundAction playerAction)
			throws KeeperException, InterruptedException {
		BlackjackGameServiceHelper.acceptRequest(player);
		if (BlackjackRoundAction.UMA_CARTA.equals(playerAction)) {
			BlackjackGameServiceHelper.sendCard(dealer.getNewCard(), player);
			return true;
		}
		if (BlackjackRoundAction.DOBRAR.equals(playerAction)) {
			player.dobrarAposta();
			BlackjackGameServiceHelper.sendCard(dealer.getNewCard(), player);
			return true;
		}
		if (BlackjackRoundAction.PARAR.equals(playerAction)) {
			/*
			 * Parar de pedir para o Player (ou seja, pular ele na lista, talvez
			 * tenha um atributo boolean se parou ou não, TODO
			 */
		}
		if (BlackjackRoundAction.DESISTIR.equals(playerAction)) {
			/*
			 * Pegar 50% da aposta dele e dar pro Dealer.
			 */
		}
		return false;

	}

	protected static void acceptRequest(final Player player) throws KeeperException, InterruptedException {
		ZookeeperService.getInstance().setDataToPlayerNode(player, BlackjackGameServiceHelper.OK, -1);
	}

	protected static byte[] waitForRequestOrAnswer(final Player player, final byte[] expectedRequestOrAnswer)
			throws KeeperException, InterruptedException {
		synchronized (ZookeeperService.mutex) {
			/*
			 * Pega os dados do próprio nó
			 */
			byte[] requestOrAnswer = ZookeeperService.getInstance().getDataFromPlayerNode(player, true);

			/*
			 * Enquanto forem diferentes do esperado, aguarde
			 */
			while (!Arrays.equals(requestOrAnswer, expectedRequestOrAnswer)) {
				/*
				 * Aguarda ser acordado
				 */
				ZookeeperService.mutex.wait();
				requestOrAnswer = ZookeeperService.getInstance().getDataFromPlayerNode(player, true);
			}
			return requestOrAnswer;
		}

	}

	/**
	 * Se o Score do Player passar de 21, automaticamente devolve ESTOUREI,
	 * senão, pede ao Player por uma ação.
	 *
	 * @param player
	 * @return Ação do Player, ou estourei caso o Player esteja com mais de 21
	 */
	protected static BlackjackRoundAction getPlayerLocalAction(final Player player) {
		if (player.getScore() > 21) {
			return BlackjackRoundAction.ESTOUREI;
		}
		if (player.getScore() < 16) {
			System.out.println("Uma carta por favor!");
			return BlackjackRoundAction.UMA_CARTA;
		}
		return BlackjackRoundAction.PARAR;
	}
}
