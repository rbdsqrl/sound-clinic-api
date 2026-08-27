package com.simplehearing.assessment.def;

import com.simplehearing.assessment.enums.AssessmentType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fixed clinical instruments — ISAA (Indian Scale for Assessment of Autism) and PRBA
 * (Pre-Requisite Behavior Assessment). Content is hardcoded rather than staff-editable:
 * these are standardised scales, not a checklist a clinic customises per program.
 */
public final class AssessmentDefinitions {

    public record Option(String label, int score) {}
    public record Item(int number, String text, List<Option> options) {}
    public record Section(String name, List<Item> items) {}

    private AssessmentDefinitions() {}

    // ── ISAA — every item shares the same 1-5 scale ───────────────────────────────

    private static final List<Option> ISAA_SCALE = List.of(
            new Option("Rarely (up to 20%)", 1),
            new Option("Sometimes (21-40%)", 2),
            new Option("Frequently (41-60%)", 3),
            new Option("Mostly (61-80%)", 4),
            new Option("Always (81-100%)", 5)
    );

    private static Item isaaItem(int number, String text) {
        return new Item(number, text, ISAA_SCALE);
    }

    public static final List<Section> ISAA = List.of(
            new Section("Social Relationship and Reciprocity", List.of(
                    isaaItem(1, "Has poor eye contact"),
                    isaaItem(2, "Lacks social smile"),
                    isaaItem(3, "Remains aloof"),
                    isaaItem(4, "Does not reach out to others"),
                    isaaItem(5, "Unable to relate to peoples"),
                    isaaItem(6, "Unable to respond to social/environmental cues"),
                    isaaItem(7, "Engage in solitary and repetative play activities"),
                    isaaItem(8, "Unable to take turn in social interaction"),
                    isaaItem(9, "Does not maintain peer relationship")
            )),
            new Section("Emotional Responses", List.of(
                    isaaItem(10, "Shows inappropriate emotional response"),
                    isaaItem(11, "Shows exaggerated emotion"),
                    isaaItem(12, "Engages in self-stimulating emotions"),
                    isaaItem(13, "Lacks fear of danger"),
                    isaaItem(14, "Excited or agitated for no apparent reason")
            )),
            new Section("Speech-Language and Communication", List.of(
                    isaaItem(15, "Acquired speech and lost it"),
                    isaaItem(16, "Has difficulty in using non-verbal language or gestures to communicate"),
                    isaaItem(17, "Engages in stereotyped and repetitive use of language"),
                    isaaItem(18, "Engages in echolalic speech"),
                    isaaItem(19, "Produces infantile squeals/ unusual noises"),
                    isaaItem(20, "Unable to initiate or sustain conversation with others"),
                    isaaItem(21, "Uses jargon or meaningless words"),
                    isaaItem(22, "Uses pronoun reversals"),
                    isaaItem(23, "Unable to grasp pragmatics of communication (real meaning)")
            )),
            new Section("Behaviour Patterns", List.of(
                    isaaItem(24, "Engages in stereotyped and repetitive motor mannerisms"),
                    isaaItem(25, "Shows attachment to inanimate objects"),
                    isaaItem(26, "Shows hyperactivity/ restlessness"),
                    isaaItem(27, "Exhibits aggressive behavior"),
                    isaaItem(28, "Throws temper tantrums"),
                    isaaItem(29, "Engages in self-injurious behavior"),
                    isaaItem(30, "Insists on sameness")
            )),
            new Section("Sensory Aspects", List.of(
                    isaaItem(31, "Unusually sensitive to sensory stimuli"),
                    isaaItem(32, "Stares into space for long periods of time"),
                    isaaItem(33, "Has difficulty in tracking objects"),
                    isaaItem(34, "Has unusual vision"),
                    isaaItem(35, "Insensitive to pain"),
                    isaaItem(36, "Responds to objects/people unusually by smelling, touching or tasting")
            )),
            new Section("Cognitive Component", List.of(
                    isaaItem(37, "Inconsistent attention and concentration"),
                    isaaItem(38, "Shows delay in responding"),
                    isaaItem(39, "Has unusual memory of some kind"),
                    isaaItem(40, "Has 'savant' ability")
            ))
    );

    // ── PRBA — each item carries its own options; several are reverse-scored ──────

    private static final List<Option> ALWAYS_SOMETIMES_NEVER = List.of(
            new Option("Always", 2), new Option("Sometimes", 1), new Option("Never", 0));

    private static final List<Option> ALWAYS_SOMETIMES_NEVER_REVERSED = List.of(
            new Option("Always", 0), new Option("Sometimes", 1), new Option("Never", 2));

    public static final List<Section> PRBA = List.of(
            new Section("Joint Attention", List.of(
                    new Item(1, "How frequently do parents talk, use gestures and facial expressions to maintain and gain attention of their child?", ALWAYS_SOMETIMES_NEVER),
                    new Item(2, "How often does the child bring objects (toys) over to show an adult?", ALWAYS_SOMETIMES_NEVER),
                    new Item(3, "How often does the child use verbal/gestural signals for drawing an adult's attention to an event or to obtain an object?", ALWAYS_SOMETIMES_NEVER)
            )),
            new Section("Sustained Attention", List.of(
                    new Item(4, "How long does the child attend to one particular activity (adult directed)?", List.of(
                            new Option("2.5-4 mins", 2), new Option("1-2.5 mins", 1), new Option("< 1 min", 0))),
                    new Item(5, "Does the child show interest in exploring and manipulating objects?", ALWAYS_SOMETIMES_NEVER),
                    new Item(6, "How frequently does the child get distracted from a task s/he is attending?", ALWAYS_SOMETIMES_NEVER_REVERSED)
            )),
            new Section("Eye Contact", List.of(
                    new Item(7, "Does the child initiate and maintain eye contact by looking directly at speaker's face during interaction?", ALWAYS_SOMETIMES_NEVER),
                    new Item(8, "How often does the child make eye contact to get an adult's attention?", ALWAYS_SOMETIMES_NEVER)
            )),
            new Section("Eye Gaze", List.of(
                    new Item(9, "Does child show interest in looking at pictures/objects when they are named?", ALWAYS_SOMETIMES_NEVER),
                    new Item(10, "How often does the child look across to see what you are pointing at?", ALWAYS_SOMETIMES_NEVER),
                    new Item(11, "How often do parents talk about/label items the child is manipulating?", ALWAYS_SOMETIMES_NEVER),
                    new Item(12, "How often do parents rely on their child's eye gaze to maintain attention?", ALWAYS_SOMETIMES_NEVER)
            )),
            new Section("Sitting Tolerance", List.of(
                    new Item(13, "How often does the child sit in one place till the completion of a particular activity?", ALWAYS_SOMETIMES_NEVER),
                    new Item(14, "How frequently do parents change play activity to make their child be seated in a place?", ALWAYS_SOMETIMES_NEVER_REVERSED),
                    new Item(15, "How long can the child sit in a place for a particular activity?", List.of(
                            new Option("4-6 mins", 2), new Option("2-4 mins", 1), new Option("< 2 mins", 0))),
                    new Item(16, "Is the child always on move?", ALWAYS_SOMETIMES_NEVER_REVERSED)
            )),
            new Section("Compliance", List.of(
                    new Item(17, "How often does the child complete a given activity?", ALWAYS_SOMETIMES_NEVER),
                    new Item(18, "How often does the child protest and pick up an activity of his/her interest?", ALWAYS_SOMETIMES_NEVER_REVERSED),
                    new Item(19, "How often does the child cooperate when the activity is changed?", ALWAYS_SOMETIMES_NEVER),
                    new Item(20, "How often does the child follow adult's instructions during an activity?", ALWAYS_SOMETIMES_NEVER)
            ))
    );

    public static List<Section> sectionsFor(AssessmentType type) {
        return type == AssessmentType.ISAA ? ISAA : PRBA;
    }

    public static int maxScoreFor(AssessmentType type) {
        return sectionsFor(type).stream()
                .flatMap(s -> s.items().stream())
                .mapToInt(i -> i.options().stream().mapToInt(Option::score).max().orElse(0))
                .sum();
    }

    /** item number -> the item, across every section. */
    public static Map<Integer, Item> itemsByNumber(AssessmentType type) {
        return sectionsFor(type).stream()
                .flatMap(s -> s.items().stream())
                .collect(Collectors.toMap(Item::number, i -> i));
    }
}
