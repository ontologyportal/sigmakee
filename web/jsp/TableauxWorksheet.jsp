<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sigma Knowledge Engineering Environment - Logic Tableaux Practice Worksheet</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            padding: 10px;
            background-color: #f5f5f5;
        }

        .container {
            min-width: 1000px;
            width: max-content;
            margin: 0 auto;
            background: white;
            padding: 10px;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }

        h1 {
            text-align: center;
            color: #333;
            margin-bottom: 30px;
            display: flex;
            justify-content: center;
            align-items: center;
            position: relative;
        }

        .save-button {
            position: absolute;
            right: 0;
            padding: 5px 10px;
            background: #007bff;
            color: white;
            border: none;
            border-radius: 5px;
            cursor: pointer;
            font-size: 16px;
        }

        .save-button:hover {
            background: #0056b3;
        }

        .formula-group {
            margin: 10px 0;
            padding: 7px;
        }

        .formula-input {
            min-width: 140px;
            max-width: 900px;
	    text-align: center;
            padding: 4px;
            border: 2px solid #ddd;
            border-radius: 4px;
            font-size: 16px;
            font-family: monospace;
            resize: none;
            overflow: hidden;
        }

        .formula-input.valid {
            background-color: #d4edda;
            border-color: #28a745;
        }

        .formula-input.invalid {
            background-color: #f8d7da;
            border-color: #dc3545;
        }

        .status-button {
            background: #f8f9fa;
            border: 2px solid #ddd;
            border-radius: 4px;
            width: 25px;
            height: 25px;
            cursor: pointer;
            font-weight: bold;
            font-size: 14px;
            margin-left: 5px;
            margin-right: 1px;
            transition: all 0.2s;
        }

        .status-button:hover {
            background: #e9ecef;
        }

        .status-button.valid {
            background: #d4edda;
            border-color: #28a745;
            color: #155724;
        }

        .status-button.invalid {
            background: #f8d7da;
            border-color: #dc3545;
            color: #721c24;
        }

        .checkbox-container {
            display: inline-block;
            margin-left: 1px;
            vertical-align: middle;
        }

        .checkbox-container input[type="checkbox"] {
            transform: scale(1.2);
            margin-right: 5px;
        }

        .button-container {
            display: flex;
            gap: 10px;
        }

        .rule-button {
            padding: 3px 7px;
            margin: 3px;
            background: #28a745;
            color: white;
            border: none;
            border-radius: 4px;
            cursor: pointer;
        }

        .rule-button:hover {
            background: #218838;
        }

        .delete-button {
	    position: relative;
            top: -15px;
            left: 15px;
            background: #e57373;
            color: white;
            border: none;
            border-radius: 50%;
            width: 20px;
            height: 20px;
            cursor: pointer;
            font-size: 12px;
            z-index: 10;
        }

        .delete-button:hover {
            background: #c82333;
        }

        .alpha-group {
            margin-left: 10px;
            position: relative;
        }

        .beta-group {
            display: flex;
            gap: 20px;
            margin-left: 10px;
            justify-content: flex-start;
            position: relative;
            border: 3px solid;
            padding: 10px;
            border-radius: 4px;
        }

        .beta-branch {
            min-width: 250px;
            padding-left: 5px;
            position: relative;
        }

        .connecting-line {
            position: absolute;
            border-left: 2px solid #333;
            z-index: 1;
        }

        .connecting-line-horizontal {
            position: absolute;
            border-top: 2px solid #333;
            z-index: 1;
        }

        .separator-bar {
            height: 3px;
            margin: 10px auto;
            border-radius: 2px;
            width: 40%;
            position: relative;
        }

        .separator-bar.beta-bar {
            width: 100%;
        }

        .alpha-tick {
            width: 3px;
            height: 10px;
            margin: 5px auto;
        }

        .formula-container {
            margin: 10px 0;
            text-align: center;
        }

        .controls-row {
            display: flex;
            justify-content: center;
            align-items: center;
            gap: 30px;
            margin: 10px 0;
        }

        .root-group {
            border: none;
            padding: 0;
            margin: 0;
        }

        /* Hidden element for measuring text width */
        .text-measurer {
            position: absolute;
            visibility: hidden;
            height: auto;
            width: auto;
            white-space: nowrap;
            font-family: monospace;
            font-size: 16px;
            padding: 4px;
        }

        /* Fixed images in upper left corner */
        .fixed-images {
            position: fixed;
            top: 10px;
            left: 10px;
            z-index: 1000;
            display: flex;
            flex-direction: column;
            gap: 10px;
        }

        .fixed-images img {
            display: block;
            max-width: 200px;
            box-shadow: 0 2px 5px rgba(0,0,0,0.2);
            border-radius: 4px;
        }
    </style>
</head>
<body>
    <!-- Fixed images in upper left corner -->
    <div class="fixed-images">
        <img src="/sigma/pixmaps/alpha_rules.gif" alt="Alpha Rules">
        <img src="/sigma/pixmaps/beta_rules.gif" alt="Beta Rules">
    </div>

    <div class="container" id="worksheet">
        <h1>Logic Tableaux Practice Worksheet
            <button class="save-button" id="saveBtn">Save</button>
        </h1>

        <div id="tableaux-root">
            <!-- Initial formula group will be inserted here -->
        </div>

        <!-- Hidden element for measuring text width -->
        <div class="text-measurer" id="textMeasurer"></div>
    </div>

    <script src="/sigma/javascript/html2canvas/html2canvas.min.js"></script>
    <script>
        let groupCounter = 0;

        function getRandomColor() {
            const letters = '0123456789ABCDEF';
            let color = '#';
            for (let i = 0; i < 6; i++) {
                color += letters[Math.floor(Math.random() * 16)];
            }
            return color;
        }

        function autoResize(input) {
            const measurer = document.getElementById('textMeasurer');
            measurer.textContent = input.value || input.placeholder;

            // Get the measured width plus some padding
            const measuredWidth = measurer.offsetWidth;
            const minWidth = 140; // Minimum width
            const maxWidth = 900; // Maximum width

            // Set the width, but constrain it between min and max
            const newWidth = Math.max(minWidth, Math.min(maxWidth, measuredWidth + 20));
            input.style.width = newWidth + 'px';
        }

        function setupInputListeners(input) {
            // Auto-resize on input
            input.addEventListener('input', function() {
                autoResize(this);
            });

            // Auto-resize on focus (in case placeholder is longer than content)
            input.addEventListener('focus', function() {
                autoResize(this);
            });

            // Auto-resize on blur
            input.addEventListener('blur', function() {
                autoResize(this);
            });

            // Initial resize
            autoResize(input);
        }

        function createFormulaGroup(isRoot = false, showButtons = true) {
            groupCounter++;
            const groupId = `group-${groupCounter}`;

            const groupDiv = document.createElement('div');
            groupDiv.className = isRoot ? 'formula-group root-group' : 'formula-group';
            groupDiv.id = groupId;
            groupDiv.style.display = 'flex';
            groupDiv.style.flexDirection = 'column';
            groupDiv.style.alignItems = 'center';

            groupDiv.innerHTML = `
                <div class="formula-container" style="display: flex; align-items: center; gap: 5px; margin: 10px 0;">
                    ${!isRoot ? '<button class="delete-button" onclick="deleteGroup(\'' + groupId + '\')">&times;</button>' : ''}
                    <input type="text" class="formula-input" placeholder="Enter formula...">
                    <button class="status-button" onclick="cycleStatus('${groupId}')" data-state="unknown">-</button>
                    <div class="checkbox-container">
                        <input type="checkbox" id="check-${groupId}">
                    </div>
                </div>

                <div class="button-container" style="display: ${!showButtons ? 'none' : 'flex'}; gap: 10px; justify-content: flex-start; margin: 2px 0; margin-left: -30px;">
                    <button class="rule-button" onclick="addAlphaRule('${groupId}')">alpha</button>
                    <button class="rule-button" onclick="addBetaRule('${groupId}')">beta</button>
                </div>
            `;

            // Set up auto-resize for the input field
            const input = groupDiv.querySelector('.formula-input');
            setupInputListeners(input);

            return groupDiv;
        }

        function cycleStatus(groupId) {
            const group = document.getElementById(groupId);
            const statusButton = group.querySelector('.status-button');
            const input = group.querySelector('.formula-input');

            const currentState = statusButton.getAttribute('data-state');
            let newState, newLabel;

            switch (currentState) {
                case 'unknown':
                    newState = 'invalid';
                    newLabel = 'x';
                    break;
                case 'invalid':
                    newState = 'valid';
                    newLabel = 'o';
                    break;
                case 'valid':
                    newState = 'unknown';
                    newLabel = '-';
                    break;
                default:
                    newState = 'unknown';
                    newLabel = '-';
            }

            statusButton.setAttribute('data-state', newState);
            statusButton.textContent = newLabel;

            // Update input styling
            input.classList.remove('valid', 'invalid');
            statusButton.classList.remove('valid', 'invalid');

            if (newState === 'valid') {
                input.classList.add('valid');
                statusButton.classList.add('valid');
            } else if (newState === 'invalid') {
                input.classList.add('invalid');
                statusButton.classList.add('invalid');
            }

            // Check if this group is part of an alpha-group
            const parentAlpha = group.parentElement;
            if (parentAlpha && parentAlpha.classList.contains('alpha-group')) {
                // Apply status to both alpha rules in the group
                const alphaGroups = parentAlpha.querySelectorAll(':scope > .formula-group');
                alphaGroups.forEach(alphaGroup => {
                    const alphaStatusButton = alphaGroup.querySelector('.status-button');
                    const alphaInput = alphaGroup.querySelector('.formula-input');

                    if (alphaStatusButton && alphaInput) {
                        alphaStatusButton.setAttribute('data-state', newState);
                        alphaStatusButton.textContent = newLabel;

                        alphaInput.classList.remove('valid', 'invalid');
                        alphaStatusButton.classList.remove('valid', 'invalid');

                        if (newState === 'valid') {
                            alphaInput.classList.add('valid');
                            alphaStatusButton.classList.add('valid');
                        } else if (newState === 'invalid') {
                            alphaInput.classList.add('invalid');
                            alphaStatusButton.classList.add('invalid');
                        }
                    }

                    // Apply to children of each alpha rule
                    applyStatusToChildren(alphaGroup, newState, newLabel);
                });
            } else {
                // If not in an alpha group, just apply to children normally
                applyStatusToChildren(group, newState, newLabel);
            }
        }

        function applyStatusToChildren(parentGroup, state, label) {
            // Find all child formula groups (exclude the parent itself)
            const childGroups = parentGroup.querySelectorAll('.formula-group');

            childGroups.forEach(childGroup => {
                // Skip if this is the parent group itself
                if (childGroup === parentGroup) return;

                const childStatusButton = childGroup.querySelector('.status-button');
                const childInput = childGroup.querySelector('.formula-input');

                if (childStatusButton && childInput) {
                    childStatusButton.setAttribute('data-state', state);
                    childStatusButton.textContent = label;

                    // Update input styling
                    childInput.classList.remove('valid', 'invalid');
                    childStatusButton.classList.remove('valid', 'invalid');

                    if (state === 'valid') {
                        childInput.classList.add('valid');
                        childStatusButton.classList.add('valid');
                    } else if (state === 'invalid') {
                        childInput.classList.add('invalid');
                        childStatusButton.classList.add('invalid');
                    }
                }
            });
        }

        function addAlphaRule(parentId) {
            const parentGroup = document.getElementById(parentId);

            // Hide the parent's buttons
            const parentButtons = parentGroup.querySelector('.button-container');
            if (parentButtons) {
                parentButtons.style.display = 'none';
            }

            const alphaContainer = document.createElement('div');
            alphaContainer.className = 'alpha-group';

            // Add top tick mark with random color
            const topTick = document.createElement('div');
            topTick.className = 'alpha-tick';
            const randomColor = getRandomColor();
            topTick.style.backgroundColor = randomColor;
            alphaContainer.appendChild(topTick);

            // Create two formula groups in series
            // First group has no buttons, second group has buttons
            const group1 = createFormulaGroup(false, false);
            alphaContainer.appendChild(group1);

            // Add middle tick mark with same color
            const middleTick = document.createElement('div');
            middleTick.className = 'alpha-tick';
            middleTick.style.backgroundColor = randomColor;
            alphaContainer.appendChild(middleTick);

            const group2 = createFormulaGroup(false, true);
            alphaContainer.appendChild(group2);

            parentGroup.appendChild(alphaContainer);
        }

        function addBetaRule(parentId) {
            const parentGroup = document.getElementById(parentId);

            // Hide the parent's buttons
            const parentButtons = parentGroup.querySelector('.button-container');
            if (parentButtons) {
                parentButtons.style.display = 'none';
            }

            const betaContainer = document.createElement('div');
            betaContainer.className = 'beta-group';

            // Set random border color for the beta container
            const randomColor = getRandomColor();
            betaContainer.style.borderColor = randomColor;

            // Create two branches in parallel, both with buttons
            const branch1 = document.createElement('div');
            branch1.className = 'beta-branch';
            const branch2 = document.createElement('div');
            branch2.className = 'beta-branch';

            const group1 = createFormulaGroup(false, true);
            const group2 = createFormulaGroup(false, true);

            branch1.appendChild(group1);
            branch2.appendChild(group2);

            betaContainer.appendChild(branch1);
            betaContainer.appendChild(branch2);

            // Add the beta container
            parentGroup.appendChild(betaContainer);
        }

        function deleteGroup(groupId) {
            const group = document.getElementById(groupId);
            if (group) {
                // Find the parent group to check if buttons should be restored
                let parentGroup = group.parentElement;

                // Navigate up to find the actual formula-group parent
                while (parentGroup && !parentGroup.classList.contains('formula-group')) {
                    parentGroup = parentGroup.parentElement;
                }

                // Remove the group or its container
                let containerToRemove = group.parentElement;

                // If it's in a beta-branch, remove the entire beta-group (both branches) and separator
                if (containerToRemove.className === 'beta-branch') {
                    const betaGroup = containerToRemove.parentElement;
                    // Remove the separator bar before the beta group
                    const previousSibling = betaGroup.previousElementSibling;
                    if (previousSibling && previousSibling.classList.contains('separator-bar')) {
                        previousSibling.remove();
                    }
                    betaGroup.remove();
                } else if (containerToRemove.className === 'alpha-group') {
                    containerToRemove.remove();
                } else {
                    group.remove();
                }

                // Check if parent should get buttons back
                if (parentGroup) {
                    const hasChildren = parentGroup.querySelector('.alpha-group, .beta-group');
                    if (!hasChildren) {
                        const parentButtons = parentGroup.querySelector('.button-container');
                        if (parentButtons) {
                            parentButtons.style.display = 'flex';
                        }
                    }
                }
            }
        }

        // Get URL parameter and decode it
        function getUrlParameter(name) {
            const urlParams = new URLSearchParams(window.location.search);
            return urlParams.get(name);
        }

        document.getElementById("saveBtn").addEventListener("click", function() {
            const element = document.body;
            const fixedImages = document.querySelector('.fixed-images');

            // Hide the fixed images before capturing
            fixedImages.style.display = 'none';

            html2canvas(element).then(canvas => {
                // Show the fixed images again
                fixedImages.style.display = 'flex';

                // Convert canvas to a blob and download
                canvas.toBlob(function(blob) {
                    const a = document.createElement("a");
                    a.href = URL.createObjectURL(blob);
                    a.download = "tableaux_worksheet.png";
                    a.click();
                    URL.revokeObjectURL(a.href);
                });
            }).catch(err => {
                // Show the fixed images again even if there's an error
                fixedImages.style.display = 'flex';
                console.error("Error capturing canvas:", err);
                alert("Error saving image.");
            });
        });


	// Initialize the worksheet with the root formula group
        document.addEventListener('DOMContentLoaded', function() {
            const root = document.getElementById('tableaux-root');
            const rootGroup = createFormulaGroup(true, true);
            root.appendChild(rootGroup);

            // Check for 'f' parameter in URL and populate root formula
            const formulaParam = getUrlParameter('f');
            if (formulaParam) {
                const rootInput = rootGroup.querySelector('.formula-input');
                if (rootInput) {
                    rootInput.value = decodeURIComponent(formulaParam);
                    autoResize(rootInput);
                }
            }
        });
    </script>
</body>
</html>