package cuchaz.enigma.translation.mapping.serde;

import java.util.ArrayList;
import java.util.List;

import cuchaz.enigma.translation.mapping.AccessModifier;
import cuchaz.enigma.translation.mapping.EntryMapping;

import cuchaz.enigma.translation.mapping.Javadoc;

import net.fabricmc.mappingio.CommentStyle;

public final class RawEntryMapping {
	private final String targetName;
	private final AccessModifier access;
	private final List<String> javadocs = new ArrayList<>();
	private CommentStyle javadocStyle = CommentStyle.HTML;

	public RawEntryMapping(String targetName) {
		this(targetName, AccessModifier.UNCHANGED);
	}

	public RawEntryMapping(String targetName, AccessModifier access) {
		this.access = access;
		this.targetName = targetName != null && !targetName.equals("-") ? targetName : null;
	}

	public void addJavadocLine(String line) {
		javadocs.add(line);
	}

	public void setJavadocStyle(CommentStyle javadocStyle) {
		this.javadocStyle = javadocStyle;
	}

	public EntryMapping bake() {
		return new EntryMapping(targetName, access, javadocs.isEmpty() ? null : new Javadoc(String.join("\n", javadocs), javadocStyle));
	}
}
