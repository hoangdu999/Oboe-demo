import axios from '@/api/axios';
import { handleApiError } from '@/api/apiUtils';

const PREFIX = '/api/ai';

// AI requests gọi Gemini API có thể mất nhiều thời gian, dùng timeout dài hơn
const AI_TIMEOUT = 120000; // 120 giây

const aiApi = {
  // Sinh câu hỏi từ 1 cardItem cụ thể
  async generateQuestionsByCardItem(cardItemId) {
    try {
      const res = await axios.get(`${PREFIX}/generate-question/${cardItemId}`, { timeout: AI_TIMEOUT });
      return res.data;
    } catch (error) {
      throw new Error(handleApiError(error));
    }
  },

  // Sinh câu hỏi ngẫu nhiên từ 1 user
  async generateQuestionsByUserId() {
    try {
      const res = await axios.get(`${PREFIX}/generate-question`, { timeout: AI_TIMEOUT });
      return res.data;
    } catch (error) {
      throw new Error(handleApiError(error));
    }
  },

  // Gửi bài làm để AI chấm điểm
  async evaluateUserAnswers(userAnswerAIDTO) {
    try {
      const res = await axios.post(`${PREFIX}/evaluate`, userAnswerAIDTO, { timeout: AI_TIMEOUT });
      return res.data;
    } catch (error) {
      throw new Error(handleApiError(error));
    }
  },
  // Dịch tiếng Nhật sang tiếng Việt (có giải thích)
  async translateJapaneseToVietnamese(text) {
    try {
      const res = await axios.post(`${PREFIX}/translate`, { text }, { timeout: AI_TIMEOUT });
      return res.data;
    } catch (error) {
      throw new Error(handleApiError(error));
    }
  }
};

export default aiApi;
