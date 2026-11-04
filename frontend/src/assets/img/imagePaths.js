// Ảnh tĩnh dùng khi chạy local (trước đây trỏ tới AWS S3 đã ngừng hoạt động).
// Thay các đường dẫn dưới đây bằng file ảnh thật trong frontend/public/images/ nếu có.
const PLACEHOLDER = '/images/placeholder.svg';

export const ImagePaths = {
  // Avatar images
  avatar: {
    default: PLACEHOLDER,
    sensei: '/images/oboe-sensei-avatar.svg',
  },

  // Footer images
  footer: {
    japanAi: PLACEHOLDER,
    japanCulture: PLACEHOLDER,
    japanSakura: PLACEHOLDER,
    japanVisa: PLACEHOLDER,
  },

  // Intro images
  intro: {
    forum: '/images/intro-forum.png',
    testFlashcard: '/images/intro-test-flashcard.png',
  },

  // Learn images
  learn: {
    celebration: PLACEHOLDER,
  },

  // Login images
  login: {
    loginBackground: PLACEHOLDER,
  },

  // Logo images
  logo: {
    logoTab: '/images/oboe-logo-o.svg',
  },
};
