# sigma-config.sh - KB Configuration Mode Switcher

## The Problem

When developing SUMO ontology extensions, you face a trade-off:

| Mode | Pros | Cons |
|------|------|------|
| **Full KB** | All terms available in jEdit/VSCode autocomplete, complete type checking | Vampire/E-Prover queries timeout or take minutes |
| **Minimal KB** | Fast inference (seconds) | Missing terms, limited autocomplete, incomplete type checking |

Manually editing `config.xml` to switch between these modes is tedious and error-prone.

## The Solution

`sigma-config.sh` lets you instantly switch between full and fast configurations:

```bash
./sigma-config.sh full    # For coding/browsing (all terms)
./sigma-config.sh fast    # For inference testing (quick queries)
./sigma-config.sh status  # Check current mode
```

## Installation

The script is included in the SigmaKEE repository. For convenience, add it to your PATH:

```bash
# Add to ~/.bashrc
export PATH="$ONTOLOGYPORTAL_GIT/sigmakee:$PATH"

# Or create a symlink
ln -s ~/workspace/sigmakee/sigma-config.sh ~/bin/sigma-config
```

## Example Workflows

### Workflow 1: Developing New Ontology Terms

This workflow is for writing new .kif files with full term coverage.

```bash
# 1. Start in FULL mode for coding
sigma-config.sh full

# 2. Launch jEdit with full term support
jedit ~/.sigmakee/KBs/my-ontology.kif

# 3. Write your axioms with full autocomplete and type checking
#    - All SUMO terms available
#    - Ctrl+Space for completion
#    - Plugins → SUMOjEdit → Check for SUO-KIF errors

# 4. When ready to test inference, switch to FAST mode
sigma-config.sh fast

# 5. Restart jEdit to reload minimal KB
#    (or use Plugins → SUMOjEdit → Reload KB)

# 6. Test your specific axiom
#    - Highlight the axiom
#    - Plugins → SUMOjEdit → Query on highlighted expression
#    - Results appear quickly (seconds, not minutes)

# 7. Switch back to FULL mode to continue coding
sigma-config.sh full
```

### Workflow 2: Quick Inference Testing Loop

This workflow is for rapidly iterating on axiom correctness.

```bash
# 1. Start in FAST mode
sigma-config.sh fast

# 2. Edit your .kif file (any editor works)
code ~/.sigmakee/KBs/my-ontology.kif

# 3. Test via command line (no jEdit needed)
cd ~/workspace/sigmakee
java -Xmx10g -Xss1m -cp "build/sigmakee.jar:lib/*" \
    com.articulate.sigma.KB v --ask "(your-query-here)" --timeout 30

# 4. Iterate: edit → save → run query → repeat
```

### Workflow 3: Contributing to SUMO

This workflow is for submitting ontology changes upstream.

```bash
# 1. Fork and clone the sumo repository
cd ~/workspace
git clone https://github.com/YOUR_USERNAME/sumo.git sumo-fork
cd sumo-fork
git remote add upstream https://github.com/ontologyportal/sumo.git

# 2. Create a feature branch
git checkout -b feature/add-cybersecurity-terms

# 3. Switch to FULL mode for development
sigma-config.sh full
jedit your-new-ontology.kif

# 4. Develop with full term support...

# 5. Before committing, validate your axioms:

#    a. Syntax check (FULL mode)
#       - Load in Sigma browser or jEdit
#       - Check for parse errors

#    b. Inference check (FAST mode)
sigma-config.sh fast
#       - Add your .kif to config-fast.xml temporarily:
#         <constituent filename="your-new-ontology.kif" />
#       - Run targeted queries to verify axioms

# 6. Run the test suite
cd ~/workspace/sigmakee
ant test

# 7. Commit and push
git add your-new-ontology.kif
git commit -m "Add cybersecurity domain terms"
git push origin feature/add-cybersecurity-terms

# 8. Open a Pull Request on GitHub
```

### Workflow 4: Customizing Fast Mode

By default, fast mode only loads `Merge.kif`. You can customize it:

```bash
# 1. Initialize config files
sigma-config.sh init

# 2. Edit the fast config to include your files
nano ~/.sigmakee/KBs/config-fast.xml

# Add your ontology file:
#   <kb name="SUMO" >
#     <constituent filename="Merge.kif" />
#     <constituent filename="my-ontology.kif" />
#   </kb>

# 3. Now fast mode includes your terms
sigma-config.sh fast
```

## Commands Reference

| Command | Description |
|---------|-------------|
| `sigma-config.sh full` | Switch to full KB (all ontology files) |
| `sigma-config.sh fast` | Switch to minimal KB (Merge.kif only) |
| `sigma-config.sh status` | Show current mode and config file locations |
| `sigma-config.sh init` | Create config-full.xml and config-fast.xml |
| `sigma-config.sh --help` | Show help message |

## Config Files

After running `init`, you'll have:

| File | Purpose |
|------|---------|
| `~/.sigmakee/KBs/config.xml` | Active configuration (what Sigma/jEdit loads) |
| `~/.sigmakee/KBs/config-full.xml` | Full KB template |
| `~/.sigmakee/KBs/config-fast.xml` | Minimal KB template |

## Tips

1. **Always restart jEdit/Sigma after switching modes** — the KB is loaded at startup

2. **Customize config-fast.xml** — add your working ontology files so they're available in fast mode

3. **Use fast mode for CI/testing** — queries complete in seconds instead of timing out

4. **Check mode before long operations** — run `sigma-config.sh status` to avoid surprises

## Troubleshooting

**"Config file not found"**
- Run `ant install` first to set up SigmaKEE
- Or run `sigma-config.sh init` after creating a config.xml manually

**"Terms missing in jEdit"**
- You're probably in fast mode: `sigma-config.sh status`
- Switch to full: `sigma-config.sh full`
- Restart jEdit

**"Vampire queries timeout"**
- You're probably in full mode with too many axioms
- Switch to fast: `sigma-config.sh fast`
- Or increase timeout: `--timeout 120`
