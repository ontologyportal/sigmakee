/** This code is copyright Articulate Software (c) 2024.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment,
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also sigmakee.sourceforge.net
*/

package com.articulate.sigma.tp;

/**
 * Exception thrown when an ATP executable (Vampire, EProver, LEO-III, etc.)
 * is not found at the configured path or is not executable.
 */
public class ExecutableNotFoundException extends ATPException {

    private final String executablePath;    // Path that was tried
    private final String configKey;         // Config key (e.g., "vampire", "eprover", "leoExecutable")

    /**
     * Create a new ExecutableNotFoundException
     *
     * @param engineName The name of the prover (e.g., "Vampire", "EProver", "LEO-III")
     * @param executablePath The path that was attempted
     * @param configKey The configuration key used to find this path
     */
    public ExecutableNotFoundException(String engineName, String executablePath, String configKey) {
        super(buildMessage(engineName, executablePath, configKey), engineName);
        this.executablePath = executablePath;
        this.configKey = configKey;
    }

    /**
     * Create a new ExecutableNotFoundException with a cause
     */
    public ExecutableNotFoundException(String engineName, String executablePath, String configKey, Throwable cause) {
        super(buildMessage(engineName, executablePath, configKey), engineName, cause);
        this.executablePath = executablePath;
        this.configKey = configKey;
    }

    private static String buildMessage(String engineName, String executablePath, String configKey) {
        StringBuilder sb = new StringBuilder();
        sb.append(engineName != null ? engineName : "Prover");
        sb.append(" executable not found");

        if (executablePath != null && !executablePath.isEmpty()) {
            sb.append(" at: ").append(executablePath);
        }

        if (configKey != null && !configKey.isEmpty()) {
            sb.append(" (config key: ").append(configKey).append(")");
        }

        return sb.toString();
    }

    /**
     * @return The path that was attempted
     */
    public String getExecutablePath() {
        return executablePath;
    }

    /**
     * @return The configuration key used to find this executable
     */
    public String getConfigKey() {
        return configKey;
    }

    @Override
    public String getSuggestion() {
        StringBuilder sb = new StringBuilder();
        sb.append("Please check that:\n");
        sb.append("1. The '").append(configKey != null ? configKey : "prover")
                .append("' setting in config.xml points to a valid executable\n");
        sb.append("2. The file exists at the specified path");

        if (executablePath != null) {
            sb.append(": ").append(executablePath);
        }

        sb.append("\n3. The file has execute permissions (chmod +x on Unix/Linux)\n");
        sb.append("4. If the prover is not installed, download and install it first");

        return sb.toString();
    }

    @Override
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessage());
        sb.append("\n\n").append(getSuggestion());
        return sb.toString();
    }
}
