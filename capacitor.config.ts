import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.example.verumomnislegalassistant', // <-- make sure this matches your Firebase Android app
  appName: 'Verum Omnis',
  webDir: 'dist',
  bundledWebRuntime: false
};

export default config;
