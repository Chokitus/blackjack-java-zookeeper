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
		BlackjackGameService.log.info("Irei chamar player a player pelas apostas!");
		for (Player player : dealer.getListOfPlayers()) {
			BlackjackGameService.log
					.info(new StringBuilder("Chamando o Player ").append(player.getName()).append(" para que diga sua aposta!"));
			BlackjackGameServiceHelper.askForBet(player);
		}
		BlackjackGameService.log.info("Todos os jogadores foram chamados, retornando ao jogo!");
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
		 */
		BlackjackGameServiceHelper.doBet(player);
	}

	/**
	 * Faz exatamente o que o nome diz, para cada player em
	 * {@link Dealer#getListOfPlayers()}, manda duas cartas.
	 *
	 * @param dealer
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public static void distributeCards(final Dealer dealer) throws KeeperException, InterruptedException {
		BlackjackGameService.log.info("Enviarei 2 cardas para cada Player.");
		for (Player player : dealer.getListOfPlayers()) {
			BlackjackGameServiceHelper.sendCard(dealer.getNewCard(), player);
			BlackjackGameServiceHelper.sendCard(dealer.getNewCard(), player);
		}
		BlackjackGameService.log.info("Cartas distribuidas, voltando ao jogo.");
	}

	/**
	 * Espera o Dealer mandar cartas
	 *
	 * @param player
	 * @param expectedCards
	 *            Número de cartas que está esperando.
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public static void waitForCards(final Player player, final int expectedCards) throws KeeperException, InterruptedException {
		ZookeeperService.getInstance().waitForCards(player, expectedCards);
	}

	/**
	 * Envia cartas para si mesmo
	 *
	 * @param dealer
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public static void sendCardToMyself(final Dealer dealer) throws KeeperException, InterruptedException {
		BlackjackGameService.log.info("Enviarei uma carta para mim mesmo!");
		Card newCard = dealer.getNewCard();
		BlackjackGameServiceHelper.sendCard(newCard, dealer);
		dealer.addToHand(newCard);
		BlackjackGameService.log.info("Carta recebida!");
	}

	/**
	 * Para cada player em {@link Dealer#getListOfPlayers()}, pede por ações. As
	 * ações são as presentes em {@link BlackjackRoundAction}, e aquelas que não
	 * são bloqueadoras (como {@link BlackjackRoundAction#UMA_CARTA} e
	 * {@link BlackjackRoundAction#DOBRAR}) retornam true ao método
	 * askForAction, fazendo com que uma nova ação seja pedida.
	 *
	 * @param dealer
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public static void askForActions(final Dealer dealer) throws KeeperException, InterruptedException {
		BlackjackGameService.log.info("Perguntarei a cada player pela sua ação!");
		for (Player player : dealer.getListOfPlayers()) {
			BlackjackGameService.log.info(new StringBuilder("Player ").append(player.getName()).append(", como deseja jogar?"));
			while (BlackjackGameServiceHelper.askForAction(dealer, player)) {
				BlackjackGameService.log.info(new StringBuilder("Player ").append(player.getName()).append(" será chamado novamente"));
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

	/**
	 * @param player
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public static void seeTable(final Player player) throws KeeperException, InterruptedException {
		ZookeeperService.getInstance().getDrawnCards(player);
	}

	public static void fillHandUntilMinimum(final Dealer dealer) throws KeeperException, InterruptedException {
		while (dealer.getScore() < 17) {
			BlackjackGameService.log.info(new StringBuilder("Atualmente minha mão está com ").append(dealer.getScore()).append(" pontos"));
			BlackjackGameService.sendCardToMyself(dealer);
		}
	}

	public static void alertPlayersForEndOfRound(final Dealer dealer) throws KeeperException, InterruptedException {
		BlackjackGameService.log.info("Informarei a todos os jogadores que a rodada chegou ao fim!");
		for (Player player : dealer.getListOfPlayers()) {
			player.printHand(true);
			int playerScore = player.getScore();
			System.out.println(playerScore);
			/*
			 * Acorda o Player para que ele também verifique se ganhou
			 */
			BlackjackGameService.log
					.info(new StringBuilder("Informando ao player ").append(player.getName()).append(" que a rodada terminou.").toString());
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
		/*
		 * Limpa tudo que o Dealer tem dos Players, menos o dinheiro.
		 */
		for (Player player : dealer.getListOfPlayers()) {
			BlackjackGameService.log.info(new StringBuilder("Limpando dados do Player ").append(player.getName()).toString());
			player.newRound();
			ZookeeperService.getInstance().removeAllCardsFromPlayer(player);
		}
		/*
		 * Este método remove todos os players sem dinheiro, e devolve-os
		 */
		List<Player> listOfMoneylessPlayer = dealer.getAndRemovePlayerWithoutMoney();
		/*
		 * Expulsa da mesa todos os players sem dinheiro
		 */
		for (Player player : listOfMoneylessPlayer) {
			BlackjackGameService.log.info(new StringBuilder("O player ").append(player.getName())
					.append(" estava sem dinheiro e estará sendo expulso do jogo!").toString());
			ZookeeperService.getInstance().removePlayer(player);
		}
		/*
		 * Tira as cartas também do Dealer, e chama pelo newRound, que além do
		 * funcionamento do newRound do Player, também limpa o rng.
		 */
		BlackjackGameService.log.info("Limpando também minhas cartas!");
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
		int expectedPlayers = dealer.getListOfPlayers().size();
		BlackjackGameService.log.info(
				new StringBuilder("Esperando que pelo menos ").append(expectedPlayers).append(" jogadores estejam na mesa").toString());
		ZookeeperService.getInstance().waitUntilTableIsFull(newRoundNode, expectedPlayers);

		ZookeeperService.getInstance().removeAllChildrenFromNode(newRoundNode);

	}

	public static void registerForNextRound(final Player player) throws KeeperException, InterruptedException {
		ZookeeperService.getInstance().registerPlayerForNextRound(player);
	}

	public static void registerPlayersForRound(final Dealer dealer) throws KeeperException, InterruptedException {
		BlackjackGameService.log.info("Limpando lista de Players e esperando que pelo menos um player entre na fila.");
		dealer.getListOfPlayers().clear();
		List<String> players = ZookeeperService.getInstance().waitUntilTableIsFull(dealer.getMesa(), 1);
		dealer.registerPlayers(players);
	}

}
