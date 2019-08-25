package java_zookeeper.blackjack;

import org.apache.zookeeper.KeeperException;

import java_zookeeper.blackjack.game.player.Dealer;
import java_zookeeper.blackjack.game.player.Player;
import java_zookeeper.blackjack.game.service.BlackjackGameService;
import java_zookeeper.blackjack.game.service.BlackjackGameServiceHelper;
import java_zookeeper.blackjack.zookeeper.ZookeeperPlayerRegister;
import java_zookeeper.blackjack.zookeeper.ZookeeperService;
import lombok.extern.log4j.Log4j;

@Log4j
public class BlackJack {

	public static void main(final String[] args) throws InterruptedException, KeeperException {

		ZookeeperService.createInstance("localhost:2181");
		BlackJack.log.info("Olá! Bem-vindo ao Blackjack, informe-me seu nome!");
		String nomeDoPlayer = BlackjackGameServiceHelper.input.nextLine();
		ZookeeperPlayerRegister.electLeader("001", nomeDoPlayer);
	}

	public void play(final int waitFor, final String mesa, final String nomeDoPlayer, final boolean firstTime)
			throws KeeperException, InterruptedException {
		if (waitFor == 0) {
			Player player = ZookeeperPlayerRegister.registerPlayer(mesa, nomeDoPlayer, firstTime);
			this.playPlayerGame(player);
		} else {
			Dealer dealer = ZookeeperPlayerRegister.registerDealer(mesa, nomeDoPlayer, 2);
			this.playDealerGame(dealer);
		}
	}

	private void playPlayerGame(final Player player) throws KeeperException, InterruptedException {
		while (player.getCurrentMoney() != 0) {
			/*
			 * Primeiro passo: responder ao pedido de apostas do dealer
			 */
			BlackjackGameService.bet(player);
			/*
			 * Segundo passo: esperar distribuição das cartas
			 */
			BlackjackGameService.waitForCards(player, 2);
			/*
			 * Terceiro passo: espera até que um pedido por ação chegue
			 */
			BlackjackGameService.waitUntilAskedForActions(player);
			/*
			 * Quarto passo: Olha para a mão das outras pessoas (e, em especial,
			 * do Dealer)
			 */
			BlackjackGameService.seeTable(player);
			/*
			 * Quinto passo: escolher uma (ou mais) ação(ões)
			 */
			BlackjackGameService.act(player);
			/*
			 * Sexto passo: esperar Dealer anunciar fim da rodada
			 */
			BlackjackGameService.waitUntilEndOfRound(player);
			/*
			 * Sétimo passo: ver se ganhamos
			 */
			BlackjackGameService.verifyPlayerResults(player);
			player.printHand(true);
			BlackJack.log.info(player.getScore());
			if (player.getCurrentMoney() <= 0) {
				return;
			}
			/*
			 * Oitavo passo: resetar
			 */
			player.newRound();
			/*
			 * Nono passo: se registrar para o próximo round
			 */
			BlackjackGameService.registerForNextRound(player);
			/*
			 * Décimo passo: aguardar Dealer anunciar novo round
			 */
			BlackjackGameService.waitUntilNextRound(player);
		}
	}

	private void playDealerGame(final Dealer dealer) throws KeeperException, InterruptedException {
		while (true) {
			BlackjackGameService.registerPlayersForRound(dealer);
			/**
			 * Primeiro passo: Pedir apostas
			 */
			BlackjackGameService.askForBet(dealer);
			/**
			 * Segundo passo: distribuir cartas
			 */
			BlackjackGameService.distributeCards(dealer);
			/**
			 * Terceiro passo: dar carta para si mesmo
			 */
			BlackjackGameService.sendCardToMyself(dealer);
			/**
			 * Quarto passo: perguntar ação, agora que todos estão com as
			 * cartas. As ações possíveis são: Dobrar (que consiste em dobrar
			 * sua aposta e pegar uma carta), Pedir carta, Parar e Desistir *
			 */
			BlackjackGameService.askForActions(dealer);
			/*
			 * Quinto passo: O Dealer, após todos os jogadores estarem
			 * confortáveis com suas mãos, completa sua mão até dar 17
			 */
			BlackjackGameService.fillHandUntilMinimum(dealer);
			/*
			 * Sexto passo: Todos os players finalizaram sua mão, o Dealer
			 * completou sua mão até 17 (no mínimo), e assim, o Dealer deverá
			 * verificar os ganhadores, pagando-os ou pegando suas apostas.
			 */
			BlackjackGameService.alertPlayersForEndOfRound(dealer);
			if (dealer.getListOfPlayers().isEmpty()) {
				return;
			}
			/*
			 * Sétimo passo: espera até que todos os Players se registrem para o
			 * próximo round.
			 */
			BlackjackGameService.waitUntilAllPlayersAreReady(dealer);
			/*
			 * Aqui devemos resetar
			 */
			BlackjackGameService.cleanTableForNextRound(dealer);
		}
	}
}
