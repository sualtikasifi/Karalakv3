// Firebase WEB config (public values, not secrets): Project settings -> Your apps -> Karalak Web.
export const firebaseConfig = {
  apiKey: "AIzaSyAvhmsNKlQ3G_tp80xv4ob5Z9Br4Y6BGVc",
  authDomain: "karalak-b6e11.firebaseapp.com",
  projectId: "karalak-b6e11",
  appId: "1:991414245044:web:667700203cd214db0569e1",
  storageBucket: "karalak-b6e11.firebasestorage.app",
  messagingSenderId: "991414245044",
};
// App Check for the web (reCAPTCHA Enterprise site key, also public): Firebase console -> App Check -> Karalak Web -> reCAPTCHA
// Enterprise, with karalak.com as the key's domain. Empty = App Check off on the web. Fill this in BEFORE enforcing App Check for
// Firestore, or the web game stops reaching the database.
export const appCheckSiteKey = "";
export const firebaseReady = () => !!(firebaseConfig.apiKey && firebaseConfig.appId);
