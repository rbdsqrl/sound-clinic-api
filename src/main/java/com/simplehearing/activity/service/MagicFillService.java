package com.simplehearing.activity.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simplehearing.activity.dto.ChecklistQuestionInput;
import com.simplehearing.activity.dto.MagicFillRequest;
import com.simplehearing.activity.dto.MagicFillResponse;
import com.simplehearing.activity.enums.ChecklistQuestionType;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.organisation.entity.Organisation;
import com.simplehearing.organisation.enums.AiProvider;
import com.simplehearing.organisation.repository.OrganisationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "Magic Fill" — drafts Instructions or a Checklist for an activity being authored. Each org's
 * Business Owner configures a provider + API key in Organisation Settings ({@code ai_provider} /
 * {@code ai_api_key} on {@code organisations}); the key is never re-serialised to the frontend.
 * When unset, {@link #isEnabled} is false and the Activity module simply omits Magic Fill —
 * calling {@link #generate} anyway returns a clear 501 rather than a crash.
 *
 * No AI SDK dependency is pulled in — each provider's HTTP API is called directly via
 * {@link RestClient}, since only one or two calls per provider are needed.
 */
@Service
public class MagicFillService {

    private static final String ANTHROPIC_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String ANTHROPIC_MODEL = "claude-haiku-4-5-20251001";

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String OPENAI_MODEL = "gpt-4o-mini";

    private static final String GEMINI_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";
    private static final String GEMINI_MODEL = "gemini-2.0-flash";

    private static final String SYSTEM_PROMPT =
            "You are helping a paediatric therapy clinic staff member draft content for a therapy "
                    + "activity. Respond with ONLY raw JSON — no markdown fences, no commentary.";

    private final OrganisationRepository organisationRepository;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public MagicFillService(OrganisationRepository organisationRepository, ObjectMapper objectMapper) {
        this.organisationRepository = organisationRepository;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    public boolean isEnabled(UUID orgId) {
        return organisationRepository.findById(orgId)
                .map(o -> o.getAiProvider() != null && o.getAiApiKey() != null && !o.getAiApiKey().isBlank())
                .orElse(false);
    }

    public MagicFillResponse generate(UUID orgId, MagicFillRequest req) {
        Organisation org = organisationRepository.findById(orgId).orElse(null);
        if (org == null || org.getAiProvider() == null || org.getAiApiKey() == null || org.getAiApiKey().isBlank()) {
            throw new ApiException(HttpStatus.NOT_IMPLEMENTED,
                    "AI magic fill is not configured — ask your Business Owner to set it up in Organisation Settings.");
        }

        boolean isChecklist = "checklist".equalsIgnoreCase(req.section());
        String prompt = isChecklist ? checklistPrompt(req) : instructionsPrompt(req);

        String rawText = callProvider(org.getAiProvider(), org.getAiApiKey(), prompt);
        String json = stripCodeFence(rawText);

        try {
            if (isChecklist) {
                return new MagicFillResponse(List.of(), parseChecklist(json));
            } else {
                return new MagicFillResponse(parseInstructions(json), List.of());
            }
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI magic fill returned an unexpected response.");
        }
    }

    // ── Provider dispatch ───────────────────────────────────────────────────

    private String callProvider(AiProvider provider, String apiKey, String prompt) {
        return switch (provider) {
            case ANTHROPIC -> callAnthropic(apiKey, prompt);
            case OPENAI -> callOpenAi(apiKey, prompt);
            case GEMINI -> callGemini(apiKey, prompt);
        };
    }

    private String callAnthropic(String apiKey, String prompt) {
        try {
            JsonNode body = restClient.post()
                    .uri(ANTHROPIC_URL)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .header("content-type", "application/json")
                    .body(Map.of(
                            "model", ANTHROPIC_MODEL,
                            "max_tokens", 1024,
                            "system", SYSTEM_PROMPT,
                            "messages", List.of(Map.of("role", "user", "content", prompt))
                    ))
                    .retrieve()
                    .body(JsonNode.class);
            requireNonEmpty(body, "content");
            return body.get("content").get(0).path("text").asText();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw providerError("Anthropic", e);
        }
    }

    private String callOpenAi(String apiKey, String prompt) {
        try {
            JsonNode body = restClient.post()
                    .uri(OPENAI_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("content-type", "application/json")
                    .body(Map.of(
                            "model", OPENAI_MODEL,
                            "max_tokens", 1024,
                            "messages", List.of(
                                    Map.of("role", "system", "content", SYSTEM_PROMPT),
                                    Map.of("role", "user", "content", prompt))
                    ))
                    .retrieve()
                    .body(JsonNode.class);
            requireNonEmpty(body, "choices");
            return body.get("choices").get(0).path("message").path("content").asText();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw providerError("OpenAI", e);
        }
    }

    private String callGemini(String apiKey, String prompt) {
        try {
            String url = String.format(GEMINI_URL_TEMPLATE, GEMINI_MODEL, apiKey);
            JsonNode body = restClient.post()
                    .uri(url)
                    .header("content-type", "application/json")
                    .body(Map.of(
                            "system_instruction", Map.of("parts", List.of(Map.of("text", SYSTEM_PROMPT))),
                            "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt))))
                    ))
                    .retrieve()
                    .body(JsonNode.class);
            requireNonEmpty(body, "candidates");
            return body.get("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw providerError("Gemini", e);
        }
    }

    private void requireNonEmpty(JsonNode body, String field) {
        if (body == null || !body.has(field) || body.get(field).isEmpty()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI magic fill returned an empty response.");
        }
    }

    private ApiException providerError(String providerName, Exception e) {
        return new ApiException(HttpStatus.BAD_GATEWAY, providerName + " request failed: " + e.getMessage());
    }

    // ── Prompts ─────────────────────────────────────────────────────────────

    private String instructionsPrompt(MagicFillRequest req) {
        return """
                Draft a short, numbered set of step-by-step instructions (4-8 steps) a parent or \
                therapist would follow to run this activity with a child.

                Activity title: %s
                About: %s
                Therapy: %s
                Skills targeted: %s
                Age range: %s %s to %s %s
                Difficulty: %s

                Respond with ONLY a JSON array of strings, one per step, e.g. ["Step one...", "Step two..."].
                """.formatted(
                nullToEmpty(req.title()), nullToEmpty(req.aboutActivity()), nullToEmpty(req.therapyName()),
                req.skillNames() == null ? "" : String.join(", ", req.skillNames()),
                req.ageMinValue(), nullToEmpty(req.ageMinUnit()), req.ageMaxValue(), nullToEmpty(req.ageMaxUnit()),
                nullToEmpty(req.difficulty()));
    }

    private String checklistPrompt(MagicFillRequest req) {
        return """
                Draft a short progress-tracking checklist (2-5 questions) a therapist would fill in after \
                a child attempts this activity, to record how well they performed.

                Activity title: %s
                About: %s
                Therapy: %s
                Skills targeted: %s
                Age range: %s %s to %s %s
                Difficulty: %s

                Respond with ONLY a JSON array of objects, each shaped like:
                {"questionText": "...", "questionType": "SINGLE_CHOICE", "options": ["Independently", "With Assistance", "With Prompt", "Unable to do"]}
                questionType must be one of SINGLE_CHOICE, MULTI_CHOICE, or TEXT. Use an empty options array for TEXT questions.
                """.formatted(
                nullToEmpty(req.title()), nullToEmpty(req.aboutActivity()), nullToEmpty(req.therapyName()),
                req.skillNames() == null ? "" : String.join(", ", req.skillNames()),
                req.ageMinValue(), nullToEmpty(req.ageMinUnit()), req.ageMaxValue(), nullToEmpty(req.ageMaxUnit()),
                nullToEmpty(req.difficulty()));
    }

    // ── Response parsing ────────────────────────────────────────────────────

    private List<String> parseInstructions(String json) throws Exception {
        JsonNode array = objectMapper.readTree(json);
        List<String> out = new ArrayList<>();
        array.forEach(node -> out.add(node.asText()));
        return out;
    }

    private List<ChecklistQuestionInput> parseChecklist(String json) throws Exception {
        JsonNode array = objectMapper.readTree(json);
        List<ChecklistQuestionInput> out = new ArrayList<>();
        for (JsonNode node : array) {
            String questionText = node.path("questionText").asText();
            ChecklistQuestionType type;
            try {
                type = ChecklistQuestionType.valueOf(node.path("questionType").asText("SINGLE_CHOICE").toUpperCase());
            } catch (IllegalArgumentException e) {
                type = ChecklistQuestionType.SINGLE_CHOICE;
            }
            List<String> options = new ArrayList<>();
            node.path("options").forEach(o -> options.add(o.asText()));
            out.add(new ChecklistQuestionInput(questionText, type, options));
        }
        return out;
    }

    private String stripCodeFence(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\n?", "");
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }

    private String nullToEmpty(Object o) {
        return o == null ? "" : o.toString();
    }
}
