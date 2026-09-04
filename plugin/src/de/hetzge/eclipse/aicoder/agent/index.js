const SCROLL_TOLERANCE = 20;

function isScrolledToBottom() {
	const scrollableHeight = document.documentElement.scrollHeight - window.innerHeight;
	return (scrollableHeight - window.scrollY) <= SCROLL_TOLERANCE;
}

function scrollToBottom() {
	window.scrollTo(0, document.documentElement.scrollHeight);
}

function createButton({ text, onClick }) {
	const button = document.createElement('button');
	button.style.cursor = 'pointer';
	button.textContent = text;
	button.addEventListener('click', onClick);
	return button;
}

function addMessage({ role, content, reasoning, timestamp }) {
	// ----------------------------------------------------
	const openResultButton = createButton({
		text: 'Open in result',
		onClick: () => {
			window.openResult(content);
		}
	});
	// ----------------------------------------------------
	const roleSpan = document.createElement('span');
	roleSpan.textContent = role;
	roleSpan.style.fontWeight = 'bold';
	roleSpan.style.marginRight = '10px';
	// ----------------------------------------------------
	const timestampSpan = document.createElement('span');
	timestampSpan.textContent = (timestamp && new Date(timestamp).toLocaleTimeString()) || "";
	timestampSpan.style.color = 'gray';
	// ----------------------------------------------------
	const header = document.createElement('div');
	header.style.display = 'flex';
	header.style.alignItems = 'center';
	header.style.justifyContent = 'space-between';
	header.style.borderBottom = '1px solid #e0e0e0';
	header.style.color = '#444';
	header.style.marginBottom = '6px';
	header.style.paddingBottom = '4px';
	header.append(roleSpan);
	header.append(timestampSpan);
	// ----------------------------------------------------
	const reasoningContainer = document.createElement('div');
	reasoningContainer.style.fontStyle = 'italic';
	reasoningContainer.style.color = '#666';
	reasoningContainer.style.marginBottom = '4px';
	reasoningContainer.style.whiteSpace = 'pre-wrap';
	reasoningContainer.textContent = reasoning;
	// ----------------------------------------------------
	const contentContainer = document.createElement('div');
	contentContainer.style.color = '#333';
	contentContainer.style.whiteSpace = 'pre-wrap';
	contentContainer.textContent = content;
	// ----------------------------------------------------
	const body = document.createElement('div');
	body.append(reasoningContainer);
	body.append(contentContainer);
	// ----------------------------------------------------
	const showFooter = role === 'ASSISTANT' && content.trim().length > 0;
	const footer = document.createElement('div');
	footer.style.display = showFooter ? 'flex' : 'none';
	footer.style.justifyContent = 'flex-end';
	footer.append(openResultButton);
	// ----------------------------------------------------
	const container = document.createElement('div');
	container.style.background = '#fff';
	container.style.border = '1px solid #c8c8c8';
	container.style.borderRadius = '2px';
	container.style.boxShadow = '0 1px 2px rgba(0, 0, 0, 0.08)';
	container.style.margin = '6px 0';
	container.style.padding = '8px';
	container.append(header);
	container.append(body);
	container.append(footer);
	// ----------------------------------------------------
	const wasAtBottom = isScrolledToBottom();
	document.body.append(container);
	if (wasAtBottom) {
		scrollToBottom();
	}
}

function addError({ content }) {
	// ----------------------------------------------------
	const container = document.createElement('div');
	container.textContent = content;
	container.style.whiteSpace = 'pre-wrap';
	container.style.color = 'red';
	container.style.background = '#fff';
	container.style.border = '1px solid #c8c8c8';
	container.style.borderRadius = '2px';
	container.style.boxShadow = '0 1px 2px rgba(0, 0, 0, 0.08)';
	container.style.margin = '6px 0';
	container.style.padding = '8px';
	// ----------------------------------------------------
	const wasAtBottom = isScrolledToBottom();
	document.body.append(container);
	if (wasAtBottom) {
		scrollToBottom();
	}
}