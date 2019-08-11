package java_zookeeper.blackjack.game.service;

import java.util.Arrays;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.zookeeper.KeeperException;

import java_zookeeper.blackjack.game.player.Player;
import java_zookeeper.blackjack.zookeeper.ZookeeperService;

public class BlackjackGameService {

	public static final String APOSTE_STRING = "Um novo round começou! Mande-me sua aposta!";
	public static final byte[] APOSTE = BlackjackGameService.APOSTE_STRING.getBytes();

	public static void askForBet(final Player player) throws KeeperException, InterruptedException {
		String name = ZookeeperService.getCorrectNodeName(player);
		synchronized (ZookeeperService.mutex) {
			ZookeeperService.getInstance().zk.setData(name, BlackjackGameService.APOSTE, 0);
			byte[] aposta = ZookeeperService.getInstance().zk.getData(name, ZookeeperService.getInstance(), null);
			while (Arrays.equals(aposta, BlackjackGameService.APOSTE)) {
				ZookeeperService.mutex.wait();
				aposta = ZookeeperService.getInstance().zk.getData(name, ZookeeperService.getInstance(), null);
			}
			Integer apostaNumerica = SerializationUtils.deserialize(aposta);
			System.out.println("Chegou a aposta: " + apostaNumerica);

			// OK, ele envia a aposta numérica, agora é armazenar a aposta

		}
	}

	public static void bet(final Player player) throws KeeperException, InterruptedException {
		String path = ZookeeperService.getCorrectNodeName(player);
		byte[] data = ZookeeperService.getInstance().zk.getData(path, ZookeeperService.getInstance(), null);
		if (!Arrays.equals(BlackjackGameService.APOSTE, data)) {
			Object deserialize = SerializationUtils.deserialize(data);
			System.out.println("Mensagem estranha do Dealer: " + deserialize);
		} else {
			System.out.println(BlackjackGameService.APOSTE_STRING);
		}
		byte[] myBet = SerializationUtils.serialize(Integer.valueOf(100));
		ZookeeperService.getInstance().zk.setData(path, myBet, 1);
	}

}
