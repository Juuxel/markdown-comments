package cuchaz.enigma.translation.mapping;

import javax.annotation.Nullable;

import net.fabricmc.mappingio.CommentStyle;

public record Javadoc(String comment, CommentStyle commentStyle) {
	public static final Javadoc EMPTY = html("");

	public boolean isEmpty() {
		return comment.isEmpty();
	}

	public boolean isBlank() {
		return comment.isBlank();
	}

	public static Javadoc html(String comment) {
		return new Javadoc(comment, CommentStyle.HTML);
	}

	@Nullable
	public static Javadoc ofNullable(@Nullable String comment, CommentStyle commentStyle) {
		return comment != null ? new Javadoc(comment, commentStyle) : null;
	}

	public static Javadoc nullToEmpty(@Nullable Javadoc javadoc) {
		return javadoc != null ? javadoc : EMPTY;
	}
}
