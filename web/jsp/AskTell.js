function toggleVampireOptions() {
    const vamp   = document.querySelector('input[name="inferenceEngine"][value="Vampire"]');
    const casc   = document.getElementById('CASC');
    const avatar = document.getElementById('Avatar');
    const custom = document.getElementById('Custom');
    const mp     = document.getElementById('ModensPonens');
    const drop   = document.getElementById('dropOnePremise');
    const holModal = document.getElementById('HolUseModals');
    // NEW: check if .thf filter is selected
    const thfRadio = document.querySelector('input[name="testFilter"][value="thf"]');
    const isThf = thfRadio && thfRadio.checked;
    // NEW: Translation Mode toggle (FOL vs HOL)
    const holRadio = document.getElementById('modeHOL');
    const isHolMode = holRadio && holRadio.checked;
    // If THF file OR HOL Translation Mode → always disable Modus Ponens + Drop One Premise
    if (isThf || isHolMode) {
        if (mp)   { mp.checked = false;   mp.disabled = true; }
        if (drop) { drop.checked = false; drop.disabled = true; }
        // Still keep Vampire mode radios tied to Vampire on/off
        const vampireOn = vamp && vamp.checked && !vamp.disabled;
        [casc, avatar, custom, holModal].forEach(el => { if (el) el.disabled = !vampireOn; });
        custom.checked = false; custom.disabled = true;
        return; // THF overrides the rest of the logic
    }
    // Original behavior when NOT in THF mode
    const vampireOn = vamp && vamp.checked && !vamp.disabled;
    [casc, avatar, custom, mp].forEach(el => { if (el) el && (el.disabled = !vampireOn); });
    const mpOn = vampireOn && mp && mp.checked;
    if (drop) {
        drop.disabled = !mpOn;
        if (!mpOn) drop.checked = false;
    }
    // Modal is Disabled on Non-HOL option
    holModal.checked = false; holModal.disabled = true;
    // Disable Custom until it gets fixed and tested!
    custom.checked = false; custom.disabled = true;
}

function toggleRunSource() {
    const src  = document.querySelector('input[name="runSource"]:checked')?.value || 'custom';
    const ta   = document.getElementById('stmtArea');
    const test = document.getElementById('testName');
    const lblC = document.getElementById('lblCustom');
    const lblT = document.getElementById('lblTest');
    const step2 = document.getElementById('step2Fieldset');
    const isTest = (src === 'test');
    ta.disabled   = isTest;
    test.disabled = !isTest;
    if (step2) step2.disabled = isTest;
    lblC.style.opacity = isTest ? .5 : 1;
    lblT.style.opacity = isTest ? 1 : .5;
}

function toggleTranslationMode() {
    const fol = document.getElementById('modeFOL');
    const hol = document.getElementById('modeHOL');
    const folBlock = document.getElementById('folOptions');
    const holBlock = document.getElementById('holOptions');
    const leo = document.getElementById('engineLEO');
    const epr = document.getElementById('engineEProver');
    const vamp = document.getElementById('engineVampire');
    const isHol = hol && hol.checked;
    if (folBlock) folBlock.style.display = isHol ? 'none' : 'block';
    if (holBlock) holBlock.style.display = isHol ? 'block' : 'none';
    // In HOL mode, only Vampire is currently supported.
    if (isHol) {
        if (vamp) vamp.checked = true;
        [leo, epr].forEach(el => { if (el) { el.dataset.prevDisabled = String(el.disabled); el.disabled = true; } });
    } else {
        // Restore engine availability (server may still disable missing binaries)
        [leo, epr].forEach(el => {
            if (el) {
                const prev = el.dataset.prevDisabled;
                if (prev !== undefined) el.disabled = (prev === "true");
            }
        });
    }
    enforceCwaAvailability();
    // Re-apply dependent enabling/disabling
    toggleVampireOptions();
}

function enforceCwaAvailability() {
    const hol = document.getElementById('modeHOL');
    const tff = document.getElementById('langTff');
    const cwa = document.getElementById('CWA');
    if (!cwa) return;
    const isHol = hol && hol.checked;
    const isTff = tff && tff.checked;
    const allowCwa = (!isHol) && isTff;
    cwa.disabled = !allowCwa;
    if (!allowCwa) cwa.checked = false;
}

function toggleTranslationModeForRunSource() {
    const src = document.querySelector('input[name="runSource"]:checked')?.value || 'custom';
    const isTest = (src === 'test');
    // Option 1 (recommended): disable everything inside a Step-2 container
    const block = document.getElementById('translationModeBlock');
    if (block) {
        const controls = block.querySelectorAll('input, select, textarea, button');
        controls.forEach(el => {
            // If you have any "always enabled" control, exclude it here with a condition.
            el.disabled = isTest;
            if (isTest && (el.type === 'checkbox' || el.type === 'radio')) el.checked = false;
        });
        block.style.opacity = isTest ? "0.6" : "1";
        return;
    }
}

function filterTestsByExtension(ext) {
    const select = document.getElementById("testFile");
    let hasVisibleSelected = false;
    for (let opt of select.options) {
        if (ext === "all") {
            opt.style.display = "";
            hasVisibleSelected = hasVisibleSelected || opt.selected;
        } else {
            const show = opt.value.endsWith("." + ext);
            opt.style.display = show ? "" : "none";
            if (show && opt.selected) hasVisibleSelected = true;
        }
    }
    // reset selection if current one is hidden
    if (!hasVisibleSelected) {
        select.selectedIndex = 0;
    }
}

document.querySelectorAll("input[name='testFilter']").forEach(radio => {
    radio.addEventListener("change", () => {
        filterTestsByExtension(radio.value);
    });
});

window.onload = function () {
    toggleVampireOptions();
    toggleRunSource();
    toggleTranslationMode();
    document.querySelectorAll('input[name="inferenceEngine"], #ModensPonens')
        .forEach(el => el.addEventListener('change', toggleVampireOptions));
    // NEW: when .thf / other filters change, re-apply logic
    document.querySelectorAll('input[name="testFilter"]')
        .forEach(el => el.addEventListener('change', toggleVampireOptions));
    document.querySelectorAll('input[name="runSource"]')
        .forEach(el => el.addEventListener('change', toggleRunSource));
    document.querySelectorAll('input[name="translationMode"]')
        .forEach(el => el.addEventListener('change', toggleTranslationMode));
    document.querySelectorAll('input[name="TPTPlang"]')
        .forEach(el => el.addEventListener('change', enforceCwaAvailability));
};

function viewSelectedTest() {
    const sel = document.getElementById('testName');
    if (!sel || !sel.value) return;
    const name = sel.value.toLowerCase();
    // .tq via ViewTest.jsp, others directly from /tests/
    const url = name.endsWith('.tq') || name.endsWith('.tptp') || name.endsWith('.tff') || name.endsWith('.thf')
        ? ('ViewTest.jsp?name=' + encodeURIComponent(sel.value))
        : ('tests/' + encodeURIComponent(sel.value));
    window.open(url, '_blank');
}

window.addEventListener('load', function(){
    document.body.classList.remove('busy');
});

document.addEventListener('DOMContentLoaded', function(){
    const sel = document.getElementById('testName');
    const radios = document.querySelectorAll('input[name="testFilter"]');
    const hidden = document.getElementById('testFilterHidden');
    const form = document.getElementById('AskTell');
    if (!sel || !radios.length) return;

    const original = Array.from(sel.options).map(o => ({text:o.text, value:o.value}));

    function applyFilter(kind){
        const prev = sel.value;
        const match = f => kind === 'all' || f.toLowerCase().endsWith('.' + kind);
        sel.innerHTML = '';
        original.filter(o => match(o.value)).forEach(o => {
            const opt = document.createElement('option'); opt.value=o.value; opt.text=o.text; sel.add(opt);
        });
        sel.value = Array.from(sel.options).some(o => o.value===prev) ? prev : (sel.options[0]?.value || '');
    }
    function currentFilter(){ return document.querySelector('input[name="testFilter"]:checked')?.value || 'all'; }

    // initialize from session-backed radio
    applyFilter(currentFilter());

    // keep session in sync on change + submit
    radios.forEach(r => r.addEventListener('change', e => { hidden.value = e.target.value; applyFilter(e.target.value); }));
    form.addEventListener('submit', () => { hidden.value = currentFilter(); });
});

(function(){
    const form = document.getElementById('AskTell');
    const overlay = document.getElementById('loading');
    const frame = document.getElementById('runFrame');

    const bar   = document.getElementById('spinBar');
    const pct   = document.getElementById('spinPct');
    const eta   = document.getElementById('spinEta');
    const limit = document.getElementById('spinLimit');
    const spinTitle = document.getElementById('spinTitle');

    let clicked = null;
    let rafId = null;
    let runInFlight = false;

    function clampInt(n, def) {
        const x = parseInt(n, 10);
        return Number.isFinite(x) && x > 0 ? x : def;
    }

    function stopProgress() {
        if (rafId) cancelAnimationFrame(rafId);
        rafId = null;
    }

    function startProgress(durationSec) {
        stopProgress();
        if (limit) limit.textContent = String(durationSec);
        if (bar) bar.style.width = '0%';
        if (pct) pct.textContent = '0%';
        if (eta) eta.textContent = `~${durationSec}s remaining`;

        const durationMs = durationSec * 1000;
        const start = performance.now();

        function tick(now) {
            const elapsed = now - start;
            const p = Math.min(1, elapsed / durationMs);

            if (bar) bar.style.width = (p * 100).toFixed(1) + '%';
            if (pct) pct.textContent = Math.floor(p * 100) + '%';

            const remaining = Math.max(0, Math.ceil((durationMs - elapsed) / 1000));
            if (eta) eta.textContent = remaining > 0 ? `~${remaining}s remaining` : 'Timeout reached';

            if (p < 1 && runInFlight) rafId = requestAnimationFrame(tick);
        }

        rafId = requestAnimationFrame(tick);
    }

    // Track which submit button was pressed
    form.querySelectorAll('input[type=submit]').forEach(b =>
        b.addEventListener('click', e => clicked = e.target.value)
    );

    // Run and Tell submit into iframe so the page stays alive
    form.addEventListener('submit', function(){
        if (clicked !== 'Run' && clicked !== 'Tell') {
            form.removeAttribute('target');
            return;
        }

        form.setAttribute('target', 'runFrame');

        // Tell is typically fast, use 10s; Run uses user-specified timeout
        const timeoutField = form.querySelector('input[name="timeout"]');
        const tSec = (clicked === 'Tell') ? 10 : clampInt(timeoutField ? timeoutField.value : null, 30);

        // Update spinner title based on action
        if (spinTitle) {
            spinTitle.textContent = (clicked === 'Tell') ? 'Adding assertion...' : 'Running inference...';
        }

        runInFlight = true;
        overlay.style.display = 'block';
        startProgress(tSec);
    });

    // IMPORTANT: hide overlay when iframe finishes loading server response
    frame.addEventListener('load', function(){
        if (!runInFlight) return;
        runInFlight = false;

        stopProgress();
        overlay.style.display = 'none';

        // Pull results from iframe and inject into main page
        try {
            const doc = frame.contentDocument || frame.contentWindow.document;
            const results = doc.getElementById('serverResults');
            const host = document.getElementById('resultsHost');

            if (!host) return;

            if (results) {
                host.innerHTML = results.innerHTML;
            } else {
                // Fallback: show something instead of "nothing"
                host.innerHTML = "<div style='color:#b00'>No #serverResults found in response.</div>";
            }
        } catch (e) {
            document.getElementById('resultsHost').innerHTML =
                "<div style='color:#b00'>Could not read iframe response (same-origin issue).</div>";
        }
    });

    // Safety: if user navigates back/forward
    window.addEventListener('pageshow', e => {
        if (e.persisted) {
            runInFlight = false;
            stopProgress();
            overlay.style.display = 'none';
        }
    });
})();