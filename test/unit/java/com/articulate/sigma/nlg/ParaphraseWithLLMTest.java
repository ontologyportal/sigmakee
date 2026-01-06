package com.articulate.sigma.nlg;

import com.articulate.sigma.KBmanager;
import com.articulate.sigma.utils.StringUtil;
import org.junit.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class ParaphraseWithLLMTest {

    private static String ollamaHost = "http://127.0.0.1:11434";

    public static boolean checkOllamaHealth(){
        OllamaClient oc = new OllamaClient(ollamaHost, 1000, 1500); // short timeouts
        return oc.isHealthy();
    }

    private String unescapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\r", "\r")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private String jsonExtractField(String json, String field) {
        if (json == null) return "";
        String key = "\"" + field + "\"";
        int k = json.indexOf(key);
        if (k < 0) return "";
        int colon = json.indexOf(':', k);
        if (colon < 0) return "";

        int q1 = json.indexOf('"', colon + 1);
        if (q1 < 0) return "";

        boolean esc = false;
        for (int i = q1 + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (esc) { esc = false; continue; }
            if (c == '\\') { esc = true; continue; }
            if (c == '"') return unescapeJson(json.substring(q1 + 1, i));
        }
        return "";
    }

    private String callOllamaJson(String prompt, String model) {

        OllamaClient ollama = new OllamaClient(ollamaHost);

        Map<String,Object> opts = new HashMap<>();
        opts.put("temperature", 0);
        opts.put("top_p", 1);
        opts.put("num_predict", 1500);

        boolean jsonMode = true;

        try {
            if (model != null && model.startsWith("gpt-oss")) {
                return ollama.chat(model, prompt, opts, jsonMode);
            }
            opts.put("seed", 0);
            return ollama.generate(model, prompt, opts, jsonMode);
        } catch (IOException e) {
            System.out.println("ERROR | callOllamaJson: " + e);
            return "{}";
        }
    }

    @Test
    public void strictPromptUseSUOKIF() throws Exception {

        final String suoKif =
                "(forall (?X0 ?X1) (or (mother ?X0 ?X1) (not (parent ?X0 ?X1)) (not (instance ?X0 Organism)) (not (instance ?X1 Organism)) (not (instance ?X1 Woman))))";

        final String glossary =
                "mother = mother\n"
                        + "parent = parent\n"
                        + "instance = instance\n"
                        + "Organism = organism\n"
                        + "Woman = woman";

        String prompt1 =
                "You are a SUO-KIF to natural-language translator.\n"
                        + "Your task is to produce a literal, structure-preserving English rendering of a SUO-KIF formula.\n\n"

                        + "STRICT RULES:\n"
                        + "- Preserve the logical structure exactly (quantifiers, implication, conjunction, negation).\n"
                        + "- Do NOT invent facts or relationships.\n"
                        + "- Do NOT simplify or interpret semantics beyond the formula structure.\n"
                        + "- Keep all SUMO terms and variables unchanged.\n"
                        + "- Use plain English words only (no logical symbols).\n"
                        + "- Focus on structural faithfulness, not natural plausibility.\n"
                        + "- After verbalizing each predicate, append the exact predicate in parentheses.\n\n"

                        + "OUTPUT FORMAT:\n"
                        + "- Output VALID JSON ONLY.\n"
                        + "- Schema: {\"paraphrase\":\"...\"}\n\n"

                        + "EXAMPLE 1:\n"
                        + "SUO-KIF:\n"
                        + "(orientation A C Right)\n"
                        + "OUTPUT:\n"
                        + "{\"paraphrase\":\"A is at Right of C (orientation A C Right)\"}\n\n"

                        + "EXAMPLE 1:\n"
                        + "SUO-KIF:\n"
                        + "(=>\n"
                        + "  (and\n"
                        + "    (instance ?CHILD Human)\n"
                        + "    (holdsDuring ?TIME (attribute ?CHILD NonFullyFormed)))\n"
                        + "  (holdsDuring ?TIME (instance ?CHILD HumanYouth)))\n\n"
                        + "OUTPUT:\n"
                        + "{\"paraphrase\":\"If ?CHILD is an instance of Human (instance ?CHILD Human) "
                        + "and during ?TIME the child has the attribute NonFullyFormed "
                        + "(holdsDuring ?TIME (attribute ?CHILD NonFullyFormed)), "
                        + "then during the same time ?CHILD is an instance of HumanYouth "
                        + "(holdsDuring ?TIME (instance ?CHILD HumanYouth)).\"}\n\n"

                        + "EXAMPLE 2:\n"
                        + "SUO-KIF:\n"
                        + "(=>\n"
                        + "  (instance ?AT AnimalTeam)\n"
                        + "  (exists (?P)\n"
                        + "    (and\n"
                        + "      (instance ?P Pulling)\n"
                        + "      (agent ?P ?AT))))\n\n"
                        + "OUTPUT:\n"
                        + "{\"paraphrase\":\"If ?AT is an instance of AnimalTeam (instance ?AT AnimalTeam), "
                        + "then there exists ?P such that ?P is an instance of Pulling "
                        + "(instance ?P Pulling) and ?P has agent ?AT (agent ?P ?AT).\"}\n\n"

                        + "GLOSSARY (Sigma):\n" + glossary + "\n\n"
                        + "SUO-KIF:\n" + suoKif;

        // QWEN 2.5 14b
        String model_1 = "qwen2.5:14b-instruct";
        long start_1 = System.nanoTime();
        String result_1 = callOllamaJson(prompt1,model_1);
        long end_1 = System.nanoTime();
        double seconds_1 = (end_1 - start_1) / 1_000_000_000.0;
        double local_machine_time_1 = 23.878; // Angelo's MAC
        String paraphrase = jsonExtractField(result_1, "paraphrase");
        System.out.println("Model: " + model_1);
        System.out.println("Paraphrase: " + paraphrase);
        System.out.println("Time took this run: " + seconds_1 +" | Angelo's MAC: " + local_machine_time_1 + "\n\n");

        // LLAMA3.2:3b
        String model_2 = "llama3.2";
        long start_2 = System.nanoTime();
        String result_2 = callOllamaJson(prompt1,model_2);
        long end_2 = System.nanoTime();
        double seconds_2 = (end_2 - start_2) / 1_000_000_000.0;
        double local_machine_time_2 = 18.62; // Angelo's MAC
        paraphrase = jsonExtractField(result_2, "paraphrase");
        System.out.println("Model: " + model_2);
        System.out.println("Paraphrase: " + paraphrase);
        System.out.println("Time took this run: " + seconds_2 +" | Angelo's MAC: " + local_machine_time_2 + "\n\n");

        // QWEN 2.5 32b
        String model_3 = "qwen2.5:32b-instruct";
        long start_3 = System.nanoTime();
        String result_3 = callOllamaJson(prompt1,model_3);
        long end_3 = System.nanoTime();
        double seconds_3 = (end_3 - start_3) / 1_000_000_000.0;
        double local_machine_time_3 = 35.1; // Angelo's MAC
        paraphrase = jsonExtractField(result_3, "paraphrase");
        System.out.println("Model: " + model_3);
        System.out.println("Paraphrase: " + paraphrase);
        System.out.println("Time took this run: " + seconds_3 +" | Angelo's MAC: " + local_machine_time_3 + "\n\n");

        // GPT-OSS
        String model_4 = "gpt-oss:20b";
        long start_4 = System.nanoTime();
        String result_4 = callOllamaJson(prompt1,model_4);
        long end_4 = System.nanoTime();
        double seconds_4 = (end_4 - start_4) / 1_000_000_000.0;
        double local_machine_time_4 = 19; // Angelo's MAC
        paraphrase = jsonExtractField(result_4, "paraphrase");
        System.out.println("Model: " + model_4);
        System.out.println("Paraphrase: " + paraphrase);
        System.out.println("Time took this run: " + seconds_4 +" | Angelo's MAC: " + local_machine_time_4 + "\n\n");


//         Assertions
        Assert.assertTrue(
                "Ollama call too slow: " + seconds_1 + " seconds",
                seconds_1 < local_machine_time_1
        );

        Assert.assertTrue(
                "Ollama call too slow: " + seconds_2 + " seconds",
                seconds_2 < local_machine_time_2
        );

        Assert.assertTrue(
                "Ollama call too slow: " + seconds_3 + " seconds",
                seconds_3 < local_machine_time_3
        );

        Assert.assertTrue(
                "Ollama call too slow: " + seconds_4 + " seconds",
                seconds_4 < local_machine_time_4
        );
    }

    @Test
    public void relaxedPromptUseSUOKIFNwithDocumentation() throws Exception {

        final String suoKif =
                "(forall (?X0 ?X1) (or (mother ?X0 ?X1) (not (parent ?X0 ?X1)) (not (instance ?X0 Organism)) (not (instance ?X1 Organism)) (not (instance ?X1 Woman))))";

        final String glossary =
                "mother = mother\n"
                        + "parent = parent\n"
                        + "instance = instance\n"
                        + "Organism = organism\n"
                        + "Woman = woman";

        String documentationBlock =
                "forall = \"Universal quantification. (forall (?X ...) A) means A holds for all values of the variables.\"\n"
                        + "or = \"Disjunction. (or A B ...) means at least one listed subformula is true.\"\n"
                        + "mother = \"The general relationship of motherhood. (mother ?CHILD ?MOTHER) means that ?MOTHER is the biological mother of ?CHILD.\"\n"
                        + "not = \"Negation. (not A) means A is false.\"\n"
                        + "parent = \"The general relationship of parenthood. (parent ?CHILD ?PARENT) means that ?PARENT is a biological parent of ?CHILD.\"\n"
                        + "instance = \"An object is an instance of a Class if it is included in that Class. An individual may be an instance of many classes, some of which may be subclasses of others. Thus, there is no assumption in the meaning of instance about specificity or uniqueness.\"\n"
                        + "Organism = \"Generally, a living individual, including all Plants and Animals.\"\n"
                        + "Woman = \"The class of Female Humans.\"";

        String prompt1 =
                "You are a SUO-KIF to natural-language explainer.\n"
                        + "Your task is to explain the meaning of a SUO-KIF formula in clear, simple English,\n"
                        + "so that a human reader can understand what the formula states and how it is used\n"
                        + "as a step in a logical proof.\n\n"

                        + "The explanation must remain faithful to the logical structure of the formula,\n"
                        + "because it will be read as part of a proof trace.\n\n"

                        + "GUIDELINES:\n"
                        + "- Explain the formula in natural English, focusing on its overall meaning.\n"
                        + "- Explicitly mention quantifiers (for all, there exists) and negations.\n"
                        + "- Do NOT hide or remove negation, existence, or universal statements.\n"
                        + "- You may rephrase implications and conjunctions in fluent English\n"
                        + "  (e.g., \"if … then …\", \"and\", \"either … or …\").\n"
                        + "- Do NOT invent facts, assumptions, or relationships.\n"
                        + "- Do NOT strengthen or weaken the statement.\n"
                        + "- Keep SUMO terms and variable names unchanged (do not rename or swap variables).\n"
                        + "- Use the provided glossary and documentation to interpret each predicate correctly.\n"
                        + "- Prefer clarity and readability over strict syntactic mirroring.\n\n"

                        + "STRUCTURE REQUIREMENTS:\n"
                        + "- Every implication (=>) must be reflected explicitly as an \"if ... then ...\" clause.\n"
                        + "- Nested implications must be rendered as nested \"if ... then ...\" clauses.\n"
                        + "- Do not merge multiple implications into a single conditional.\n"
                        + "- Render (and A B ...) strictly as \"A and B ...\".\n"
                        + "- Render (or A B ...) strictly as \"either A or B ...\".\n"
                        + "- Do NOT introduce \"either/or\" unless the original formula contains (or ...).\n"
                        + "- Preserve predicate argument bindings exactly: if a predicate applies to ?X1 in SUO-KIF,\n"
                        + "  it must apply to ?X1 in the explanation.\n"
                        + "- Do not move properties, attributes, or roles from one variable to another.\n"
                        + "- Always state the final consequent of each implication explicitly (do not stop at conditions).\n\n"

                        + "IMPORTANT:\n"
                        + "- This is an explanation, not a rewrite of the logic.\n"
                        + "- The reader should be able to follow how this statement contributes to the proof.\n\n"

                        + "OUTPUT FORMAT:\n"
                        + "- Output VALID JSON ONLY.\n"
                        + "- Schema: {\"paraphrase\":\"...\"}\n\n"

                        + "GLOSSARY (Sigma):\n"
                        + glossary + "\n\n"

                        + "DOCUMENTATION (SUMO):\n"
                        + documentationBlock + "\n\n"

                        + "SUO-KIF:\n"
                        + suoKif;

        // QWEN 2.5 14b
        String model_1 = "qwen2.5:14b-instruct";
        long start_1 = System.nanoTime();
        String result_1 = callOllamaJson(prompt1,model_1);
        long end_1 = System.nanoTime();
        double seconds_1 = (end_1 - start_1) / 1_000_000_000.0;
        double local_machine_time_1 = 22.7; // Angelo's MAC
        String paraphrase = jsonExtractField(result_1, "paraphrase");
        System.out.println("Model: " + model_1);
        System.out.println("Paraphrase: " + paraphrase);
        System.out.println("Time took this run: " + seconds_1 +" | Angelo's MAC: " + local_machine_time_1 + "\n\n");

        // LLAMA3.2:3b
        String model_2 = "llama3.2";
        long start_2 = System.nanoTime();
        String result_2 = callOllamaJson(prompt1,model_2);
        long end_2 = System.nanoTime();
        double seconds_2 = (end_2 - start_2) / 1_000_000_000.0;
        double local_machine_time_2 = 12.4; // Angelo's MAC
        paraphrase = jsonExtractField(result_2, "paraphrase");
        System.out.println("Model: " + model_2);
        System.out.println("Paraphrase: " + paraphrase);
        System.out.println("Time took this run: " + seconds_2 +" | Angelo's MAC: " + local_machine_time_2 + "\n\n");

        // QWEN 2.5 32b
        String model_3 = "qwen2.5:32b-instruct";
        long start_3 = System.nanoTime();
        String result_3 = callOllamaJson(prompt1,model_3);
        long end_3 = System.nanoTime();
        double seconds_3 = (end_3 - start_3) / 1_000_000_000.0;
        double local_machine_time_3 = 35.4; // Angelo's MAC
        paraphrase = jsonExtractField(result_3, "paraphrase");
        System.out.println("Model: " + model_3);
        System.out.println("Paraphrase: " + paraphrase);
        System.out.println("Time took this run: " + seconds_3 +" | Angelo's MAC: " + local_machine_time_3 + "\n\n");

        // GPT-OSS
        String model_4 = "gpt-oss:20b";
        long start_4 = System.nanoTime();
        String result_4 = callOllamaJson(prompt1,model_4);
        long end_4 = System.nanoTime();
        double seconds_4 = (end_4 - start_4) / 1_000_000_000.0;
        double local_machine_time_4 = 20.8; // Angelo's MAC
        paraphrase = jsonExtractField(result_4, "paraphrase");
        System.out.println("Model: " + model_4);
        System.out.println("Paraphrase: " + paraphrase);
        System.out.println("Time took this run: " + seconds_4 +" | Angelo's MAC: " + local_machine_time_4 + "\n\n");


//         Assertions
        Assert.assertTrue(
                "Ollama call too slow: " + seconds_1 + " seconds",
                seconds_1 < local_machine_time_1
        );

        Assert.assertTrue(
                "Ollama call too slow: " + seconds_2 + " seconds",
                seconds_2 < local_machine_time_2
        );

        Assert.assertTrue(
                "Ollama call too slow: " + seconds_3 + " seconds",
                seconds_3 < local_machine_time_3
        );

        Assert.assertTrue(
                "Ollama call too slow: " + seconds_4 + " seconds",
                seconds_4 < local_machine_time_4
        );
    }

    @Test
    public void relaxedPromptUseTemplatewithDocumentation() throws Exception {

        final String suoKif =
                "(forall (?X0 ?X1) (or (mother ?X0 ?X1) (not (parent ?X0 ?X1)) (not (instance ?X0 Organism)) (not (instance ?X1 Organism)) (not (instance ?X1 Woman))))";

        final String glossary =
                "mother = mother\n"
                        + "parent = parent\n"
                        + "instance = instance\n"
                        + "Organism = organism\n"
                        + "Woman = woman";

        String documentationBlock =
                "forall = \"Universal quantification. (forall (?X ...) A) means A holds for all values of the variables.\"\n"
                        + "or = \"Disjunction. (or A B ...) means at least one listed subformula is true.\"\n"
                        + "mother = \"The general relationship of motherhood. (mother ?CHILD ?MOTHER) means that ?MOTHER is the biological mother of ?CHILD.\"\n"
                        + "not = \"Negation. (not A) means A is false.\"\n"
                        + "parent = \"The general relationship of parenthood. (parent ?CHILD ?PARENT) means that ?PARENT is a biological parent of ?CHILD.\"\n"
                        + "instance = \"An object is an instance of a Class if it is included in that Class. An individual may be an instance of many classes, some of which may be subclasses of others. Thus, there is no assumption in the meaning of instance about specificity or uniqueness.\"\n"
                        + "Organism = \"Generally, a living individual, including all Plants and Animals.\"\n"
                        + "Woman = \"The class of Female Humans.\"";

        String cleanedTemplate = "for all an organism and another organism the other organism is a mother of the organism or the other organism is not a parent of the organism or the organism is not an instance of organism or the other organism is not an instance of organism or the other organism is not an instance of woman";

        String prompt1 =
                // =========================================================================================
                // PART 1: STATIC PREFIX
                // This section is identical for every single call. Ollama will cache these tokens (~300-400).
                // =========================================================================================
                "### SYSTEM ROLE:\n"
                        + "You are a logic-to-English translator. Your sole purpose is to rewrite the 'TEMPLATE' into natural English while maintaining a 1:1 logical mapping to the 'SUO-KIF' formula.\n\n"

                        + "### MANDATORY CONSTRAINTS:\n"
                        + "1. ISOMORPHISM: Maintain the exact logical skeleton. If the formula is a disjunction (OR), the output must be a disjunction. If it is a conditional (IF/THEN), the output must be a conditional.\n"
                        + "2. NO SUMMARIZATION: Do not explain the 'meaning' of the formula. Describe the logical relationship exactly as written.\n"
                        + "3. NO LEAKAGE: Do not use terms like 'parent' or 'sibling' unless they appear in the SUO-KIF or Glossary below.\n"
                        + "4. PREDICATE NAMES: Use the <GLOSSARY> to translate predicate symbols into English words.\n"
                        + "5. VERBALIZE SYMBOLS: Do not include mathematical or logical symbols (e.g., ¬, ∨, →, =>, symbols for 'not', 'or', 'implies'). Replace all logical operators with their full English word equivalents (e.g., 'it is not the case that', 'or', 'if... then').\n\n"

                        + "### OUTPUT FORMAT:\n"
                        + "Generate valid JSON ONLY. No preamble.\n"
                        + "Schema: {\n"
                        + "  \"variable_mapping\": { \"?X0\": \"...\" },\n"
                        + "  \"paraphrase\": \"...\"\n"
                        + "}\n\n"

                        + "### STYLE EXAMPLE (Abstract):\n"
                        + "SUO-KIF: (forall (?X) (or (not (instance ?X A)) (instance ?X B)))\n"
                        + "Correct: 'For every entity, either that entity is not an instance of A, or it is an instance of B.'\n"
                        + "Wrong: 'All A are B.' (Reason: This collapses the logical structure).\n"
                        + "Wrong: 'For all X, ¬A(X) ∨ B(X).' (Reason: Uses prohibited symbols).\n\n"

                        // =========================================================================================
                        // PART 2: DYNAMIC DATA
                        // This section changes with every loop iteration. The Cache stops working here.
                        // =========================================================================================
                        + "### REFERENCE MATERIAL:\n"
                        + "<GLOSSARY>\n" + glossary + "\n</GLOSSARY>\n"
                        + "<DOCUMENTATION>\n" + documentationBlock + "\n</DOCUMENTATION>\n\n"

                        + "### CURRENT TASK DATA:\n"
                        + "SUO-KIF: " + suoKif + "\n"
                        + "TEMPLATE: " + cleanedTemplate + "\n\n"

                        // Small trigger to ensure JSON generation starts immediately
                        + "### INSTRUCTION:\n"
                        + "Generate the JSON response now:";

        // QWEN 2.5 14b
        String model_1 = "qwen2.5:14b-instruct";
        long start_1 = System.nanoTime();
        String result_1 = callOllamaJson(prompt1,model_1);
        long end_1 = System.nanoTime();
        double seconds_1 = (end_1 - start_1) / 1_000_000_000.0;
        double local_machine_time_1 = 22.7; // Angelo's MAC
        String paraphrase = jsonExtractField(result_1, "paraphrase");
        System.out.println("Model: " + model_1);
        System.out.println("Paraphrase: " + paraphrase);
        System.out.println("Time took this run: " + seconds_1 +" | Angelo's MAC: " + local_machine_time_1 + "\n\n");

        // LLAMA3.2:3b
        String model_2 = "llama3.2";
        long start_2 = System.nanoTime();
        String result_2 = callOllamaJson(prompt1,model_2);
        long end_2 = System.nanoTime();
        double seconds_2 = (end_2 - start_2) / 1_000_000_000.0;
        double local_machine_time_2 = 12.4; // Angelo's MAC
        paraphrase = jsonExtractField(result_2, "paraphrase");
        System.out.println("Model: " + model_2);
        System.out.println("Paraphrase: " + paraphrase);
        System.out.println("Time took this run: " + seconds_2 +" | Angelo's MAC: " + local_machine_time_2 + "\n\n");

        // QWEN 2.5 32b
        String model_3 = "qwen2.5:32b-instruct";
        long start_3 = System.nanoTime();
        String result_3 = callOllamaJson(prompt1,model_3);
        long end_3 = System.nanoTime();
        double seconds_3 = (end_3 - start_3) / 1_000_000_000.0;
        double local_machine_time_3 = 35.4; // Angelo's MAC
        paraphrase = jsonExtractField(result_3, "paraphrase");
        System.out.println("Model: " + model_3);
        System.out.println("Paraphrase: " + paraphrase);
        System.out.println("Time took this run: " + seconds_3 +" | Angelo's MAC: " + local_machine_time_3 + "\n\n");

        // GPT-OSS
        String model_4 = "gpt-oss:20b";
        long start_4 = System.nanoTime();
        String result_4 = callOllamaJson(prompt1,model_4);
        long end_4 = System.nanoTime();
        double seconds_4 = (end_4 - start_4) / 1_000_000_000.0;
        double local_machine_time_4 = 20.8; // Angelo's MAC
        paraphrase = jsonExtractField(result_4, "paraphrase");
        System.out.println("Model: " + model_4);
        System.out.println("Paraphrase: " + paraphrase);
        System.out.println("REsult: "+result_4);
        System.out.println("Time took this run: " + seconds_4 +" | Angelo's MAC: " + local_machine_time_4 + "\n\n");


//         Assertions
        Assert.assertTrue(
                "Ollama call too slow: " + seconds_1 + " seconds",
                seconds_1 < local_machine_time_1
        );

        Assert.assertTrue(
                "Ollama call too slow: " + seconds_2 + " seconds",
                seconds_2 < local_machine_time_2
        );

        Assert.assertTrue(
                "Ollama call too slow: " + seconds_3 + " seconds",
                seconds_3 < local_machine_time_3
        );

        Assert.assertTrue(
                "Ollama call too slow: " + seconds_4 + " seconds",
                seconds_4 < local_machine_time_4
        );
    }

    @Test
    public void simplestUseTemplateOnly() throws Exception {

        String cleanedTemplate = "for all an organism and another organism the other organism is a mother of the organism or the other organism is not a parent of the organism or the organism is not an instance of organism or the other organism is not an instance of organism or the other organism is not an instance of woman";

        String prompt1 =
                "### SYSTEM ROLE:\n"
                        + "You are an English rewriter.\n"
                        + "Your sole job is to rewrite the given TEMPLATE into clear, grammatical, natural English.\n"
                        + "Do NOT use any external knowledge. Do NOT interpret SUMO. Do NOT infer new meaning.\n\n"

                        + "### HARD CONSTRAINTS:\n"
                        + "1. PRESERVE LOGICAL SKELETON: Keep the same logical connectives already present in the TEMPLATE.\n"
                        + "   - Keep all occurrences of: \"if\", \"then\", \"and\", \"or\", \"either\", \"not\", \"for all\", \"there exists\".\n"
                        + "   - Do NOT change \"and\" into \"or\" or vice versa.\n"
                        + "   - Do NOT introduce \"either/or\" unless the TEMPLATE already contains \"or\".\n"
                        + "2. NO NEW FACTS: Do not add information not stated in the TEMPLATE.\n"
                        + "3. NO SYMBOLS: Do not use logical symbols (¬, ∨, →, =>). Use plain English words only.\n"
                        + "4. ENTITY CONSISTENCY: Do not merge or split entities. If the TEMPLATE mentions X and Y, keep them distinct.\n\n"

                        + "### OUTPUT FORMAT:\n"
                        + "Return valid JSON ONLY. No preamble.\n"
                        + "Schema: {\"paraphrase\":\"...\"}\n\n"

                        + "### INPUT:\n"
                        + "TEMPLATE:\n"
                        + cleanedTemplate + "\n\n"

                        + "### INSTRUCTION:\n"
                        + "Rewrite the TEMPLATE now and output the JSON:";

        // QWEN 2.5 14b
        String model_1 = "qwen2.5:14b-instruct";
        long start_1 = System.nanoTime();
        String result_1 = callOllamaJson(prompt1,model_1);
        long end_1 = System.nanoTime();
        double seconds_1 = (end_1 - start_1) / 1_000_000_000.0;
        double local_machine_time_1 = 22.7; // Angelo's MAC
        String paraphrase = jsonExtractField(result_1, "paraphrase");
        System.out.println("Model: " + model_1);
        System.out.println("Paraphrase: " + paraphrase);
        System.out.println("Time took this run: " + seconds_1 +" | Angelo's MAC: " + local_machine_time_1 + "\n\n");

        // LLAMA3.2:3b
        String model_2 = "llama3.2";
        long start_2 = System.nanoTime();
        String result_2 = callOllamaJson(prompt1,model_2);
        long end_2 = System.nanoTime();
        double seconds_2 = (end_2 - start_2) / 1_000_000_000.0;
        double local_machine_time_2 = 12.4; // Angelo's MAC
        paraphrase = jsonExtractField(result_2, "paraphrase");
        System.out.println("Model: " + model_2);
        System.out.println("Paraphrase: " + paraphrase);
        System.out.println("Time took this run: " + seconds_2 +" | Angelo's MAC: " + local_machine_time_2 + "\n\n");

        // QWEN 2.5 32b
        String model_3 = "qwen2.5:32b-instruct";
        long start_3 = System.nanoTime();
        String result_3 = callOllamaJson(prompt1,model_3);
        long end_3 = System.nanoTime();
        double seconds_3 = (end_3 - start_3) / 1_000_000_000.0;
        double local_machine_time_3 = 35.4; // Angelo's MAC
        paraphrase = jsonExtractField(result_3, "paraphrase");
        System.out.println("Model: " + model_3);
        System.out.println("Paraphrase: " + paraphrase);
        System.out.println("Time took this run: " + seconds_3 +" | Angelo's MAC: " + local_machine_time_3 + "\n\n");

        // GPT-OSS
        String model_4 = "gpt-oss:20b";
        long start_4 = System.nanoTime();
        String result_4 = callOllamaJson(prompt1,model_4);
        long end_4 = System.nanoTime();
        double seconds_4 = (end_4 - start_4) / 1_000_000_000.0;
        double local_machine_time_4 = 20.8; // Angelo's MAC
        paraphrase = jsonExtractField(result_4, "paraphrase");
        System.out.println("Model: " + model_4);
        System.out.println("Paraphrase: " + paraphrase);
        System.out.println("REsult: "+result_4);
        System.out.println("Time took this run: " + seconds_4 +" | Angelo's MAC: " + local_machine_time_4 + "\n\n");


//         Assertions
        Assert.assertTrue(
                "Ollama call too slow: " + seconds_1 + " seconds",
                seconds_1 < local_machine_time_1
        );

        Assert.assertTrue(
                "Ollama call too slow: " + seconds_2 + " seconds",
                seconds_2 < local_machine_time_2
        );

        Assert.assertTrue(
                "Ollama call too slow: " + seconds_3 + " seconds",
                seconds_3 < local_machine_time_3
        );

        Assert.assertTrue(
                "Ollama call too slow: " + seconds_4 + " seconds",
                seconds_4 < local_machine_time_4
        );
    }

}
