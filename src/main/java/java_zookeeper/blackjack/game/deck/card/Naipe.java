package java_zookeeper.blackjack.game.deck.card;

import lombok.Getter;

@Getter
public enum Naipe {
	COPAS(" de Copas"), PAUS(" de Paus"), ESPADAS(" de Espadas"), OUROS(" de Ouros");

	private String descricao;

	Naipe(final String descricao) {
		this.descricao = descricao;
	}

	@Override
	public String toString() {
		return this.descricao;
	}

	public static Naipe getNaipe(final int ceil) {
		return Naipe.values()[ceil - 1];
	}
}
