// Firebase WEB config (public values, not secrets): Project settings -> Your apps -> Karalak Web.
export const firebaseConfig = {
  apiKey: "AIzaSyAvhmsNKlQ3G_tp80xv4ob5Z9Br4Y6BGVc",
  authDomain: "karalak-b6e11.firebaseapp.com",
  projectId: "karalak-b6e11",
  appId: "1:991414245044:web:667700203cd214db0569e1",
  storageBucket: "karalak-b6e11.firebasestorage.app",
  messagingSenderId: "991414245044",
};
export const firebaseReady = () => !!(firebaseConfig.apiKey && firebaseConfig.appId);
