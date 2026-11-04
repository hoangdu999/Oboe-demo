// src/firebase.js
import firebase from 'firebase/compat/app';
import 'firebase/compat/auth';

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
  measurementId: import.meta.env.VITE_FIREBASE_MEASUREMENT_ID,
};

// Đăng nhập Google/Facebook qua Firebase là tuỳ chọn.
// Nếu không cấu hình .env Firebase, app vẫn chạy với đăng nhập email/số điện thoại.
let auth = null;

try {
  if (firebaseConfig.apiKey && !firebase.apps?.length) {
    firebase.initializeApp(firebaseConfig);
  }
  if (firebase.apps?.length) {
    auth = firebase.auth();
  } else {
    console.warn('Firebase chưa được cấu hình — bỏ qua đăng nhập Google/Facebook.');
  }
} catch (error) {
  console.error('Firebase initialization error', error);
}

export { auth, firebase };
