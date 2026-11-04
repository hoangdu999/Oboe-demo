package com.example.Oboe.Service;

import com.example.Oboe.DTOs.*;
import com.example.Oboe.Entity.*;
import com.example.Oboe.Repository.*;
import com.example.Oboe.Util.RomajiUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {

    @Autowired
    private KanjiRepository kanjiRepository;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private GrammarRepository grammarRepository;

    @Autowired
    private SampleSentenceRepository sampleSentenceRepository;

    @Autowired
    private ReadingRepository readingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuizzesRepository quizzesRepository;

    @Autowired
    private FlashCardRepository flashCardRepository;

    // Gợi ý tất cả các loại học liệu (cho /suggest)
    public List<Map<String, String>> suggestAllTypes(String keyword) {
        List<Map<String, String>> suggestions = new ArrayList<>();

        suggestions.addAll(searchByType(keyword, "vocabulary"));
        suggestions.addAll(searchByType(keyword, "kanji"));
        suggestions.addAll(searchByType(keyword, "grammar"));
        suggestions.addAll(searchByType(keyword, "sentence"));

        return suggestions;
    }

    // Tìm theo type cụ thể (học liệu)
    public List<Map<String, String>> searchByType(String keyword, String type) {
        List<Map<String, String>> suggestions = new ArrayList<>();

        switch (type.toLowerCase()) {
            case "vocabulary": {
                Map<UUID, List<String>> readings = readingsByOwner("vocabulary");
                List<Vocabulary> results = new ArrayList<>(vocabularyRepository.searchVocabulary(keyword));
                Set<UUID> found = results.stream().map(Vocabulary::getVocalbId).collect(Collectors.toSet());
                for (Vocabulary v : vocabularyRepository.findAll()) {
                    if (found.contains(v.getVocalbId())) continue;
                    List<String> sources = new ArrayList<>();
                    sources.add(v.getWords());
                    sources.addAll(readings.getOrDefault(v.getVocalbId(), List.of()));
                    if (RomajiUtils.matchesRomaji(keyword, sources)) {
                        results.add(v);
                        found.add(v.getVocalbId());
                    }
                }
                for (Vocabulary v : results) {
                    Map<String, String> item = new HashMap<>();
                    item.put("type", "vocabulary");
                    item.put("word", v.getWords());
                    item.put("reading", v.getVietnamesePronunciation());
                    item.put("meaning", v.getMeanning());
                    item.put("id", v.getVocalbId().toString());
                    suggestions.add(item);
                }
                break;
            }

            case "kanji": {
                Map<UUID, List<String>> readings = readingsByOwner("kanji");
                List<Kanji> results = new ArrayList<>(kanjiRepository.searchKanji(keyword));
                Set<UUID> found = results.stream().map(Kanji::getKanjiId).collect(Collectors.toSet());
                for (Kanji k : kanjiRepository.findAll()) {
                    if (found.contains(k.getKanjiId())) continue;
                    List<String> sources = new ArrayList<>();
                    sources.add(k.getCharacter_name());
                    sources.addAll(readings.getOrDefault(k.getKanjiId(), List.of()));
                    if (RomajiUtils.matchesRomaji(keyword, sources)) {
                        results.add(k);
                        found.add(k.getKanjiId());
                    }
                }
                for (Kanji k : results) {
                    Map<String, String> item = new HashMap<>();
                    item.put("type", "kanji");
                    item.put("word", k.getCharacter_name());
                    item.put("reading", k.getVietnamesePronunciation());
                    item.put("meaning", k.getMeaning());
                    item.put("id", k.getKanjiId().toString());
                    suggestions.add(item);
                }
                break;
            }

            case "grammar": {
                Map<UUID, List<String>> readings = readingsByOwner("grammar");
                List<Grammar> results = new ArrayList<>(grammarRepository.searchGrammar(keyword));
                Set<UUID> found = results.stream().map(Grammar::getGrammaID).collect(Collectors.toSet());
                for (Grammar g : grammarRepository.findAll()) {
                    if (found.contains(g.getGrammaID())) continue;
                    List<String> sources = new ArrayList<>();
                    sources.add(g.getStructure());
                    sources.add(g.getExample());
                    sources.add(g.getVietnamesePronunciation());
                    sources.addAll(readings.getOrDefault(g.getGrammaID(), List.of()));
                    if (RomajiUtils.matchesRomaji(keyword, sources)) {
                        results.add(g);
                        found.add(g.getGrammaID());
                    }
                }
                for (Grammar g : results) {
                    Map<String, String> item = new HashMap<>();
                    item.put("type", "grammar");
                    item.put("word", g.getStructure());
                    item.put("reading", g.getVietnamesePronunciation());
                    item.put("meaning", g.getExplanation());
                    item.put("id", g.getGrammaID().toString());
                    suggestions.add(item);
                }
                break;
            }

            case "sentence": {
                Map<UUID, List<String>> readings = readingsByOwner("sentence");
                List<SampleSentence> results = new ArrayList<>(sampleSentenceRepository.searchByVietnameseMeaning(keyword));
                Set<UUID> found = results.stream().map(SampleSentence::getSample_sentence_id).collect(Collectors.toSet());
                for (SampleSentence s : sampleSentenceRepository.findAll()) {
                    if (found.contains(s.getSample_sentence_id())) continue;
                    List<String> sources = new ArrayList<>();
                    sources.add(s.getJapaneseText());
                    sources.add(s.getVietnamesePronunciation());
                    sources.addAll(readings.getOrDefault(s.getSample_sentence_id(), List.of()));
                    if (RomajiUtils.matchesRomaji(keyword, sources)) {
                        results.add(s);
                        found.add(s.getSample_sentence_id());
                    }
                }
                for (SampleSentence s : results) {
                    Map<String, String> item = new HashMap<>();
                    item.put("type", "sentence");
                    item.put("word", s.getJapaneseText());
                    item.put("reading", s.getVietnamesePronunciation());
                    item.put("meaning", s.getVietnameseMeaning());
                    item.put("id", s.getSample_sentence_id().toString());
                    suggestions.add(item);
                }
                break;
            }

            default:
                throw new IllegalArgumentException("Type không hợp lệ: " + type);
        }

        return suggestions;
    }

    // Gom các reading của từng owner theo ownerType (dùng để tra romaji)
    private Map<UUID, List<String>> readingsByOwner(String ownerType) {
        Map<UUID, List<String>> map = new HashMap<>();
        for (Reading r : readingRepository.findByOwnerType(ownerType)) {
            map.computeIfAbsent(r.getOwnerId(), k -> new ArrayList<>()).add(r.getReadingText());
        }
        return map;
    }
}
