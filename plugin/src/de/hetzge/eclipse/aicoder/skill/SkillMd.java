package de.hetzge.eclipse.aicoder.skill;

import java.nio.file.Path;

import mjson.Json;

public record SkillMd(Path path, Json frontmatter, String body) {

}
