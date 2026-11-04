<template>
  <DetailPage
    type="word"
    :item="wordData"
    :itemId="wordId"
    mainField="words"
    readingField=""
    meaningField="meanning"
    :showRelated="true"
    :relatedItems="relatedKanji"
    :relatedTitle="t('kanjiDetail.relatedKanji')"
    relatedMainField="characterName"
    relatedKeyField="kanjiId"
    :emptyRelatedMessage="t('kanjiDetail.noRelatedKanji')"
    :notFoundMessage="t('wordDetail.notFound')"
    @relatedItemClick="navigateToKanjiDetail"
  />
</template>

<script>
import { ref, watch, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import DetailPage from '@/components/layout/detail-search/DetailSearch.vue';
import vocabularyApi from '@/api/modules/vocabularyApi';

export default {
  name: 'WordDetail',
  components: {
    DetailPage
  },
  setup() {
    const { t } = useI18n();
    const route = useRoute();
    const router = useRouter();
    const wordId = ref(route.params.id);
    const wordData = ref(null);
    const relatedKanji = ref([]);
    const isLoading = ref(false);

    // Function to fetch word data
    const fetchWordData = async (id) => {
      try {
        isLoading.value = true;
        const response = await vocabularyApi.getById(id);
        wordData.value = response;
      } catch (error) {
        console.error('Error fetching word data:', error);
        wordData.value = null;
      } finally {
        isLoading.value = false;
      }
    };

    const fetchRelatedKanji = async (id) => {
      try {
        relatedKanji.value = await vocabularyApi.getRelatedKanji(id);
      } catch (error) {
        console.error('Error fetching related kanji for word:', error);
        relatedKanji.value = [];
      }
    };

    const navigateToKanjiDetail = (item) => {
      router.push({ name: 'KanjiDetail', params: { id: item.kanjiId || item.id } });
    };

    // Initial fetch
    onMounted(() => {
      if (wordId.value) {
        fetchWordData(wordId.value);
        fetchRelatedKanji(wordId.value);
      }
    });

    // Watch for route changes
    watch(
      () => route.params.id,
      (newId) => {
        if (newId) {
          wordId.value = newId;
          fetchWordData(wordId.value);
          fetchRelatedKanji(wordId.value);
        }
      }
    );

    return {
      t,
      wordData,
      relatedKanji,
      wordId,
      isLoading,
      navigateToKanjiDetail
    };
  }
};
</script>

<style lang="scss" scoped>
@use '@/views/search-japanese/word-detail/WordDetail.scss';
</style>
