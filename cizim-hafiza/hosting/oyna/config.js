// Firebase WEB config (public values, not secrets). Fill in after creating the Web app
// in Firebase console -> Project settings -> Your apps -> Web. Until then the game runs offline.
export const firebaseConfig = {
  apiKey: "",
  authDomain: "karalak-b6e11.firebaseapp.com",
  projectId: "karalak-b6e11",
  appId: "",
};
export const firebaseReady = () => !!(firebaseConfig.apiKey && firebaseConfig.appId);
