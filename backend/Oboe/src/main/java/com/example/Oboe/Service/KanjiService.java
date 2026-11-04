package com.example.Oboe.Service;

import com.example.Oboe.DTOs.KanjiDTOs;
import com.example.Oboe.Entity.Kanji;
import com.example.Oboe.Entity.Reading;
import com.example.Oboe.Repository.KanjiRepository;
import com.example.Oboe.Repository.ReadingRepository;
import com.example.Oboe.Util.RomajiUtils;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class KanjiService {

    private final KanjiRepository kanjiRepository;
    private final ReadingRepository readingRepository;

    public KanjiService(KanjiRepository kanjiRepository, ReadingRepository readingRepository) {
        this.kanjiRepository = kanjiRepository;
        this.readingRepository = readingRepository;
    }


    public Map<String, Object> getAllKanji(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Kanji> kanjiPage = kanjiRepository.findAll(pageable);

        List<KanjiDTOs> kanjiDTOs = kanjiPage.getContent()
                .stream()
                .map(this::kanjiToDTO)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("kanjis", kanjiDTOs);
        response.put("currentPage", kanjiPage.getNumber());
        response.put("pageSize", kanjiPage.getSize());
        response.put("totalElements", kanjiPage.getTotalElements());
        response.put("totalPages", kanjiPage.getTotalPages());
        response.put("isLastPage", kanjiPage.isLast());

        return response;
    }


    public KanjiDTOs createKanji(KanjiDTOs dto){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            throw new SecurityException("Bạn không có quyền tạo Kanji.");
        }

        Kanji kanji = new Kanji();
        kanji.setStrokes(dto.getStrokes());
        kanji.setMeaning(dto.getMeaning());
        kanji.setCharacter_name(dto.getCharacterName());
        kanji.setVietnamesePronunciation(dto.getVietnamesePronunciation());

        Kanji saved = kanjiRepository.save(kanji);
        return kanjiToDTO(saved);
    }


    public KanjiDTOs getKanjiByKanjiId(UUID kanjiId) {
        Kanji kanji = kanjiRepository.findById(kanjiId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Kanji với ID: " + kanjiId));
        return kanjiToDTO(kanji);
    }


    public KanjiDTOs updateKanji(KanjiDTOs dto, UUID kanjiId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() ||
                !auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            throw new SecurityException("Bạn không có quyền cập nhật Kanji.");
        }

        Kanji kanji = getKanjiEntityById(kanjiId);
        if (kanji == null) return null;

        if (dto.getCharacterName() != null) {
            kanji.setCharacter_name(dto.getCharacterName());
        }
        if (dto.getMeaning() != null) {
            kanji.setMeaning(dto.getMeaning());
        }
        if (dto.getStrokes() != null) {
            kanji.setStrokes(dto.getStrokes());
        }
        if (dto.getVietnamesePronunciation() != null) {
            kanji.setVietnamesePronunciation(dto.getVietnamesePronunciation());
        }

        Kanji updated = kanjiRepository.save(kanji);
        return kanjiToDTO(updated);
    }


    public void deleteKanji(UUID kanjiId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() ||
                !auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            throw new SecurityException("Bạn không có quyền xóa Kanji.");
        }

        Kanji kanji = getKanjiEntityById(kanjiId);
        if (kanji == null) throw new RuntimeException("Không tìm thấy Kanji với ID: " + kanjiId);

        kanjiRepository.delete(kanji);
    }


    public List<KanjiDTOs> searchKanji(String keyword) {
        List<Kanji> kanjis = new ArrayList<>(kanjiRepository.searchKanji(keyword));
        Set<UUID> found = kanjis.stream().map(Kanji::getKanjiId).collect(Collectors.toSet());

        Map<UUID, List<String>> readings = readingRepository.findByOwnerType("kanji").stream()
                .collect(Collectors.groupingBy(Reading::getOwnerId,
                        Collectors.mapping(Reading::getReadingText, Collectors.toList())));

        for (Kanji k : kanjiRepository.findAll()) {
            if (found.contains(k.getKanjiId())) continue;
            List<String> sources = new ArrayList<>();
            sources.add(k.getCharacter_name());
            sources.addAll(readings.getOrDefault(k.getKanjiId(), List.of()));
            if (RomajiUtils.matchesRomaji(keyword, sources)) {
                kanjis.add(k);
                found.add(k.getKanjiId());
            }
        }

        return kanjis.stream()
                .map(this::kanjiToDTO)
                .collect(Collectors.toList());
    }


    public List<KanjiDTOs> getRelatedKanji(UUID kanjiId) {
        Kanji kanji = kanjiRepository.findById(kanjiId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Kanji"));

        String meaning = kanji.getMeaning();
        List<Kanji> relatedKanjis = kanjiRepository.findRelatedByMeaning(meaning, kanjiId);

        return relatedKanjis.stream()
                .map(this::kanjiToDTO)
                .collect(Collectors.toList());
    }


    public Kanji getKanjiEntityById(UUID kanjiId) {
        return kanjiRepository.findById(kanjiId).orElse(null);
    }


    public KanjiDTOs kanjiToDTO(Kanji kanji) {
        KanjiDTOs dto = new KanjiDTOs();
        dto.setKanjiId(kanji.getKanjiId());
        dto.setCharacterName(kanji.getCharacter_name());
        dto.setMeaning(kanji.getMeaning());
        dto.setStrokes(kanji.getStrokes());
        dto.setVietnamesePronunciation(kanji.getVietnamesePronunciation());
        List<Reading> readings = readingRepository.findByOwnerTypeAndOwnerId("kanji", kanji.getKanjiId());
        dto.setReading(joinReadings(readings, "onyomi"));
        dto.setKunyomi(joinReadings(readings, "kunyomi"));
        return dto;
    }

    private String joinReadings(List<Reading> readings, String readingType) {
        return readings.stream()
                .filter(reading -> readingType.equalsIgnoreCase(reading.getReadingType()))
                .map(Reading::getReadingText)
                .distinct()
                .collect(Collectors.joining(", "));
    }
}
    
