package java_zookeeper.blackjack.game.actions;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum BlackjackRoundAction {
	DOBRAR("2"), UMA_CARTA("+"), PARAR("ok"), DESISTIR("x"), ESTOUREI(">21");

	@Getter
	private String command;

	public static Optional<BlackjackRoundAction> parseAction(final String maybeAction) {
		if (StringUtils.isBlank(maybeAction)) {
			return Optional.empty();
		}
		for (BlackjackRoundAction action : BlackjackRoundAction.values()) {
			if (action.getCommand().equals(maybeAction)) {
				return Optional.of(action);
			}
		}
		return Optional.empty();
	}
}
