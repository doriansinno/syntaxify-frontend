// helper functions for line numbers, etc. (original v04 code)
// API Base URL - ändere dies für verschiedene Umgebungen
const API_BASE_URL = import.meta.env?.VITE_API_BASE_URL || 'https://syntaxify-backend.onrender.com';

const textarea = document.getElementById("Eingabe");
const linesDiv = document.getElementById("lines");

if (textarea && linesDiv) {
    const mirror = document.createElement('div');
    mirror.style.position = 'absolute';
    mirror.style.top = '-9999px';
    mirror.style.left = '-9999px';
    mirror.style.visibility = 'hidden';
    mirror.style.whiteSpace = 'pre-wrap';
    mirror.style.wordWrap = 'break-word';
    mirror.style.boxSizing = 'border-box';
    document.body.appendChild(mirror);

    function getLineHeight(el) {
        const cs = window.getComputedStyle(el);
        let lh = parseFloat(cs.lineHeight);
        if (isNaN(lh)) {
            const fs = parseFloat(cs.fontSize) || 14;
            lh = fs * 1.2;
        }
        return lh;
    }

    function updateLines() {
        const taStyle = window.getComputedStyle(textarea);
        mirror.style.width = textarea.clientWidth + 'px';
        mirror.style.whiteSpace = taStyle.whiteSpace || 'pre-wrap';
        mirror.style.font = taStyle.font;
        mirror.style.fontSize = taStyle.fontSize;
        mirror.style.fontFamily = taStyle.fontFamily;
        mirror.style.lineHeight = taStyle.lineHeight;
        mirror.style.padding = taStyle.padding;
        mirror.style.border = taStyle.border;
        mirror.style.letterSpacing = taStyle.letterSpacing;

        mirror.textContent = textarea.value || ' ';
        const lineHeight = getLineHeight(textarea);
        const mirrorHeight = mirror.clientHeight;

        const visualLines = Math.max(1, Math.round(mirrorHeight / lineHeight));
        let lines = '';
        for (let i = 1; i <= visualLines; i++) {
            lines += i + '<br>';
        }
        linesDiv.innerHTML = lines;
        linesDiv.style.height = textarea.clientHeight + 'px';
    }

    function syncScroll() {
        const taScroll = textarea.scrollTop;
        const taScrollable = textarea.scrollHeight - textarea.clientHeight;
        const lnScrollable = linesDiv.scrollHeight - linesDiv.clientHeight;

        linesDiv.scrollTop = taScrollable > 0 && lnScrollable > 0
            ? taScroll * (lnScrollable / taScrollable)
            : taScroll;
    }

    textarea.addEventListener("input", () => {
        updateLines();
        syncScroll();
    });
    textarea.addEventListener("scroll", syncScroll);
    window.addEventListener("resize", syncScroll);

    // Initial
    updateLines();
    syncScroll();
}

const outputText = document.getElementById('outputText');
const outputCount = document.getElementById('outputCount');

if (outputText && outputCount) {
    const mirrorOutput = document.createElement('div');
    mirrorOutput.style.position = 'absolute';
    mirrorOutput.style.top = '-9999px';
    mirrorOutput.style.left = '-9999px';
    mirrorOutput.style.visibility = 'hidden';
    mirrorOutput.style.whiteSpace = 'pre-wrap';
    mirrorOutput.style.wordWrap = 'break-word';
    mirrorOutput.style.boxSizing = 'border-box';
    document.body.appendChild(mirrorOutput);

    function getLineHeight(el) {
        const cs = window.getComputedStyle(el);
        let lh = parseFloat(cs.lineHeight);
        if (isNaN(lh)) {
            const fs = parseFloat(cs.fontSize) || 14;
            lh = fs * 1.2;
        }
        return lh;
    }

    function updateOutputLines() {
        const taStyle = window.getComputedStyle(outputText);
        mirrorOutput.style.width = outputText.clientWidth + 'px';
        mirrorOutput.style.whiteSpace = taStyle.whiteSpace || 'pre-wrap';
        mirrorOutput.style.font = taStyle.font;
        mirrorOutput.style.fontSize = taStyle.fontSize;
        mirrorOutput.style.fontFamily = taStyle.fontFamily;
        mirrorOutput.style.lineHeight = taStyle.lineHeight;
        mirrorOutput.style.padding = taStyle.padding;
        mirrorOutput.style.border = taStyle.border;

        mirrorOutput.textContent = outputText.value;
        const lineHeight = getLineHeight(outputText);
        const visualLines = Math.max(1, Math.round(mirrorOutput.clientHeight / lineHeight));

        let lines = '';
        for (let i = 1; i <= visualLines; i++) {
            lines += i + '<br>';
        }
        outputCount.innerHTML = lines;
        outputCount.style.height = outputText.clientHeight + 'px';
    }

    function syncScroll() {
        outputCount.scrollTop = outputText.scrollTop;
    }

    outputText.addEventListener('input', () => { updateOutputLines(); syncScroll(); });
    outputText.addEventListener('scroll', syncScroll);
    window.addEventListener('resize', () => { updateOutputLines(); syncScroll(); });

    updateOutputLines();
    syncScroll();
}

// dropdown logic and chosen language
let selectedLanguage = 'python';
const dropdown = document.getElementById("languageDropdown");
const selected = dropdown?.querySelector(".dropdown-selected");
const items = dropdown?.querySelectorAll(".dropdown-item") || [];

selected?.addEventListener("click", () => {
    dropdown.classList.toggle("active");
});

items.forEach(item => {
    item.addEventListener("click", () => {
        selected.textContent = item.textContent;
        selectedLanguage = item.getAttribute('data-value') || item.textContent.toLowerCase();
        dropdown.classList.remove("active");
    });
});

document.addEventListener("click", e => {
    if (!dropdown.contains(e.target)) {
        dropdown.classList.remove("active");
    }
});

// toggle explanation panel
const toggleBtn = document.getElementById('toggleOutputExplanation');
const panel = document.getElementById('outputExplanationPanel');
const navbar = document.querySelector('nav.navbar');

function updatePanelPosition() {
    const navbarHeight = navbar.offsetHeight;
    panel.style.top = navbarHeight + 'px';
    panel.style.height = `calc(100vh - ${navbarHeight}px)`;
}

window.addEventListener('load', () => {
    updatePanelPosition();
    panel.classList.remove('active');
    toggleBtn.style.right = '40px';
    toggleBtn.textContent = '>';
});

window.addEventListener('resize', updatePanelPosition);

toggleBtn.addEventListener('click', () => {
    panel.classList.toggle('active');

    if(panel.classList.contains('active')){
        toggleBtn.style.right = '700px';
        toggleBtn.textContent = '<';
    } else {
        toggleBtn.style.right = '40px';
        toggleBtn.textContent = '>';
    }
});

// translation function adapted to backend API
async function translate() {
    const input = document.getElementById("Eingabe").value;
    const target = selectedLanguage;

    try {
        const response = await fetch(`${API_BASE_URL}/api/translate`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                input: input,
                sourceLanguage: "de",
                targetLanguage: target
            })
        });
        const data = await response.json();
        outputText.value = data.translatedCode || "Keine Antwort vom Server";
        if (typeof updateOutputLines === 'function') updateOutputLines();
    } catch (err) {
        outputText.value = "Fehler bei der Übersetzung";
        if (typeof updateOutputLines === 'function') updateOutputLines();
    }
}