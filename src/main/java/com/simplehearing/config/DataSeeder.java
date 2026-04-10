package com.simplehearing.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simplehearing.data.page.PageDataPort;
import com.simplehearing.data.page.PageEntity;
import com.simplehearing.data.page.PageComponentEntity;
import com.simplehearing.dto.page.section.BrandsData;
import com.simplehearing.dto.page.section.CtaBannerData;
import com.simplehearing.dto.page.section.HeroData;
import com.simplehearing.dto.page.section.ServicesPreviewData;
import com.simplehearing.dto.page.section.StatsBarData;
import com.simplehearing.dto.page.section.WhyChooseUsData;
import com.simplehearing.dto.page.section.ServicesPreviewData.ServiceCard;
import com.simplehearing.dto.page.section.StatsBarData.StatItem;
import com.simplehearing.dto.page.section.WhyChooseUsData.BulletPoint;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner seedPageData(PageDataPort pageDataPort, ObjectMapper objectMapper) {
        return args -> {
            if (pageDataPort.findByPageId("home").isPresent()) {
                return;
            }

            PageEntity home = new PageEntity(
                "home",
                "Simple Hearing & Speech Care | Expert Audiological Care",
                "Board-certified audiologist Dr. Suravi Dash providing comprehensive hearing and speech care services in Odisha, India."
            );

            home.addSection(new PageComponentEntity(
                "hero",
                "hero",
                1,
                toJson(objectMapper, HeroData.of(
                    "Better Hearing",
                    "A Seamless Journey To",
                    "Expert audiological care by Dr. Suravi Dash",
                    new com.simplehearing.dto.page.CtaButton("Book Appointment", "/appointment"),
                    new com.simplehearing.dto.page.CtaButton("Our Services", "/services")
                ))
            ));

            home.addSection(new PageComponentEntity(
                "stats_bar",
                "stats_bar",
                2,
                toJson(objectMapper, StatsBarData.of(List.of(
                    new StatItem("13+", "Years Experience"),
                    new StatItem("5000+", "Patients Served"),
                    new StatItem("6", "Services Offered")
                )))
            ));

            home.addSection(new PageComponentEntity(
                "services_preview",
                "services_preview",
                3,
                toJson(objectMapper, ServicesPreviewData.of(
                    "Our Services",
                    "Comprehensive hearing & speech care",
                    "View All Services",
                    "/services",
                    List.of(
                        new ServiceCard(1, "Pediatric Speech-Language Therapy", "Speech development for children", "pediatric"),
                        new ServiceCard(2, "Sensory Integration Therapy", "Sensory processing disorders", "sensory"),
                        new ServiceCard(3, "Stroke Rehabilitation Therapy", "Post-stroke speech recovery", "stroke"),
                        new ServiceCard(4, "Swallowing Disorder Therapy", "Dysphagia treatment", "swallowing"),
                        new ServiceCard(5, "AVT for Cochlear Implant", "Auditory-Verbal Therapy", "avt"),
                        new ServiceCard(6, "Cochlear Implant Clinic", "Full cochlear implant services", "cochlear")
                    )
                ))
            ));

            home.addSection(new PageComponentEntity(
                "brands",
                "brands",
                4,
                toJson(objectMapper, BrandsData.of(
                    "Hearing Aid Brands",
                    "Top brands we work with",
                    List.of("GN Resound", "Starkey", "Phonak", "Oticon", "Widex", "Cochlear")
                ))
            ));

            home.addSection(new PageComponentEntity(
                "why_choose_us",
                "why_choose_us",
                5,
                toJson(objectMapper, WhyChooseUsData.of(
                    "Why Choose Us?",
                    "Our commitment to your hearing health",
                    List.of(
                        new BulletPoint("check_circle", "Board-certified audiologist with 13+ years"),
                        new BulletPoint("star", "State-of-the-art diagnostic equipment"),
                        new BulletPoint("person", "Personalized patient-centered care"),
                        new BulletPoint("location", "Conveniently located clinic")
                    )
                ))
            ));

            home.addSection(new PageComponentEntity(
                "cta_banner",
                "cta_banner",
                6,
                toJson(objectMapper, CtaBannerData.of(
                    "Ready to improve your hearing?",
                    "Schedule a free consultation today",
                    "Book Free Consultation",
                    "/appointment"
                ))
            ));

            pageDataPort.save(home);
        };
    }

    private String toJson(ObjectMapper objectMapper, Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize page component", e);
        }
    }
}
