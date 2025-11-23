# **Eclipse AI Coder**
**Website Metadata & Assets**
- **Favicon/Icon Files**:
  - `apple-touch-icon.png` (180×180)
  - `favicon-32x32.png` (32×32)
  - `favicon-16x16.png` (16×16)
  - `site.webmanifest` (manifest file)
- **Title**: `Eclipse AI Coder`
- **Logo Image**: `images/title.webp` (400×400px, displayed in header).

---

## **1. Overview**
### **About the Project**
- **Description**: An extension for the [Eclipse IDE](https://eclipseide.org) that brings **AI-driven inline completion** to the editor.

### **Features**
1. **Local Model Support**:
   - Works with locally running models (e.g., [Ollama](https://ollama.com), [LM Studio](https://lmstudio.ai)).
2. **Cloud LLM Support**:
   - Supports **Codestral** (by [Mistral AI](https://mistral.ai))—requires a user-provided API key.
3. **Completion Triggers**:
   - **Automatic** or **manual** via shortcut:
     - **Inline completion**: `Ctrl` + `Shift` + `Space`
     - **Complete selected code**: `Ctrl` + `Shift` + `Space`
4. **Code Generation/Editing**:
   - **Inline edit/generate** via instructions: `Ctrl` + `Shift` + `1`
5. **Context Control**:
   - View and modify the prompt context.
6. **Insights**:
   - Token count, request durations, and status for each request.
7. **Java-Specific Features**:
   - Deep understanding of Java projects (adds imports, variables in scope to prompts).
8. **Multiline Toggle**:
   - Switch between single-line and multiline completions.

### **Installation**
- **Option A**: Install from [Eclipse Marketplace](https://marketplace.eclipse.org/content/ai-coder).
- **Option B**: Use the update site:
  ```plaintext
  https://hetzge.github.io/aicoder/eclipse/beta1
  ```
- *(Commented-out Option C: Direct JAR download link removed.)*

---

## **2. Screenshots**
**Gallery Images** (all 400px height, `object-fit: contain`):
1. `images/example.png` – Example Screenshot
2. `images/context.png` – Context Screenshot
3. `images/history.png` – History Screenshot

---

## **3. Comparison with Other Solutions**
| Feature                     | Eclipse AI Coder       | [Tabnine](https://www.tabnine.com) | [GitHub Copilot](https://github.com/features/copilot) | [Copilot4Eclipse](https://www.genuitec.com/products/copilot4eclipse) | [Tabby](https://www.tabbyml.com) | [GitLab Duo](https://marketplace.eclipse.org/content/gitlab-eclipse) | [Amazon Q](https://marketplace.eclipse.org/content/amazon-q) |
|-----------------------------|------------------------|------------------------------------|-------------------------------------------------------|----------------------------------------------------------------------|----------------------------------|-----------------------------------------------------------------------|---------------------------------------------------------------|
| **Inline Completion**       | ✅                     | ✅                                 | ✅                                                    | ✅                                                                   | ✅                               | ✅                                                                    | ✅                                                            |
| **Eclipse Support**         | ✅                     | ✅                                 | ✅                                                    | ✅                                                                   | ✅                               | ✅                                                                    | ✅                                                            |
| **Non-commercial**          | ✅                     | ❌                                 | ❌                                                    | ❌                                                                   | ❌                               | ❌                                                                    | ❌                                                            |
| **Open Source**             | ✅                     | ❌                                 | ❌                                                    | ❌                                                                   | ✅                               | ❌                                                                    | ❌                                                            |
| **Manual Trigger**          | ✅                     | ❌                                 | ❌                                                    | ✅                                                                   | ✅                               | ❌                                                                    | ❌                                                            |
| **Bring Your Own Key**      | ✅                     | ❌                                 | ❌                                                    | ❌                                                                   | ✅                               | ❌                                                                    | ❌                                                            |
| **Local LLMs**              | ✅                     | ❌                                 | ❌                                                    | ❌                                                                   | ✅                               | ❌                                                                    | ❌                                                            |
| **Toggle Multiline**        | ✅                     | ❌                                 | ❌                                                    | ❌                                                                   | ❌                               | ❌                                                                    | ❌                                                            |
| **Context Control**         | ✅                     | ❌                                 | ❌                                                    | ❌                                                                   | ❌                               | ❌                                                                    | ❌                                                            |
| **Next Edit Suggestion**    | ❌                     | ❌                                 | ✅                                                    | ❌                                                                   | ❌                               | ❌                                                                    | ❌                                                            |
| **LLM Providers**           | Local, Mistral         | Tabnine                            | GitHub Copilot                                        | GitHub Copilot                                                       | Deepseek, Local, Mistral         | GitLab                                                                 | Amazon                                                       |

---

## **4. LLM Providers**
### **Codestral (Mistral AI)**
- **API Key**: Free (currently). Get one [here](https://console.mistral.ai/codestral).
- **Configuration**:
  - Set the API key in Eclipse preferences (screenshot: `images/settings.png`).

### **Ollama**
- **Setup Steps**:
  1. Install Ollama ([download](https://ollama.com/download)).
  2. Pull a model (e.g., [Qwen2.5-Coder](https://ollama.com/library/qwen2.5-coder)):
     ```bash
     ollama pull qwen2.5-coder:3b
     ```
  3. Configure in Eclipse:
     - **Base URL**: `http://localhost:11434`
     - **Model**: `qwen2.5-coder:3b`
     *(Models must support suffix input; example: `images/ollama-template.png`)*

### **OpenAI-Compatible APIs**
- **Example**: [OpenRouter](https://openrouter.ai/api) (base URL: `https://openrouter.ai/api`).
- **Note**: Many OpenAI models may not support "fill-in-the-middle" tasks.

---

## **5. Documentation**
### **Controls (Shortcuts)**
| Shortcut                     | Selection | Action                                                                                     |
|------------------------------|-----------|--------------------------------------------------------------------------------------------|
| `Ctrl` + `Shift` + `Space`   | ❌        | Trigger inline completion at cursor position.                                              |
| `Ctrl` + `Shift` + `Space`   | ✅        | Trigger completion/correction of selected code.                                           |
| `Ctrl` + `Shift` + `1`       | ❌        | Open dialog to generate code at cursor via instruction.                                    |
| `Ctrl` + `Shift` + `1`       | ✅        | Open dialog to edit selected code via instruction.                                         |
| `Ctrl` + `3`                 | ❌        | Open quick access for code generation instructions at cursor.                               |
| `Ctrl` + `3`                 | ✅        | Open quick access for editing instructions on selected code.                                |

### **Settings**
| Setting                      | Description                                                                                 |
|------------------------------|---------------------------------------------------------------------------------------------|
| **Enable Multiline Completion** | Allow multiline suggestions.                                                                |
| **Enable Autocomplete**       | Trigger completions automatically (or only via shortcut if disabled).                      |
| **Only on Change Autocomplete**| Trigger only after file changes (e.g., typing).                                             |
| **Ignore JRE Classes**       | Exclude Java Runtime Environment classes from context (assumed known by LLMs).              |
| **Cleanup Code on Apply**    | Format Java code when applying suggestions.                                                 |
| **Maximum Prefix Size**      | Lines before cursor to include in prompt.                                                  |
| **Maximum Suffix Size**      | Lines after cursor to include in prompt.                                                   |
| **Maximum Tokens**           | Limit tokens generated by the LLM.                                                          |
| **Debounce (ms)**            | Delay before autocompletion triggers (to avoid rapid recalculations).                      |

### **Contexts (Prompt Inclusions)**
| Context               | Description                                                                 | Filetype |
|-----------------------|-----------------------------------------------------------------------------|----------|
| **Project Informations** | Project name, location, Java version (for Java projects).                   | All      |
| **File Tree**         | Tree structure of project files.                                            | All      |
| **Dependencies**       | List of project dependencies (WIP).                                         | All      |
| **Open Editors**      | Content of currently open files.                                            | All      |
| **Super (Java)**      | Interfaces of supertypes in the current Java file.                          | Java     |
| **Scope (Java)**      | Interfaces of types in the current scope.                                   | Java     |
| **Imports (Java)**    | Interfaces of imported types.                                               | Java     |
| **Package (Java)**    | Interfaces of types in the current package.                                | Java     |
| **User**              | Custom user-provided content.                                               | All      |
| **Last Edits**        | Source code around recently edited locations.                               | All      |
| **Clipboard**         | Content of the clipboard.                                                   | All      |
| **Fill in Middle**    | Current edit location + surrounding context lines.                          | All      |

---

## **7. Feedback**
- **Report Issues**: [GitHub Issues](https://github.com/hetzge/eclipse-ai-coder/issues).
- **Star the Project**: [GitHub Repository](https://github.com/hetzge/eclipse-ai-coder).

---
## **Technical Notes**
- **Styling**: The page uses a gradient background (`#2A7B9B` → `#57C785` → `#EDDD53`), tabbed interface with JavaScript (`openTab` function), and responsive design (adjusts for screens <768px).
- **Images**: All screenshots are hosted in `/images/` (e.g., `codestral.png`, `settings.png`).
- **Favicon**: Uses PNG icons and a manifest file for PWA support.