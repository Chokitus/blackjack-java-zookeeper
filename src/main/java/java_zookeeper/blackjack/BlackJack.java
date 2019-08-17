package java_zookeeper.blackjack;

import java.io.IOException;

import org.apache.zookeeper.KeeperException;

import java_zookeeper.blackjack.game.player.Dealer;
import java_zookeeper.blackjack.game.player.Player;
import java_zookeeper.blackjack.game.service.BlackjackGameService;
import java_zookeeper.blackjack.zookeeper.ZookeeperPlayerRegister;
import java_zookeeper.blackjack.zookeeper.ZookeeperService;

public class BlackJack {

	public static void main(final String[] args) throws IOException, InterruptedException, KeeperException {
		ZookeeperService.createInstance("localhost:2181");
		if (args[0].equals("dealer")) {
			Dealer dealer = ZookeeperPlayerRegister.registerDealer("001", "NomeDoDealer", 5);
			new BlackJack().playDealerGame(dealer);
		} else {
			Player player = ZookeeperPlayerRegister.registerPlayer("001", "NomeDoPlayer");
			new BlackJack().playPlayerGame(player);
		}
	}

	private void playPlayerGame(final Player player) throws KeeperException, InterruptedException {
		/**
		 * Primeiro passo: responder ao pedido de apostas do dealer
		 */
		BlackjackGameService.bet(player);
		/**
		 * Segundo passo: esperar distribuição das cartas
		 *
		 * TODO: Analisar também a carta das outras pessoas
		 */
		BlackjackGameService.waitForCards(player, 2);
		/**
		 * Terceiro passo: Ver cartas das outras pessoas, do Dealer, suas
		 * próprias cartas e escolher uma ação
		 */
		BlackjackGameService.seeTableAndAct(player);
	}

	private void playDealerGame(final Dealer dealer) throws KeeperException, InterruptedException {
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
		 * Quarto passo: perguntar ação, agora que todos estão com as cartas. As
		 * ações possíveis são: Dobrar (que consiste em dobrar sua aposta e
		 * pegar uma carta), Pedir carta, Parar e Desistir *
		 */
		BlackjackGameService.askForActions(dealer);

	}
}
