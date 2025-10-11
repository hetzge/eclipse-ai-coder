package de.hetzge.eclipse.aicoder.content;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IPath;

import mjson.Json;

public final class InstructionStorage {

	private static final String INSTRUCTIONS_JSON_FILENAME = "editInstructions.json";
	private static final String LAST_INSTRUCTION_JSON_FILENAME = "lastInstruction.json";
	private static final int MAX_INSTRUCTIONS = 10;

	private final IPath stateLocation;
	private final LinkedList<EditInstruction> instructions;
	private EditInstruction lastInstruction;

	private InstructionStorage(IPath stateLocation, List<EditInstruction> instructions, EditInstruction lastInstruction) {
		this.stateLocation = stateLocation;
		this.instructions = new LinkedList<>(instructions);
		this.lastInstruction = lastInstruction;
	}

	public void addEditInstruction(String instruction) throws IOException {
		if (instruction.isBlank()) {
			return;
		}
		addEditInstruction(new EditInstruction("#" + createDateTimeString(), "History: " + ellipsis(instruction), instruction));
	}

	public void addEditInstruction(EditInstruction editInstruction) throws IOException {
		// Remove old instruction with same content
		for (int i = 0; i < this.instructions.size(); i++) {
			if (this.instructions.get(i).content().trim().equals(editInstruction.content().trim())) {
				this.instructions.remove(i);
				break;
			}
		}
		// Add new instruction
		this.instructions.add(editInstruction);
		// Remove oldest instruction if there are more than max instructions
		if (this.instructions.size() > MAX_INSTRUCTIONS) {
			this.instructions.removeFirst();
		}
		persistInstructions();
	}

	public void setLastInstruction(String instruction) throws IOException {
		if (instruction.isBlank()) {
			return;
		}
		this.lastInstruction = new EditInstruction(createDateTimeString(), "Autosave: " + ellipsis(instruction), instruction);
		persistLastInstruction(this.lastInstruction);
	}

	public List<EditInstruction> getEditInstructions() {
		return this.instructions;
	}

	public EditInstruction getLastInstruction() {
		return this.lastInstruction;
	}

	private void persistInstructions() throws IOException {
		final File file = this.stateLocation.append(INSTRUCTIONS_JSON_FILENAME).toFile();
		try (FileWriter fileWriter = new FileWriter(file)) {
			fileWriter.write(Json.array(this.instructions.stream().map(EditInstruction::toJson).toArray()).toString());
		}
	}

	private void persistLastInstruction(EditInstruction lastInstruction) throws IOException {
		final File file = this.stateLocation.append(LAST_INSTRUCTION_JSON_FILENAME).toFile();
		try (FileWriter fileWriter = new FileWriter(file)) {
			fileWriter.write(lastInstruction.toJson().toString());
		}
	}

	public static InstructionStorage load(IPath stateLocation) throws IOException {
		final List<EditInstruction> instructions = loadInstructions(stateLocation);
		final EditInstruction lastInstruction = loadLastInstruction(stateLocation);
		return new InstructionStorage(stateLocation, instructions, lastInstruction);
	}

	private static List<EditInstruction> loadInstructions(IPath stateLocation) throws IOException, FileNotFoundException {
		final File instructionsFile = stateLocation.append(INSTRUCTIONS_JSON_FILENAME).toFile();
		if (!instructionsFile.exists()) {
			return List.of();
		}
		try (FileInputStream fileInputStream = new FileInputStream(instructionsFile)) {
			final String fileContent = new String(fileInputStream.readAllBytes(), StandardCharsets.UTF_8);
			return Json.read(!fileContent.isBlank() ? fileContent : "[]")
					.asJsonList()
					.stream()
					.map(EditInstruction::fromJson)
					.toList();
		}
	}

	private static String ellipsis(String text) {
		if (text.contains("\n")) {
			return text.substring(0, text.indexOf("\n")) + "...";
		}
		if (text.length() > 100) {
			return text.substring(0, 100) + "...";
		}
		return text;
	}

	private static String createDateTimeString() {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
	}

	private static EditInstruction loadLastInstruction(IPath stateLocation) throws IOException, FileNotFoundException {
		final File lastInstructionFile = stateLocation.append(LAST_INSTRUCTION_JSON_FILENAME).toFile();
		if (!lastInstructionFile.exists()) {
			return new EditInstruction("", "", "");
		}
		try (FileInputStream fileInputStream = new FileInputStream(lastInstructionFile)) {
			final String fileContent = new String(fileInputStream.readAllBytes(), StandardCharsets.UTF_8);
			return EditInstruction.fromJson(Json.read(!fileContent.isBlank() ? fileContent : new EditInstruction("", "", "").toJson().toString()));
		}
	}
}
