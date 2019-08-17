package java_zookeeper.blackjack.zookeeper;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

public class ZookeeperServiceWrong implements Watcher {

	@Override
	public void process(final WatchedEvent event) {
		// TODO Auto-generated method stub

	}
	//
	// private static ZookeeperServiceWrong instance;
	// static Integer mutex = Integer.valueOf(-1);
	//
	// private ZooKeeper zk;
	// private CountDownLatch latch = new CountDownLatch(1);
	//
	// private ZookeeperServiceWrong() {
	//
	// }
	//
	// public static ZookeeperServiceWrong getInstance() throws IOException,
	// InterruptedException {
	// if (ZookeeperServiceWrong.instance == null) {
	// ZookeeperServiceWrong.instance = new ZookeeperServiceWrong();
	// ZookeeperServiceWrong.instance.connect("localhost:2181");
	// }
	// return ZookeeperServiceWrong.instance;
	// }
	//
	// public void connect(final String host) throws IOException,
	// InterruptedException {
	// this.zk = new ZooKeeper(host, 5000, this);
	// System.out.println(this.zk);
	// this.latch.await();
	// }
	//
	// public void close() throws InterruptedException {
	// this.zk.close();
	// }
	//
	// private ZooKeeper getZooKeeper() {
	// if (this.zk == null || !this.zk.getState().equals(States.CONNECTED)) {
	// throw new IllegalStateException("ZooKeeper is not connected.");
	// }
	// return this.zk;
	// }
	//
	// /**
	// * <p>
	// * Cria uma nova mesa no Jogo, sendo seu nome conhecido entre todos os
	// * jogadores. Este método também adiciona um Watch no nó criado, a ser
	// usado
	// * pelo Dealer, conforme necessidade descrita em
	// * {@link #createNewPlayerNode(String, Player, int)}
	// * </p>
	// *
	// * @param mesa
	// * @return
	// * @throws KeeperException
	// * @throws InterruptedException
	// */
	// public synchronized List<String> createNewMesa(final String mesa) throws
	// KeeperException, InterruptedException {
	//
	// // TODO: Verificar que o Watch foi colocado e como usá-lo.
	//
	// String mesaId = this.getZooKeeper().create("/" + mesa, null,
	// Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
	//
	// return this.getZooKeeper().getChildren("/" + mesa, this);
	// }
	//
	// /**
	// * <p>
	// * Cria um novo Player na mesa, passando o nome da mesa, o player e a sua
	// * chave. A ideia é que um Player espere o Dealer criar a mesa, e se
	// * registre nesta mesa. Ao criar o nó, o Player deixará a sua chave no nó.
	// O
	// * Dealer deverá ter um Watch na mesa, pois assim que um novo Player criar
	// * um nó, o Dealer deverá olhar para a chave da mesma, memorizá-la (num
	// * HashMap), e sobrescrever a chave. Assim, fica acordado uma chave
	// * (presumidamente única) para cada jogador.
	// * </p>
	// * <p>
	// * Assim que pelo menos <b> n jogadores </> entrarem, o jogo começa.
	// * </p>
	// *
	// * @param mesa
	// * @param player
	// * @param key
	// * @return
	// * @throws KeeperException
	// * @throws InterruptedException
	// */
	// public String createNewPlayerNode(final Player player, final int key)
	// throws KeeperException, InterruptedException {
	// if (this.getZooKeeper().exists("/" + player.getMesa(), false) == null) {
	// throw new IllegalStateException("Um dealer deve ser decidido antes que os
	// players se registrem.");
	// }
	// return this.getZooKeeper().create("/" + player.getMesa() + "/" +
	// player.getName(), String.valueOf(key).getBytes(),
	// Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
	// }
	//
	// public String enviarCardParaPlayer(final Player player, final Card card)
	// throws KeeperException, InterruptedException {
	// return this.createZNode(player, card.serialize(player));
	// }
	//
	// public String createZNode(final Player player, final byte[] data) throws
	// KeeperException, InterruptedException {
	// return this.getZooKeeper().create("/" + player.getMesa() + "/" +
	// player.getName(), data, Ids.OPEN_ACL_UNSAFE,
	// CreateMode.PERSISTENT_SEQUENTIAL);
	// }
	//
	// @Override
	// synchronized public void process(final WatchedEvent event) {
	// if (event.getState() == KeeperState.SyncConnected) {
	// ZookeeperServiceWrong.this.latch.countDown();
	// return;
	// }
	// synchronized (ZookeeperServiceWrong.mutex) {
	// ZookeeperServiceWrong.mutex.notifyAll();
	// }
	// }
	//
	// public synchronized void awaitPlayerRegistration() throws
	// InterruptedException {
	// ZookeeperServiceWrong.mutex.wait();
	// }

}
