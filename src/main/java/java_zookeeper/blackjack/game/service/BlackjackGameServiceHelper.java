package java_zookeeper.blackjack.game.service;

import java.util.Arrays;
import java.util.Optional;
import java.util.Scanner;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.KeeperException;

import java_zookeeper.blackjack.game.actions.BlackjackRoundAction;
import java_zookeeper.blackjack.game.deck.card.Card;
import java_zookeeper.blackjack.game.player.Dealer;
import java_zookeeper.blackjack.game.player.Player;
import java_zookeeper.blackjack.zookeeper.ZookeeperService;
import lombok.extern.log4j.Log4j;

@Log4j
public abstract class BlackjackGameServiceHelper {
	protected static final String APOSTE_STRING = "Um novo round começou! Mande-me sua aposta!";
	protected static final byte[] APOSTE = BlackjackGameServiceHelper.APOSTE_STRING.getBytes();

	protected static final String ACAO_STRING = "É hora de jogar! Escolha sua ação.";
	protected static final byte[] ACAO = BlackjackGameServiceHelper.ACAO_STRING.getBytes();

	protected static final String OK_STRING = "Entendido!";
	protected static final byte[] OK = BlackjackGameServiceHelper.OK_STRING.getBytes();

	protected static final String FIM_STRING = "Acabaram as apostas, hora de ver quem ganhou!";
	protected static final byte[] FIM = BlackjackGameServiceHelper.FIM_STRING.getBytes();

	public static final Scanner input = new Scanner(System.in);

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
		BlackjackGameServiceHelper.log
				.info(new StringBuilder("Enviando ").append(card.toString()).append(" para ").append(player.getName()).append("!"));
		ZookeeperService.getInstance().enviarCardParaPlayer(player, card);
		player.addToHand(card);
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
			BlackjackGameServiceHelper.log.info("O player deseja uma nova carta!");
			BlackjackGameServiceHelper.sendCard(dealer.getNewCard(), player);
			return true;
		}
		if (BlackjackRoundAction.DOBRAR.equals(playerAction)) {
			BlackjackGameServiceHelper.log.info("O player deseja dobrar a aposta!");
			player.dobrarAposta();
			BlackjackGameServiceHelper.sendCard(dealer.getNewCard(), player);
			return true;
		}
		if (BlackjackRoundAction.PARAR.equals(playerAction)) {
			BlackjackGameServiceHelper.log.info("O player deseja parar! Hora de chamar outro jogador!");
		}
		if (BlackjackRoundAction.DESISTIR.equals(playerAction)) {
			/*
			 * Pegar 50% da aposta dele e dar pro Dealer. TODO
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
		player.printHand(false);
		if (player.getScore() > 21) {
			return BlackjackRoundAction.ESTOUREI;
		}
		BlackjackGameServiceHelper.log
				.info(new StringBuilder("Escolha uma ação! As ações possíveis são: \n").append(BlackjackRoundAction.UMA_CARTA.getCommand())
						.append(": Mais uma carta \n").append(BlackjackRoundAction.DOBRAR.getCommand()).append(": Dobrar \n")
						.append(BlackjackRoundAction.PARAR.getCommand()).append(": Parar \n")
						.append(BlackjackRoundAction.DESISTIR.getCommand()).append(": Desistir (50% da sua aposta retorna para você)"));
		String line = BlackjackGameServiceHelper.input.nextLine();
		Optional<BlackjackRoundAction> maybeAction = BlackjackRoundAction.parseAction(line);
		/*
		 * Enquanto você não digitou um comando válido, pede novamente.
		 */
		while (!maybeAction.isPresent()) {
			line = BlackjackGameServiceHelper.input.nextLine();
			maybeAction = BlackjackRoundAction.parseAction(line);
		}
		return maybeAction.get();
	}

	protected static int getReward(final int playerScore, final int aposta) {
		return (int) (playerScore == 21 ? aposta * 1.5 : aposta);
	}

	protected static void verifyWinner(final int dealerScore, final Player player, final int playerScore)
			throws KeeperException, InterruptedException {
		StringBuilder playerString = new StringBuilder("Player ").append(player.getName());
		if (dealerScore > 21) {
			/*
			 * Se não passou de 21, ganhou! Se ficou com 21, ainda ganha um
			 * adicional de 50% da aposta
			 */
			if (playerScore <= 21) {
				BlackjackGameServiceHelper.log.info(playerString.append(" ganhou!").toString());
				player.setToCurrentMoney(BlackjackGameServiceHelper.getReward(playerScore, player.getAposta()));
			} else {
				BlackjackGameServiceHelper.log.info(playerString.append(" empatou e não ganha nada!").toString());
			}
		} else {
			if (playerScore > dealerScore && playerScore <= 21) {
				BlackjackGameServiceHelper.log.info(playerString.append(" ganhou!").toString());
				player.setToCurrentMoney(BlackjackGameServiceHelper.getReward(playerScore, player.getAposta()));
			} else if (playerScore == dealerScore) {
				BlackjackGameServiceHelper.log.info(playerString.append(" empatou e não ganha nada!").toString());
			} else {
				BlackjackGameServiceHelper.log.info(playerString.append(" perdeu!").toString());
				player.setToCurrentMoney(-player.getAposta());
			}
		}
	}

	protected static void doBet(final Player player) throws KeeperException, InterruptedException {
		String playerBet = "";
		do {
			System.out.println("Insira uma aposta (deve ser numérica!)");
			playerBet = BlackjackGameServiceHelper.input.nextLine();
		} while (!StringUtils.isNumeric(playerBet));
		Integer numericPlayerBet = Integer.valueOf(playerBet);
		byte[] myBet = SerializationUtils.serialize(numericPlayerBet);
		player.setAposta(numericPlayerBet);
		ZookeeperService.getInstance().setDataToPlayerNode(player, myBet, -1);
	}
}
