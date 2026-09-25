package de.hetzge.eclipse.aicoder.skill;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.yaml.snakeyaml.Yaml;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.util.LambdaExceptionUtils;
import mjson.Json;

public final class StandardSkillUtils {

	private StandardSkillUtils() {
	}

	public static List<Skill> loadStandardSkills() throws IOException {
		final List<Skill> skillWorkspaceEntries = new ArrayList<>();
		skillWorkspaceEntries.addAll(loadStandardSkills(Path.of(System.getProperty("user.home")).resolve(".claude/skills"), "claude"));
		skillWorkspaceEntries.addAll(loadStandardSkills(Path.of(System.getProperty("user.home")).resolve(".cursor/skills"), "cursor"));
		skillWorkspaceEntries.addAll(loadStandardSkills(Path.of(System.getProperty("user.home")).resolve(".agent/skills"), "agent"));
		skillWorkspaceEntries.addAll(loadStandardSkills(Path.of(System.getProperty("user.home")).resolve(".aicoder/skills"), "aicoder"));
		return skillWorkspaceEntries;
	}

	private static List<Skill> loadStandardSkills(Path folder, String prefix) throws IOException {
		if (!folder.toFile().exists()) {
			return List.of();
		}
		return Files.list(folder)
				.filter(Files::isDirectory)
				.map(LambdaExceptionUtils.rethrowFunction(path -> loadStandardSkill(path, prefix)))
				.flatMap(Optional::stream)
				.toList();
	}

	private static Optional<Skill> loadStandardSkill(Path folder, String prefix) throws IOException {
		try {
			if (!Files.exists(folder)) {
				return Optional.empty();
			}
			final String key = prefix + "-" + folder.getFileName().toString();
			final Path skillMdPath = folder.resolve("SKILL.md");
			if (!Files.exists(skillMdPath)) {
				return Optional.empty();
			}
			final SkillMd skillMd = loadSkillMd(skillMdPath);
			final String title = prefix + " - " + skillMd.frontmatter().at("name", key).asString();
			final String description = Optional.ofNullable(skillMd.frontmatter().at("description")).map(Json::asString).orElse(null);
			return Optional.of(new Skill(key, title, description, skillMd));
		} catch (final Exception exception) {
			AiCoderActivator.log().warn("Failed to load skill from " + folder.toString(), exception);
			return Optional.empty();
		}
	}

	private static SkillMd loadSkillMd(Path path) throws Exception {
		final String content = Files.readString(path);
		// 1. Split frontmatter from body
		String fmBlock = "";
		String body = content;
		if (content.startsWith("---")) {
			final int end = content.indexOf("\n---", 3);
			if (end != -1) {
				fmBlock = content.substring(3, end).trim();
				body = content.substring(end + 4).trim(); // skip closing ---
			}
		}
		// 2. Parse YAML frontmatter
		final Json frontmatter = Json.make(new Yaml().load(fmBlock));
		return new SkillMd(path, frontmatter, body);
	}

}
