package cuchaz.enigma.gui.newabstraction;

import net.fabricmc.mappingio.CommentStyle;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.translation.mapping.Javadoc;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.utils.validation.Message;
import cuchaz.enigma.utils.validation.ValidationContext;

public class EntryValidation {
	public static boolean validateJavadoc(ValidationContext vc, Javadoc javadoc) {
		if (javadoc.commentStyle() == CommentStyle.HTML && javadoc.comment().contains("*/")) {
			vc.raise(Message.ILLEGAL_DOC_COMMENT_END);
			return false;
		}

		return true;
	}

	public static boolean validateRename(ValidationContext vc, EnigmaProject p, Entry<?> entry, String newName) {
		return p.getMapper().getValidator().validateRename(vc, entry, newName);
	}
}
