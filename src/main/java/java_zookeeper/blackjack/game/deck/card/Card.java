package java_zookeeper.blackjack.game.deck.card;

import java.io.Serializable;

import org.apache.commons.lang3.SerializationUtils;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class Card implements Serializable {

	private static final long serialVersionUID = 1L;

	private Naipe naipe;
	private char valor;

	private Card(final Naipe naipe, final char valor) {
		this.setNaipe(naipe);
		this.setValor(valor);
	}

	public static Card getCard(final int card) {
		int pos = (card - 1) / 13;
		Naipe naipe = Naipe.getNaipe(pos + 1);
		char valor = Card.parseValor(card - pos * 13);
		return new Card(naipe, valor);
	}

	private static char parseValor(final int valor) {
		switch (valor) {
			case 1 :
				return 'A';
			case 10 :
				return 'D';
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
			case 'D' :
				return 10;
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

	public String getValor() {
		return this.valor == 'D' ? "10" : String.valueOf(this.valor);
	}

	@Override
	public String toString() {
		return new StringBuilder().append(this.getValor()).append(this.getNaipe().toString()).toString();
	}

	public byte[] getBytes() {
		return SerializationUtils.serialize(this);
	}

	public static Card cardFromBytes(final byte[] bytes) {
		return SerializationUtils.deserialize(bytes);
	}
}
