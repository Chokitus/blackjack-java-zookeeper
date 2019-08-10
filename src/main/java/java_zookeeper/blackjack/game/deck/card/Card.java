package java_zookeeper.blackjack.game.deck.card;

import java.io.Serializable;

import org.apache.commons.lang3.SerializationUtils;

import java_zookeeper.blackjack.game.player.Player;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Card implements Serializable {
	private Naipe naipe;
	private char valor;

	private Card(final Naipe naipe, final char valor) {
		this.setNaipe(naipe);
		this.setValor(valor);
	}

	public static Card getCard(final int card) {
		Naipe naipe = Naipe.getNaipe((int) Math.ceil(card / 13.0));
		char valor = Card.parseValor((int) Math.ceil(card / 4.0));
		return new Card(naipe, valor);
	}

	private static char parseValor(final int valor) {
		switch (valor) {
			case 1 :
				return 'A';
			case 11 :
				return 'J';
			case 12 :
				return 'Q';
			case 13 :
				return 'K';
			default :
				return Character.forDigit(valor, 10);
		}
	}

	public int getNumericValue(final int score) {
		switch (this.valor) {
			case 'A' :
				return score > 10 ? 1 : 11; // O Ás vale como 11 e como 1,
											// dependendo da sua mão
			case 'J' :
				return 10;
			case 'Q' :
				return 10;
			case 'K' :
				return 10;
			default :
				return Character.getNumericValue(this.valor);
		}
	}

	@Override
	public String toString() {
		return new StringBuilder().append(this.getValor()).append(this.getNaipe().toString()).toString();
	}

	public byte[] serialize(final Player player) {

		// TODO: Criptografar (da forma mais simples possível) a carta usando a
		// chave (Player.getKey()). Essa parte eu vou ver mais pra frente.

		return SerializationUtils.serialize(this);
	}

	public static Card cardFromBytes(final byte[] bytes) {

		// TODO: Mesma coisa, decriptografar os bytes usando a chave. A ideia é,
		// portanto, criar uma criptografia revertível com uma única chave, tem
		// milhares na internet, vou ver uma ainda.

		return SerializationUtils.deserialize(bytes);
	}
}
