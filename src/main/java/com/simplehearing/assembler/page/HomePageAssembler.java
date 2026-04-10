package com.simplehearing.assembler.page;

import com.simplehearing.dto.page.*;
import com.simplehearing.dto.page.section.*;
import com.simplehearing.dto.page.section.ServicesPreviewData.ServiceCard;
import com.simplehearing.dto.page.section.StatsBarData.StatItem;
import com.simplehearing.dto.page.section.WhyChooseUsData.BulletPoint;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Assembles the home page layout.
 *
 * ── Stub note ──────────────────────────────────────────────────────────────
 * All data is currently hardcoded. Future integration points are marked with
 * TODO comments. When a repository is introduced, inject it here and replace
 * the relevant stub list — no other class needs to change.
 * ───────────────────────────────────────────────────────────────────────────
 */
@Component("home")
public class HomePageAssembler implements PageAssembler {

    @Override
    public PageResponse assemble() {
        return new PageResponse(
            "home",
            meta(),
            List.of(
                new PageSection("hero",             "hero",             1, hero()),
                new PageSection("stats_bar",        "stats_bar",        2, statsBar()),
                new PageSection("services_preview", "services_preview", 3, servicesPreview()),
                new PageSection("brands",           "brands",           4, brands()),
                new PageSection("why_choose_us",    "why_choose_us",    5, whyChooseUs()),
                new PageSection("cta_banner",       "cta_banner",       6, ctaBanner())
            )
        );
    }

    // ── Private section builders ────────────────────────────────────────────

    private PageMeta meta() {
        return new PageMeta(
            "Simple Hearing & Speech Care | Expert Audiological Care",
            "Board-certified audiologist Dr. Suravi Dash providing comprehensive " +
            "hearing and speech care services in Odisha, India."
        );
    }

    private HeroData hero() {
        return HeroData.of(
            "Better Hearing",
            "A Seamless Journey To",
            "Expert audiological care by Dr. Suravi Dash",
            new CtaButton("Book Appointment", "/appointment"),
            new CtaButton("Our Services",     "/services")
        );
    }

    private StatsBarData statsBar() {
        return StatsBarData.of(List.of(
            new StatItem("13+",   "Years Experience"),
            new StatItem("5000+", "Patients Served"),
            new StatItem("6",     "Services Offered")
        ));
    }

    private ServicesPreviewData servicesPreview() {
        // TODO: replace stub list with ServiceRepository.findAll() once DB is wired
        return ServicesPreviewData.of(
            "Our Services",
            "Comprehensive hearing & speech care",
            "View All Services",
            "/services",
            stubServices()
        );
    }

    private List<ServiceCard> stubServices() {
        // TODO: load from database
        return List.of(
            new ServiceCard(1, "Pediatric Speech-Language Therapy", "Speech development for children",      "pediatric"),
            new ServiceCard(2, "Sensory Integration Therapy",       "Sensory processing disorders",         "sensory"),
            new ServiceCard(3, "Stroke Rehabilitation Therapy",     "Post-stroke speech recovery",          "stroke"),
            new ServiceCard(4, "Swallowing Disorder Therapy",       "Dysphagia treatment",                  "swallowing"),
            new ServiceCard(5, "AVT for Cochlear Implant",          "Auditory-Verbal Therapy",              "avt"),
            new ServiceCard(6, "Cochlear Implant Clinic",           "Full cochlear implant services",       "cochlear")
        );
    }

    private BrandsData brands() {
        // TODO: load from database or config
        return BrandsData.of(
            "Hearing Aid Brands",
            "Top brands we work with",
            List.of("GN Resound", "Starkey", "Phonak", "Oticon", "Widex", "Cochlear")
        );
    }

    private WhyChooseUsData whyChooseUs() {
        return WhyChooseUsData.of(
            "Why Choose Us?",
            "Our commitment to your hearing health",
            List.of(
                new BulletPoint("check_circle", "Board-certified audiologist with 13+ years"),
                new BulletPoint("star",         "State-of-the-art diagnostic equipment"),
                new BulletPoint("person",       "Personalized patient-centered care"),
                new BulletPoint("location",     "Conveniently located clinic")
            )
        );
    }

    private CtaBannerData ctaBanner() {
        return CtaBannerData.of(
            "Ready to improve your hearing?",
            "Schedule a free consultation today",
            "Book Free Consultation",
            "/appointment"
        );
    }
}
