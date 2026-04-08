package com.simplehearing.config;

import com.simplehearing.entity.BlogPost;
import com.simplehearing.entity.GalleryItem;
import com.simplehearing.entity.HearingService;
import com.simplehearing.enums.GalleryType;
import com.simplehearing.repository.BlogPostRepository;
import com.simplehearing.repository.GalleryItemRepository;
import com.simplehearing.repository.HearingServiceRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class DataSeeder implements ApplicationRunner {

    private final HearingServiceRepository serviceRepo;
    private final BlogPostRepository blogRepo;
    private final GalleryItemRepository galleryRepo;

    public DataSeeder(HearingServiceRepository serviceRepo,
                      BlogPostRepository blogRepo,
                      GalleryItemRepository galleryRepo) {
        this.serviceRepo = serviceRepo;
        this.blogRepo = blogRepo;
        this.galleryRepo = galleryRepo;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedServices();
        seedBlogPosts();
        seedGallery();
    }

    private void seedServices() {
        if (serviceRepo.count() > 0) return;

        serviceRepo.saveAll(List.of(
            new HearingService(
                "Pediatric Speech-Language Therapy",
                "Speech and language therapy for children with developmental delays and communication difficulties.",
                "Comprehensive speech and language therapy for children with developmental delays, articulation disorders, language impairments, and communication difficulties.",
                List.of("Speech sound disorders", "Language delays & disorders", "Autism spectrum communication", "Stuttering & fluency", "Voice disorders in children", "Early intervention programs"),
                new BigDecimal("1200.00"), 45, 1),
            new HearingService(
                "Sensory Integration Therapy",
                "Specialized therapy to help children and adults process sensory information more effectively.",
                "Specialized therapy to help children and adults process and integrate sensory information from their environment more effectively.",
                List.of("Sensory processing disorder", "Tactile defensiveness", "Vestibular processing", "Proprioceptive integration", "Sensory diet programs", "School-based consultation"),
                new BigDecimal("1500.00"), 60, 2),
            new HearingService(
                "Stroke Rehabilitation Therapy",
                "Evidence-based speech and language rehabilitation for stroke survivors.",
                "Evidence-based speech and language rehabilitation for stroke survivors dealing with aphasia, dysarthria, or cognitive-communication disorders.",
                List.of("Aphasia treatment", "Dysarthria rehabilitation", "Cognitive-communication disorders", "Apraxia of speech", "Swallowing rehabilitation post-stroke", "Family counseling & training"),
                new BigDecimal("1000.00"), 45, 3),
            new HearingService(
                "Swallowing Disorder Therapy",
                "Diagnosis and treatment of dysphagia in children and adults using evidence-based techniques.",
                "Diagnosis and treatment of dysphagia (swallowing disorders) in children and adults using evidence-based techniques.",
                List.of("Clinical swallowing evaluation", "Videofluoroscopic assessment", "Oral motor therapy", "Diet modification guidance", "Feeding therapy for children", "Caregiver education"),
                new BigDecimal("1200.00"), 45, 4),
            new HearingService(
                "AVT for Cochlear Implant",
                "Auditory-Verbal Therapy helping children with cochlear implants develop spoken language.",
                "Auditory-Verbal Therapy (AVT) helps children with cochlear implants or hearing aids develop spoken language through listening.",
                List.of("Listening skill development", "Spoken language therapy", "Parent coaching & guidance", "Auditory skill progression", "Integration with education", "Outcome monitoring"),
                new BigDecimal("1800.00"), 60, 5),
            new HearingService(
                "Cochlear Implant Clinic",
                "Full-service cochlear implant support including candidacy evaluation, mapping, and rehabilitation.",
                "Full-service cochlear implant support including candidacy evaluation, device programming (mapping), and long-term habilitation.",
                List.of("Candidacy evaluation", "Pre-implant counseling", "Device programming (mapping)", "Post-implant rehabilitation", "Multi-brand device support", "Remote programming available"),
                new BigDecimal("2500.00"), 90, 6),
            new HearingService(
                "Hearing Aid Consultation",
                "Expert advice on hearing aid selection, fitting, and follow-up care.",
                "Comprehensive consultation for hearing aid selection, fitting, adjustment, and long-term care covering all major brands.",
                List.of("Hearing assessment", "Device recommendation", "Trial period guidance", "Fine-tuning & fitting", "Follow-up care", "Brand comparison — Resound, Starkey, Phonak, Oticon, Widex"),
                new BigDecimal("500.00"), 30, 7),
            new HearingService(
                "Hearing Test / Audiometry",
                "Comprehensive diagnostic audiological evaluation for all age groups.",
                "Comprehensive diagnostic audiological evaluation using state-of-the-art equipment to assess hearing ability across all age groups.",
                List.of("Pure tone audiometry", "Speech audiometry", "Tympanometry", "OAE testing", "ABR / ASSR", "Paediatric audiological assessment"),
                new BigDecimal("800.00"), 30, 8)
        ));
    }

    private void seedBlogPosts() {
        if (blogRepo.count() > 0) return;

        blogRepo.saveAll(List.of(
            new BlogPost(
                "Understanding Hearing Loss: Types, Causes & Treatment Options",
                "Audiology",
                "Hearing loss affects millions worldwide. Learn about the different types of hearing loss, their causes, and the latest treatment options available.",
                "<p>Hearing loss is one of the most common sensory disorders affecting people of all ages. It can range from mild difficulty hearing soft sounds to complete deafness.</p><h2>Types of Hearing Loss</h2><p><strong>Conductive hearing loss</strong> occurs when sound cannot efficiently travel through the outer ear canal to the eardrum and the tiny bones of the middle ear...</p>",
                "5 min read", true, LocalDate.of(2025, 3, 15), "#B3C8E8"),
            new BlogPost(
                "When Should Your Child See a Speech Therapist?",
                "Pediatric Care",
                "Early intervention is key for speech and language development. Discover the signs that indicate your child may benefit from professional speech therapy.",
                "<p>Speech and language development follows a predictable pattern in most children, but every child is unique. Knowing when to seek professional help can make a significant difference...</p>",
                "4 min read", false, LocalDate.of(2025, 2, 28), "#B3D9C8"),
            new BlogPost(
                "Cochlear Implants vs Hearing Aids: Which is Right for You?",
                "Hearing Technology",
                "Choosing between a cochlear implant and a hearing aid is a major decision. This guide breaks down the differences to help you make an informed choice.",
                "<p>Both cochlear implants and hearing aids can dramatically improve the quality of life for people with hearing loss, but they work very differently and are suited to different types and degrees of hearing loss...</p>",
                "6 min read", false, LocalDate.of(2025, 2, 10), "#D9C8B3"),
            new BlogPost(
                "Life After Stroke: Regaining Your Communication Skills",
                "Stroke Rehab",
                "Stroke can affect speech and language. Learn about evidence-based rehabilitation strategies that help stroke survivors regain communication abilities.",
                "<p>A stroke can have a profound effect on a person's ability to communicate. Aphasia, dysarthria, and cognitive-communication disorders are common after stroke...</p>",
                "7 min read", false, LocalDate.of(2025, 1, 22), "#C8B3D9"),
            new BlogPost(
                "Swallowing Difficulties: Don't Ignore the Signs",
                "Dysphagia",
                "Dysphagia (difficulty swallowing) can significantly impact quality of life. Recognize the warning signs and learn when to seek professional help.",
                "<p>Swallowing is something most of us do without thinking — about 600 times a day. But for millions of people, swallowing is a daily challenge that can lead to serious health complications...</p>",
                "4 min read", false, LocalDate.of(2025, 1, 5), "#D9B3B3"),
            new BlogPost(
                "Hearing Aids in 2025: The Latest Technology Explained",
                "Hearing Technology",
                "Hearing aid technology has advanced dramatically. From AI-powered sound processing to invisible-in-canal designs, explore what's available today.",
                "<p>Modern hearing aids are sophisticated medical devices packed with technology that would have seemed like science fiction just a decade ago...</p>",
                "5 min read", false, LocalDate.of(2024, 12, 18), "#B3D9D9")
        ));
    }

    private void seedGallery() {
        if (galleryRepo.count() > 0) return;

        galleryRepo.saveAll(List.of(
            // Photos
            new GalleryItem(GalleryType.PHOTO, "Clinic Reception", "Facility", null, null, "#B3C8E8", 1),
            new GalleryItem(GalleryType.PHOTO, "Audiology Lab", "Equipment", null, null, "#B3D9C8", 2),
            new GalleryItem(GalleryType.PHOTO, "Patient Consultation", "Clinical", null, null, "#D9C8B3", 3),
            new GalleryItem(GalleryType.PHOTO, "Hearing Aid Fitting", "Treatment", null, null, "#C8B3D9", 4),
            new GalleryItem(GalleryType.PHOTO, "Speech Therapy Room", "Facility", null, null, "#D9B3B3", 5),
            new GalleryItem(GalleryType.PHOTO, "Cochlear Implant Mapping", "Treatment", null, null, "#B3D9D9", 6),
            new GalleryItem(GalleryType.PHOTO, "Pediatric Therapy", "Clinical", null, null, "#D9D9B3", 7),
            new GalleryItem(GalleryType.PHOTO, "Diagnostic Equipment", "Equipment", null, null, "#B3C8D9", 8),
            new GalleryItem(GalleryType.PHOTO, "Team Photo", "Staff", null, null, "#D9C8D9", 9),
            // Videos
            new GalleryItem(GalleryType.VIDEO, "Hearing Aid Fitting Process", "Treatment", null, "3:45", "#B3C8E8", 1),
            new GalleryItem(GalleryType.VIDEO, "Cochlear Implant Patient Journey", "Clinical", null, "5:20", "#B3D9C8", 2),
            new GalleryItem(GalleryType.VIDEO, "Speech Therapy for Children", "Pediatric Care", null, "4:10", "#D9C8B3", 3),
            new GalleryItem(GalleryType.VIDEO, "Understanding Audiology", "Education", null, "6:00", "#C8B3D9", 4),
            new GalleryItem(GalleryType.VIDEO, "Patient Testimonials", "Stories", null, "2:30", "#D9B3B3", 5)
        ));
    }
}
